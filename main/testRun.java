package main;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import common.DFileID;
import dfs.MyDFS;

public class testRun implements Runnable {
	String input;
	String output;
	MyDFS dfs;
	
	testRun(String input, String output, MyDFS dfs){
		this.input=input;
		this.output=output;
		this.dfs=dfs;
	}

	
	public void run(){
		long start = System.currentTimeMillis();
		try {
			DFileID id = dfs.createDFile();
			byte[] buffer = getBuffer(new FileInputStream(input));
			dfs.write(id, buffer, 0, buffer.length);
			dfs.sync();
			byte[] readBuffer = new byte[buffer.length];
			dfs.read(id, readBuffer, 0, buffer.length);
			
			long end = System.currentTimeMillis();
			System.out.println("Time elapsed is " + (end - start) / 1000.0 + " seconds");
			printFile(readBuffer, output);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public static byte[] getBuffer(InputStream is) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		int nRead;
		byte[] data = new byte[1256165];
		while ((nRead = is.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, nRead);
		}
		buffer.flush();		
		return buffer.toByteArray();
	}
	
	public static void printFile(byte[] buffer, String fileName) throws IOException {
		if (buffer == null) {
			return;
		}		
		FileOutputStream out = new FileOutputStream(fileName);
		out.write(buffer);
		out.close();
	}
	
	

}
