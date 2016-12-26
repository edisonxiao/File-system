package virtualdisk;

import common.Constants.DiskOperationType;

import dblockcache.DBuffer;

public class Tuple {

	DBuffer buffer;
	DiskOperationType type;
	
	Tuple(DBuffer buf, DiskOperationType type){
		
		buffer=buf;
		this.type=type;
	}
	
	public DBuffer getBuffer(){
		return buffer;
	}
	
	public DiskOperationType getType(){
		return type;
	}
	
	
}
