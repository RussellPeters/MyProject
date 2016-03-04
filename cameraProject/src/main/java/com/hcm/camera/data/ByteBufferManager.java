package com.hcm.camera.data;

import com.hcm.camera.data.net.ByteBuffer;


public class ByteBufferManager {
	ByteBuffer[] _buffers;

	private Object lock = new Object();

	private int min = 0;
	private int mout = 0;
	private int shengyu = 0;
	private int total = 0;

	public ByteBufferManager(int count, int bytesize) {
		_buffers = new ByteBuffer[count];
		shengyu = count;
		total = count;
		for (int i = 0; i < count; i++) {
			_buffers[i] = new ByteBuffer(bytesize);
		}

	}

	public ByteBuffer getInBuff() {
		synchronized (lock) {
			if (shengyu == 0)
				return null;
			else {
				return _buffers[min];
			}
		}
	}

	public void commitBuff() {
		synchronized (lock) {
			shengyu--;
			min++;
			if (min == total) {
				min = 0;
			}
		}
	}

	public ByteBuffer getOutBuff() {
		synchronized (lock) {
			if (shengyu == total) {
				return null;
			} else {
				return _buffers[mout];
			}
		}
	}

	public void reCallBuff() {
		synchronized (lock) {
			shengyu++;
			mout++;
			if (mout == total)
				mout = 0;
		}
	}

	public int getPosion() {
		return (total - shengyu);
	}
	
	public int getShengYu(){
		return shengyu;
	}
}
