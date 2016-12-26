package virtualdisk;

import java.io.IOException;

import common.Constants.DiskOperationType;
import dblockcache.*;

public class IOHandler implements Runnable {

	VirtualDisk disk;
	DBuffer buffer;
	DiskOperationType type;

	public IOHandler(VirtualDisk disk, DBuffer buffer, DiskOperationType type) {
		this.disk = disk;
		this.buffer = buffer;
		this.type = type;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub

		try {
			disk.startRequest(buffer, type);
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}

	}

}
