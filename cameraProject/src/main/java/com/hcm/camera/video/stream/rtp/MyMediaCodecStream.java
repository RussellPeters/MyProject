package com.hcm.camera.video.stream.rtp;

import java.io.IOException;
import java.io.InputStream;

public class MyMediaCodecStream extends InputStream {

	private byte[] buffs;
	private int postion = 0;
	private int readp = 0;

	public MyMediaCodecStream(byte[] width, int height) {
	}

	@Override
	public int read() throws IOException {

		return 0;
	}

	public int read(byte[] buffer, int offset, int length) throws IOException {
		int result = 0;
		if ((postion - readp) < length) {
			result = (postion - readp);
		} else {
			result = length;
		}
		System.arraycopy(buffs, readp, buffer, offset, length);
		readp += length;
		return result;
	}

	public void writ(byte[] buff, int start, int len) {
		if (buffs == null) {
			buffs = new byte[len];
		}
		System.arraycopy(buff, start, buffs, postion, len);
		postion += len;
	}

	public int getSize() {
		return postion - readp;
	}
}
