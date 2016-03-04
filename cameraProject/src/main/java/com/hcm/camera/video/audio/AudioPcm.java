package com.hcm.camera.video.audio;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import com.gci.nutil.L;
import com.hcm.camera.data.net.NetServer;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class AudioPcm extends Thread {
	private InetAddress ip;
	private int port = 0;
	private AudioRecord mAudioRecord;
	private int samplingRate = 8000;
	private byte[] buff;
	private boolean isStart = false;

	@Override
	public void start() {
		try {
			isStart = true;
			init();
		} catch (Exception e) {
			e.printStackTrace();
		}
		super.start();
	}

	private void init() {
		final int bufferSize = AudioRecord.getMinBufferSize(samplingRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
		buff = new byte[bufferSize];
		mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, samplingRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
		mAudioRecord.startRecording();
	}

	@Override
	public void run() {
		int len = 0;
		while (isStart) {
			try {
				len = mAudioRecord.read(buff, 0, buff.length);
				if (len == AudioRecord.ERROR_INVALID_OPERATION || len == AudioRecord.ERROR_BAD_VALUE) {
					Log.e("TAG", "An error occured with the AudioRecord API !");
				} else {
					NetServer.getIntance().getSocket().sendAudioMideaPack(buff, len);
				}
			} catch (Exception e) {
				isStart = false;
				e.printStackTrace();
			}
		}
	}

	public void setAddress(InetAddress ip) {
		this.ip = ip;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void stopThread() {
		isStart = false;
		mAudioRecord.startRecording();
		mAudioRecord.stop();
		mAudioRecord.release();
	}
}
