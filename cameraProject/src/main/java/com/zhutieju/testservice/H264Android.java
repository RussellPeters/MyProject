package com.zhutieju.testservice;

public class H264Android {
	static {
		System.loadLibrary("ffmpeg");
		System.loadLibrary("H264Android");
	}
	
	public native int initDecoder(int width,int heigth);
	public native int dalDecoder(byte[] in,int size,byte[] out);
	public native int releaseDecoder();
}
