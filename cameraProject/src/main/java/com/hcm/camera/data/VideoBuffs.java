package com.hcm.camera.data;

public class VideoBuffs {
	private byte[][][] _buff;
	private Object lock = new Object();

	private int min = 0;
	private int mout = 0;
	private int cat = 300;
	private int temp = 0;

	public VideoBuffs(int buffcount, int buffsize) {
		_buff = new byte[buffcount][2][buffsize];
	}

	public byte[][] getInBuff() {
		synchronized (lock) {
			if (cat == 0)
				return null;
			else {
				return _buff[min];
			}
		}
	}

	public void commitBuff() {
		synchronized (lock) {
			cat--;
			min++;
			if (min == 300) {
				min = 0;
			}
		}
	}
	/**
	 * ��ȡ���
	 * @return
	 */
	public byte[][] getOutBuff() {
		synchronized (lock) {
			if (cat == 300) {
				return null;
			} else {
				cat++;
				temp = mout;
				mout++;
				if (mout == 300)
					mout = 0;
				return _buff[temp];
			}
		}
	}

}
