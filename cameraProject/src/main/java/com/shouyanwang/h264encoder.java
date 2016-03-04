package com.shouyanwang;

import com.hcm.camera.data.ByteBufferManager;
import com.hcm.camera.data.net.ByteBuffer;

import android.util.Log;

public class h264encoder extends Thread {
	private int width, height;
	private long handle;

	private ByteBufferManager outbuffes = new ByteBufferManager(2, width * height * 3 / 2);

	private ByteBufferManager inbuffes = new ByteBufferManager(4, width * height * 4);

	public h264encoder(int width, int height) {
		this.width = width;
		this.height = height;
		handle = initEncoder(width, height);
	}

	public ByteBufferManager getOutBuffs() {
		return this.outbuffes;
	}

	public ByteBufferManager getInBuffs() {
		return this.inbuffes;
	}

	@Override
	public void run() {
		super.run();
		while (!Thread.interrupted()) {
			try {
				ByteBuffer inbuffs = inbuffes.getOutBuff();
				ByteBuffer outbuffs = outbuffes.getInBuff();
				if (inbuffs != null && outbuffs != null) {
					Log.e("Tag", "开始编码");
					int ret = encodeframe(handle, -1, inbuffs.getBuff(), inbuffs.getDataPostion(), outbuffs.getBuff());
					Log.e("编码", ret + "");
					inbuffes.reCallBuff();
					if (ret > 0) {
						outbuffs.datapostion = ret;
						outbuffes.commitBuff();
					}
				}
				Thread.sleep(2);
			} catch (Exception e) {
				Log.e("软编码", "编码信息错误_" + e.getMessage());
				e.printStackTrace();
			}
		}
		destory(handle);
	}

	enum intype {
		X264_TYPE_P, X264_TYPE_IDR, X264_TYPE_I, X264_TYPE_AUTO;
	}

	static {
		System.loadLibrary("X264Encoder");
	}

	public native long initEncoder(int encodeWidth, int encodeHeight);

	public native int destory(long handle);

	public native int encodeframe(long handle, int type, byte[] in, int inSize, byte[] out);
}
