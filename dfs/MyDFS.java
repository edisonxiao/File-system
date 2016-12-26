package dfs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import virtualdisk.MyVirtualDisk;
import virtualdisk.VirtualDisk;
import common.Constants;
import common.DFileID;
import common.Util;
import dblockcache.DBuffer;
import dblockcache.DBufferCache;
import dblockcache.MyCache;

public class MyDFS extends DFS {
	private DBufferCache cache;
	private List<DFileID> dfiles = new ArrayList<DFileID>();
	public Map<Integer, List<Integer>> blockMap;
	private List<Integer> freeBlocks;

	public MyDFS(String volName, boolean format) {
		super(volName, format);
	}

	public MyDFS(boolean format) {
		super(format);
	}

	public MyDFS() {
		super();
	}

	@Override
	public void init() {
		// initialize virtual disk
		VirtualDisk disk = null;
		String volName = getVolName();
		boolean format = isFormatted();
		try {
			disk = new MyVirtualDisk(volName, format);
			new Thread( (MyVirtualDisk) disk).start();
		} catch (FileNotFoundException e) {
			System.err
					.println("Initialization Failure: Invalid file specified.");
			System.exit(1);
		} catch (IOException e) {
			System.err
					.println("Initialization Failure: I/O Exception occured when creating Virtual Disk");
			System.exit(1);
		}
		// initialize cache

		cache = new MyCache(Constants.NUM_OF_CACHE_BLOCKS, disk);
		initializeInodeRegion();
		initializeBlockMap();
		initializeFreeList();
	}

	@Override
	public synchronized DFileID createDFile() {
		DFileID id = null;
		int numFiles = Constants.MAX_DFILES;
		for (int i = 1; i <= numFiles; ++i) {
			DBuffer iNodeDBuffer = cache.getBlock(i);
			// find first available DFileID indicated by i
			
				if (iNodeAvailable(iNodeDBuffer)) {
					id = new DFileID(i);
					dfiles.add(id);

					byte[] buffer = iNodeDBuffer.getBuffer();
					byte[] ubuffer = new byte[buffer.length];

					// byte 0 set to 1, because i_node is now in use
					ubuffer[0] = 1;

					// bytes 1-4 of i_node contain the DFileID
					int dfileID = id.getDFileID();
					byte[] dfileIDBytes = Util.intToBytes(dfileID);
					System.arraycopy(dfileIDBytes, 0, ubuffer, 1, 4);

					// bytes 5-8 of i_node contain the file size, initially 0
					int fileSize = 0;
					byte[] fileSizeBytes = Util.intToBytes(fileSize);
					System.arraycopy(fileSizeBytes, 0, ubuffer, 5, 4);
					
					iNodeDBuffer.write(ubuffer, 0, ubuffer.length);
					blockMap.put(id.getDFileID(), new LinkedList<Integer>());
					cache.releaseBlock(iNodeDBuffer);
					// only create 1 file
					break;
				
			}
		}
		return id;
	}

	@Override
	public void destroyDFile(DFileID dFID) {
		synchronized (dFID) {
			int id = dFID.getDFileID();
			synchronized (this) {
				dfiles.remove(dFID);
				freeBlocks.addAll(blockMap.get(id));
				blockMap.remove(id);
			}
			DBuffer dbuf = cache.getBlock(id);
			byte[] ubuffer = new byte[dbuf.getBuffer().length];
			dbuf.read(ubuffer, 0, ubuffer.length);
			ubuffer[0] = 0; // make i_node not in use
			dbuf.write(ubuffer, 0, ubuffer.length);
		}
	}

