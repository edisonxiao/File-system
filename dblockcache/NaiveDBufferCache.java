package dblockcache;

import java.io.IOException;

import common.Constants.DiskOperationType;

import virtualdisk.VirtualDisk;

/**
 * @author Erick Gonzalez
 * NaiveDBufferCache is a naive implementation that does not really cache anything. It
 * just makes an I/O every time that a block is requested.
 * 
 * This class is useful for testing purposes, in order to avoid the complexity
 * of a real cache
 *
 */
public class NaiveDBufferCache extends DBufferCache {
	private VirtualDisk disk;

	public NaiveDBufferCache(int cacheSize, VirtualDisk disk) {
		super(cacheSize);
		this.disk = disk;
	}

	@Override
	public DBuffer getBlock(int blockID) {
		DBuffer dbuf = new MyDBuffer(blockID, disk);
		try {
			disk.startRequest(dbuf, DiskOperationType.READ);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return dbuf;
	}

	@Override
	public void releaseBlock(DBuffer buf) {
		try {			
			disk.startRequest(buf, DiskOperationType.WRITE);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	@Override
	public void sync() {
		// no need to sync, because nothing is cached
	}

}
