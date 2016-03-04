package com.hcm.camera.video;

import java.net.InetAddress;

import com.hcm.camera.comm.CommTool;
import com.hcm.camera.video.audio.AudioPcm;
import com.hcm.camera.video.gl.SurfaceView;

import android.util.Log;


public class VideoManager {
	public static VideoManager _m = null;
	private H264Stream stream;
	private AudioPcm pcm;
	private boolean isRun = false;

	public static VideoManager getIntance() {
		if (_m == null) {
			_m = new VideoManager();
		}
		return _m;
	}

	public synchronized void start(SurfaceView suiface) {
		if (!isRun) {
			InetAddress address = null;
			try {
				stream = new H264Stream();
				stream.setSurfaceView(suiface);
				stream.start();
				
				pcm = new AudioPcm();
				pcm.setAddress(address);
				pcm.setPort(4400);
				pcm.start();
				isRun = true;
			} catch (Exception e) {
			}
		} else {
			stream.zStart(suiface);
		}
	}

	public synchronized void stop() {
		try {
			if (stream != null) {
				stream.stop();
			}
			if (pcm != null)
				pcm.stopThread();
		} catch (Exception e) {
			Log.e("BBBBB", e.getMessage());
		}
		isRun = false;
		stream = null;
		pcm = null;
	}
}
