package com.hcm.camera.data.net;

public class BufferTool {
	public static long bytes2Long(byte[] byteNum, int begin, int end) {
		long num = 0;
		for (int ix = begin; ix < end; ++ix) {
			num <<= 8;
			num |= (byteNum[ix] & 0xff);
		}
		return num;
	}

	@SuppressWarnings("unused")
	public static void setLong(byte[] buff, long n, int begin, int end) {
		for (end--; end >= begin; end--) {
			buff[end] = (byte) (n % 256);
			n >>= 8;
		}
	}
}
