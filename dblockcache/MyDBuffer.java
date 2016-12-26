package dblockcache;

import java.io.IOException;

import main.Main;
import common.Constants;
import common.Constants.DiskOperationType;
import virtualdisk.IOHandler;
import virtualdisk.VirtualDisk;

public class MyDBuffer extends DBuffer {
	private byte[] myBuffer;
	private int blockID;
	private boolean dirty;
	private boolean Held;
	private boolean IObusy;
	private VirtualDisk disk;
	private Object IOLock = new Object();
	private Object dirtyLock = new Object();
	DiskOperationType operationType;

	public MyDBuffer(int blockID, VirtualDisk disk) {
		myBuffer = new byte[Constants.BLOCK_SIZE];
		this.blockID = blockID;
		this.disk = disk;
		this.Held = false;
		this.dirty = false;
		this.IObusy = false;
		operationType = null;

	}

	@Override
	public void startFetch() {
		synchronized (this) {
			operationType = DiskOperationType.READ;
			IObusy = true;
		}
		try {
			disk.startRequest(this, operationType);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void startPush() {
		synchronized (this) {
			operationType = DiskOperationType.WRITE;
			IObusy = true;
		}
		try {
			disk.startRequest(this, operationType);
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public synchronized boolean checkValid() {
		return !IObusy;
	}

	@Override
	public boolean waitValid() {
		synchronized (IOLock) {
			while (IObusy) {
				try {
					IOLock.wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
				}
			}
		}
		if (!IObusy) {
			return true;
		}
		return false;

	}

	@Override
	public synchronized boolean checkClean() {
		return !dirty;
	}

	@Override
	public boolean waitClean() {
		synchronized (dirtyLock){
		while (dirty) {
			try {
				dirtyLock.wait();
			} catch (InterruptedException e) {
			}
		}
		}
		if (!dirty) {
			return true;
		}
		return false;
	}

	@Override
	public synchronized boolean isBusy() {
		return Held || IObusy;
	}

	@Override
	public int read(byte[] buffer, int startOffset, int count) {
		if (waitValid()) {
			synchronized (this){
				try{
					if(buffer.length<startOffset+count){
						count=buffer.length-startOffset;
					}
			System.arraycopy(myBuffer, 0, buffer, startOffset, count);
				}
				catch(ArrayIndexOutOfBoundsException e){
					System.out.println("start offset is "+startOffset+ " count is "+count+" buffer size is "+ buffer.length);
					e.printStackTrace();
				}
			}
			return count;
		}
		return -1;
	}

	@Override
	public int write(byte[] buffer, int startOffset, int count) {
		if (waitValid()) {
			synchronized (this){
			System.arraycopy(buffer, startOffset, myBuffer, 0, count);
			dirty = true;
			}
			return count;
		}
		return -1;
	}

	@Override
	public void ioComplete() {
		synchronized (this){
		IObusy = false;
		}
		synchronized (IOLock) {
			IOLock.notifyAll();
		}
		if (operationType == DiskOperationType.WRITE) {
			synchronized (dirtyLock){
				dirty = false;
				dirtyLock.notifyAll();
			}
		}

	}

	@Override
	public synchronized int getBlockID() {
		return blockID;
	}

	@Override
	public synchronized byte[] getBuffer() {
		return myBuffer;
	}

	public synchronized void setBusy(boolean wrbusy) {
		Held = wrbusy;
	}

	/*
	 * public void run(){ try { disk.startRequest(this, operationType); } catch
	 * (IllegalArgumentException e) { // TODO Auto-generated catch block
	 * e.printStackTrace(); System.exit(1); } catch (IOException e) { // TODO
	 * Auto-generated catch block e.printStackTrace(); System.exit(1); } }
	 */

	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < myBuffer.length; ++i) {
			sb.append((char) myBuffer[i]);
		}
		sb.append("\n");
		return sb.toString();
	}

}
