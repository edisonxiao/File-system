package virtualdisk;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import common.Constants.DiskOperationType;
import dblockcache.DBuffer;

public class MyVirtualDisk extends VirtualDisk implements Runnable {

	private Queue<Tuple> taskQ = new LinkedList<Tuple>();
	private Object emptyQLock = new Object();

	public MyVirtualDisk(String volName, boolean format)
			throws FileNotFoundException, IOException {
		super(volName, format);
	}

	public MyVirtualDisk(boolean format) throws FileNotFoundException,
			IOException {
		super(format);
	}

	public MyVirtualDisk() throws FileNotFoundException, IOException {
		super();
	}

	public int requestNumber = 1;

	@Override
	public void startRequest(DBuffer buf, DiskOperationType operation) {
		if(buf.getBlockID()>10000){
			int s=1;
		}
		synchronized (taskQ) {
			taskQ.add(new Tuple(buf, operation));
			++requestNumber;
		}
		synchronized (emptyQLock) {
			emptyQLock.notifyAll();
		}

	}

	@Override
	public void run() {
		while (true) {
			synchronized (emptyQLock) {
				while (taskQ.isEmpty()) {
					try {
						emptyQLock.wait();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		
			try {
				processNextRequest();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();

			}
		}
	}

	private synchronized void processNextRequest() throws IOException {
		Tuple task;
		synchronized (taskQ){
		task = taskQ.remove();
		}
		DBuffer buf = task.getBuffer();
		DiskOperationType operation = task.getType();
		if (buf == null || operation == null) {
			return;
		}
		if (DiskOperationType.READ == operation) {
			synchronized (buf){
			readBlock(buf);
			}
		} else if (DiskOperationType.WRITE == operation) {
			synchronized (buf){
			writeBlock(buf);
			}
		}
		buf.ioComplete();

	}

}