	@Override
	public int read(DFileID dFID, byte[] buffer, int startOffset, int count) {
		synchronized (dFID) {
			DBuffer iNode = getINodeBlock(dFID);
			List<Integer> blockNumbers = null;
			synchronized (this) {
				blockNumbers = blockMap.get(dFID.getDFileID());
			}
			int numBlocksToRead = (int) Math
					.ceil(((double) count / Constants.BLOCK_SIZE));

			// read all blocks except last one
			int amountRead = 0;
			int bufferIndex = startOffset;
			int i;
			for (i = 0; i < blockNumbers.size() && i < numBlocksToRead - 1; ++i) {
				DBuffer dbuf = cache.getBlock(blockNumbers.get(i));
				dbuf.read(buffer, bufferIndex, Constants.BLOCK_SIZE);
				amountRead += Constants.BLOCK_SIZE;
				bufferIndex += Constants.BLOCK_SIZE;
				cache.releaseBlock(dbuf);
			}
			// read the last block
			if (i < blockNumbers.size()) {
				DBuffer dbuf = cache.getBlock(blockNumbers.get(i));
				int fileSize = getFileSize(iNode);
				dbuf.read(buffer, bufferIndex, fileSize % Constants.BLOCK_SIZE);
				amountRead += fileSize % Constants.BLOCK_SIZE;
				bufferIndex += fileSize % Constants.BLOCK_SIZE;
				cache.releaseBlock(dbuf);
			}
			return amountRead;
		}
	}

	@Override
	public int write(DFileID dFID, byte[] buffer, int startOffset, int count) {
		synchronized (dFID) {
			DBuffer iNode = getINodeBlock(dFID);
			int fileSize = 0;
			// if file exists
			if (iNode != null) {
				List<Integer> currentBlockNumbers = null;
				synchronized (this) {
					currentBlockNumbers = blockMap.get(dFID.getDFileID());
					freeBlocks.addAll(currentBlockNumbers);
				}
				currentBlockNumbers.clear();

				// get new set of block numbers
				getNewBlockNumbers(currentBlockNumbers, count);
				int blockIndex = 0;
				int bufferIndex = startOffset;
				int bytesLeft = count;
				while (bufferIndex < buffer.length && bytesLeft > 0
						&& blockIndex < currentBlockNumbers.size()) {
					DBuffer dbuf = cache.getBlock(currentBlockNumbers
							.get(blockIndex++));
					int bytesWritten = bytesLeft > Constants.BLOCK_SIZE ? Constants.BLOCK_SIZE
							: bytesLeft;
					dbuf.write(buffer, bufferIndex, bytesWritten);
					bytesLeft -= bytesWritten;
					fileSize += bytesWritten;
					bufferIndex += bytesWritten;
					cache.releaseBlock(dbuf);
				}
				updateFileSize(dFID, fileSize);
			}
			return fileSize;
		}
	}

	private void updateFileSize(DFileID dFID, int newFileSize) {
		DBuffer iNode = getINodeBlock(dFID);
		byte[] iNodeBuffer = iNode.getBuffer();
		byte[] ubuffer = new byte[iNodeBuffer.length];
		System.arraycopy(iNodeBuffer, 0, ubuffer, 0, iNodeBuffer.length);
		byte[] fileSizeBytes = Util.intToBytes(newFileSize);

		System.arraycopy(fileSizeBytes, 0, ubuffer, 5, 4);
		iNode.write(ubuffer, 0, ubuffer.length);
		cache.releaseBlock(iNode);
	}

	private void getNewBlockNumbers(List<Integer> currentBlockNumbers,
			int fileSize) {
		int numBlocks = (int) Math
				.ceil(((double) fileSize / Constants.BLOCK_SIZE));
		for (int i = 0; i < Constants.MAX_BLOCKS_PER_FILE && i < numBlocks; ++i) {
			synchronized (this) {
				currentBlockNumbers.add(freeBlocks.remove(0));
			}
		}
	}

	@Override
	public synchronized int sizeDFile(DFileID dFID) {
		synchronized (dFID) {
			if (dFID == null) {
				return -1;
			}
			DBuffer iNode = getINodeBlock(dFID);
			return getFileSize(iNode);
		}
	}

	@Override
	public synchronized List<DFileID> listAllDFiles() {
		return dfiles;
	}

	@Override
	public synchronized void sync() {
		cache.sync();
	}

	/*
	 * Extracts file size from an i_node's block buffer
	 */
	private int getFileSize(DBuffer iNode) {
		if (iNode == null) {
			return -1;
		}
		byte[] sizeBytes = new byte[4];
		System.arraycopy(iNode.getBuffer(), 5, sizeBytes, 0, 4);
		return Util.bytesToInt(sizeBytes);
	}

	/*
	 * Extracts fileID from an i_node's block buffer
	 */
	private int getDFileID(byte[] iNodeBuffer) {
		if (iNodeBuffer == null) {
			return -1;
		}
		byte[] dfileIDBytes = new byte[4];
		System.arraycopy(iNodeBuffer, 1, dfileIDBytes, 0, 4);
		return Util.bytesToInt(dfileIDBytes);
	}

