//

package main;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Scanner;

import common.DFileID;
import dfs.DFS;
import dfs.MyDFS;

public class Main {
	public static void main(String[] args) throws IOException {
		MyDFS dfs = new MyDFS(false);		
		
		new Thread (new testRun("abbey.txt","test.txt",dfs)).start();
		new Thread (new testRun("mobydick.txt","test.txt",dfs)).start();
		new Thread (new testRun("eco.txt","ec.txt",dfs)).start();
		new Thread (new testRun("blexp10.txt","b10.txt",dfs)).start();
		new Thread (new testRun("circedge.txt","ccd.txt",dfs)).start();
		new Thread (new testRun("hack_ths.txt","ht.txt",dfs)).start();
	}
	

}
