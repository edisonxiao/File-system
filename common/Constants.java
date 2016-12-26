package common;

/*
 * This class contains the global constants used in DFS
 */

public class Constants {

	/* The below constants indicate that we have approximately 268 MB of
	 * disk space with 67 MB of memory cache; a block can hold upto 32 inodes and
	 * the maximum file size is constrained to be 500 blocks. These are compile
	 * time constants and can be changed during evaluation.  Your implementation
	 * should be free of any hard-coded constants.  
	 */

	public static final int NUM_OF_BLOCKS = 2048; // 2^18
	//public static final int NUM_OF_BLOCKS = 32;
	public static final int BLOCK_SIZE = 4096; // 2kB
	//public static final int INODE_SIZE = 32; //32 Bytes
	//public static final int NUM_OF_CACHE_BLOCKS = 16; 
	public static final int NUM_OF_CACHE_BLOCKS = 2048; // 2^16
	public static final int MAX_BLOCKS_PER_FILE = 500;
	public static final int MAX_FILE_SIZE = BLOCK_SIZE * MAX_BLOCKS_PER_FILE; // Constraint on the max file size
	// 4 bytes for the file size, and an additional 4 bytes for every possible block number per file. 
	// also add padding number of bytes to make one I_NODE fit in a block
	public static final int INODE_SIZE = 12 + 4 * MAX_BLOCKS_PER_FILE + BLOCK_SIZE % (12 + 4 * MAX_BLOCKS_PER_FILE);  

	public static final int MAX_DFILES = 512; // For recylcing DFileIDs

	/* DStore Operation types */
	public enum DiskOperationType {
		READ, WRITE
	};

	/* Virtual disk file/store name */
	public static final String vdiskName = "DSTORE.dat";
}
