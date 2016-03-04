package com.hcm.camera.video.encoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.view.ViewDebug.HierarchyTraceType;

import com.gci.nutil.L;
import com.hcm.camera.data.ByteBufferManager;
import com.hcm.camera.data.net.ByteBuffer;
import com.hcm.camera.video.stream.rtp.RtpSocket;

public class JpegEncode {
	private int width, height;
	private int VideoFormatIndex;
	private int VideoQuality = 20;

	private ByteBufferManager buffes = new ByteBufferManager(10, RtpSocket.MTU);

	private List<byte[]> _lst = new ArrayList<byte[]>();
	// width * height * 4

	private boolean _isRun = true;

	public JpegEncode(int width, int height, int VideoFormatIndex) {
		this.width = width;
		this.height = height;
		this.VideoFormatIndex = VideoFormatIndex;
	}

	private Thread thr = new Thread(new Runnable() {
		@Override
		public void run() {
			while (_isRun) {
				try {
					if (_lst.size() > 0) {
						byte[] data = _lst.get(0);
						_lst.remove(0);
						ByteBuffer bf = buffes.getInBuff();
						if (bf != null) {
							outstream = new ByteArrayOutputStream();
							YuvImage image = new YuvImage(data, ImageFormat.NV21, width, height, null);
							data = null;
							if (image != null) {
								image.compressToJpeg(new Rect(0, 0, width, height), VideoQuality, outstream);
								bf.init();
								bf.buff = outstream.toByteArray();
								bf.datapostion = bf.buff.length;
								buffes.commitBuff();
								image = null;
								outstream = null;
								// System.gc();
							}
						}
					} else {
						Thread.sleep(20);
					}
				} catch (Exception e) {
					L.e("编码错误", e.getMessage());
				}
			}
		}
	});

	ByteArrayOutputStream outstream;

	public void encodeFrame(byte[] data) {
		synchronized (_lst) {
			if (_lst.size() <= 15) {
				_lst.add(data);
			} else {
				data = null;
			}
		}
	}

	public ByteBufferManager gettBuffs() {
		return this.buffes;
	}

	public void start() {
		_isRun = true;
		if (!thr.isAlive())
			thr.start();
	}

	public void stop() {
		_isRun = false;
		try {
			Thread.sleep(100);
		} catch (Exception e) {
		}
	}
}