	/*
	 * Gets block numbers for the file id with id == dfileID
	 */
	private List<Integer> getBlockNumbers(DFileID dfileID) {
		
		List<Integer> blockNumbers = new ArrayList<Integer>();
		if (dfileID != null) {
			DBuffer iNode = getINodeBlock(dfileID);
			if (iNode != null) {
				int fileSize = getFileSize(iNode);
				int numBlocks = (int) Math
						.ceil(((double) fileSize / Constants.BLOCK_SIZE));
				for (int i = 0; i < numBlocks; ++i) {
					int offset = 9; // first 9 bytes do not contain a block #
					byte[] blockNumberBytes = new byte[4];
					blockNumberBytes[0] = (byte) (offset + 4 * i);
					blockNumberBytes[1] = (byte) (offset + 4 * i + 1);
					blockNumberBytes[2] = (byte) (offset + 4 * i + 2);
					blockNumberBytes[3] = (byte) (offset + 4 * i + 3);
					int blockNumber = Util.bytesToInt(blockNumberBytes);
					blockNumbers.add(blockNumber);
				}
			}
		}


		return blockNumbers;
	}

	/*
	 * Finds i_node DBuffer that contains the block map of file with id ==
	 * dfileID. If it does not exist, return null.
	 */
	private DBuffer getINodeBlock(DFileID dfileID) {
		if (dfileID != null) {
			int numFiles = Constants.MAX_DFILES;
			// loop through i_node region (block_1 -> block_numFiles)
			for (int i = 1; i <= numFiles; ++i) {
				DBuffer dbuf = cache.getBlock(i);
				byte[] buffer = dbuf.getBuffer();
				// if i_node is in use
				if (!iNodeAvailable(dbuf)) {
					DFileID dbufFileID = new DFileID(getDFileID(buffer));
					// if this is the i_node with the given dfileID
					if (dbufFileID.equals(dfileID)) {
						
						return dbuf;
					}
				}
			}
		}
		return null;
	}

	/*
	 * Initialize all blocks in the i_node region
	 */
	private void initializeInodeRegion() {
		int numInodes = Constants.MAX_DFILES;
		// loop starting at block #1, and ending at block #numInodes
		// this is the range of the i_node region
		for (int i = 1; i <= numInodes; ++i) {
			DBuffer iNodeDBuffer = cache.getBlock(i);
			byte[] oneByte = new byte[1];
			oneByte[0] = 0;
			iNodeDBuffer.write(oneByte, 0, 1);
			// zero out first byte of i_node buffer. first byte indicates
			// that i_node is not currently in use
			cache.releaseBlock(iNodeDBuffer);
		}
	}

	/*
	 * Returns true if an i_node has not yet been associated with a file on
	 * disk.
	 */
	private boolean iNodeAvailable(DBuffer dbuf) {
		if (dbuf == null) {
			return false;
		}
		byte[] buffer = dbuf.getBuffer();
		if (buffer.length == 0) {
			return false;
		}
		return buffer[0] == 0;
	}

	private void initializeBlockMap() {
		blockMap = new HashMap<Integer, List<Integer>>();
		int numInodes = Constants.MAX_DFILES;
		for (int i = 1; i <= numInodes; ++i) {
			DBuffer dbuf = cache.getBlock(i);
			byte[] buffer = dbuf.getBuffer();
			if (iNodeAvailable(dbuf)) {
				int fileID = getDFileID(buffer);
				if (!blockMap.containsKey(fileID)) {
					blockMap.put(fileID, new LinkedList<Integer>());
				}
			}
		}
	}

	private void initializeFreeList() {
		freeBlocks = new LinkedList<Integer>();
		for (int i = Constants.MAX_DFILES + 1; i < Constants.NUM_OF_BLOCKS; ++i) {
			if (!blockExists(i)) {
				freeBlocks.add(i);
			}
		}
	}

	private synchronized boolean blockExists(int blockNumber) {
		for (List<Integer> blocks : blockMap.values()) {
			for (Integer block : blocks) {
				if (block == blockNumber) {
					return true;
				}
			}
		}
		return false;
	}

}
