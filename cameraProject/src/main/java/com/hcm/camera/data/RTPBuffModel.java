package com.hcm.camera.data;

import java.io.IOException;
import java.io.InputStream;

import android.util.Log;

public class RTPBuffModel extends InputStream {

	private byte[] buff;
	public int datapostion = 4;
	private int rep = 0;
	private int size = 0;
	private long timesamp = 0;
	private byte payload = 0;
	private long seq = 0;
	private byte[] SSI = new byte[4];

	public RTPBuffModel(int size) {
		 buff = new byte[size];
		// buff[0] = 0x00;
		// buff[1] = 0x00;
		// buff[2] = 0x00;
		// buff[3] = 0x01;
		this.size = size;
	}

	public void init() {
		datapostion = 0;
		rep = 0;
		timesamp = 0;
	}

	public void checkhead() {
		if (rep == 0 && buff[0] == 0x00 && buff[1] == 0x00 && buff[2] == 0x00
				&& buff[3] == 0x01) {
			rep = 4;
		}
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

	public long getTimesamp() {
		return timesamp;
	}

	public void setTimesamp(long timesamp) {
		this.timesamp = timesamp;
	}

	public byte getPayload() {
		return payload;
	}

	public void setPayload(byte payload) {
		this.payload = payload;
	}

	public long getSeq() {
		return seq;
	}

	public void setSeq(long seq) {
		this.seq = seq;
	}

	public byte[] getSSI() {
		return this.SSI;
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
