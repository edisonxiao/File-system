package common;

public class Util {
	public static byte[] intToBytes(int x) {
		byte[] bytes = new byte[4];
		bytes[0] = (byte)(x & 0xFF);
		bytes[1] = (byte)((x >> 8) & 0xFF);
		bytes[2] = (byte)((x >> 16) & 0xFF);
		bytes[3] = (byte)((x >> 24) & 0xFF);		
		return bytes;
	}
 
	public static int bytesToInt(byte[] bytes) {
		return bytes[0] & 0xFF | (bytes[1] & 0xFF) << 8 | (bytes[2] & 0xFF) << 16 | (bytes[3] & 0xFF) << 24;
	}
}
