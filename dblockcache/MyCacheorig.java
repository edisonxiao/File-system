package dblockcache;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import common.Constants;
import common.Constants.DiskOperationType;
import virtualdisk.VirtualDisk;

public class MyCache extends DBufferCache{
	private VirtualDisk myDisk;
	private List<Integer> bufferList;
	private HashMap<Integer, DBuffer> bufferMap;
	

	public MyCache(int cacheSize, VirtualDisk disk) {
		super(cacheSize);
		myDisk = disk;
		this.bufferList = new LinkedList<Integer>();
		this.bufferMap = new HashMap<Integer, DBuffer>(cacheSize);
		
	}

	@Override
	public DBuffer getBlock(int blockID) {

		if(bufferList.contains(blockID)){
			refresh(blockID);
			return bufferMap.get(blockID);
		}
		
		DBuffer dbuf = new MyDBuffer(blockID, myDisk);
		dbuf.startFetch();
		dbuf.waitValid();
		putBlock(dbuf);
		return dbuf;
	}
	
	public synchronized void refresh(int blockID){
		for(int i = 0; i < bufferList.size(); i++){
			if(bufferList.get(i) == blockID){
				bufferList.remove(i);
				break;
			}
		}
		bufferList.add(blockID);
	}
	
	public synchronized void putBlock(DBuffer dbuf){
		
		bufferMap.put(dbuf.getBlockID(), dbuf);
		
		if(!isFull()){
			bufferList.add(dbuf.getBlockID());
			return;
		}
		
		removeLRUBlock(0);
		bufferList.add(dbuf.getBlockID());
		
	}
	@Override
	public void releaseBlock(DBuffer dbuf) {
		((MyDBuffer)dbuf).setBusy(false);
	}
	
	public synchronized void removeLRUBlock(int index){
		if(index == 0){
			bufferList.remove(0);
			return;
		}
		DBuffer temp = bufferMap.get(bufferList.get(index));
		if(temp.isBusy()){
			removeLRUBlock(index - 1);
			return;
		}
		
		if(!temp.checkClean()){
			temp.startPush();
			temp.waitClean();
		}
		
		
		bufferList.remove(index);
		bufferMap.remove(temp.getBlockID());
		return;
		
	}
	
	public boolean isFull(){
		return bufferList.size() >= Constants.NUM_OF_CACHE_BLOCKS;
	}

	@Override
	public void sync() {
		for(int i = 0; i < bufferList.size(); i++){
			DBuffer temp = bufferMap.get(bufferList.get(i));
			if(!temp.checkClean()){
				temp.startPush();
				temp.waitClean();
			}
		}
		
	}
	

}
