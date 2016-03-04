package com.hcm.camera.data.net;

import java.io.IOException;
import java.io.InputStream;

public class ByteBuffer extends InputStream{
	public byte[] buff;
	public int datapostion = 4;
	private int rep = 0;
	private int size = 0;
	
	/** 已读取长度*/
	private long seq = 0;
	
	public ByteBuffer(int size) {
		 buff = new byte[size];
		this.size = size;
	}

	public void init() {
		datapostion = 0;
		rep = 0;
	}

	public int getDataPostion() {
		return (this.datapostion - rep);
	}

	public synchronized void write(byte[] bytes, int start, int lenght) {
		if ((datapostion + lenght) > size) {
			byte[] newBytes = new byte[datapostion + lenght + 1024];
			System.arraycopy(buff, 0, newBytes, 0, size);
			buff = newBytes;
		}
		System.arraycopy(bytes, start, buff, datapostion, lenght);
		datapostion += lenght;
	}

	public byte[] getBuff() {
		return this.buff;
	}

	public int getSize() {
		return this.size;
	}

	public long getSeq() {
		return seq;
	}

	public void setSeq(long seq) {
		this.seq = seq;
	}

	@Override
	public synchronized int read() throws IOException {
		if (this.size > this.datapostion) {
			datapostion++;
			return buff[datapostion];
		} else {
			return -1;
		}
	}
	
	@Override
	public int read(byte[] buffer, int offset, int length) throws IOException {
		int result = 0;
		if ((datapostion - rep) < length) {
			result = (datapostion - rep);
		} else {
			result = length;
		}
		System.arraycopy(buff, rep, buffer, offset, length);
		rep += length;
		return result;
	}
}
