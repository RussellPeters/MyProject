package com.gci.libmad;

import java.util.ArrayList;
import java.util.List;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

/**
 * Jni调用类，连接 libmad.so 通过C代码实现对Mp3文件的解码，并播放控制类
 * 
 * @author Administrator
 * 
 */
public class NativeMP3Decoder {

	private String _path = null;
	private int _start = 0;
	private AudioTrack mAudioTrack;
	private int samplerate;
	private int mAudioMinBufSize;
	private boolean isCatch = false;
	private List<short[]> audioBuffer;
	private short[] buff;
	private int length = 1024 * 1024;

	public NativeMP3Decoder(String path, int start) {
		this._path = path;
		this._start = start;
		try {
			int ret = initAudioPlayer(path, start);
			initAutioPlayer();
			if (length < mAudioMinBufSize)
				length = mAudioMinBufSize;
		} catch (Exception e) {
			Log.e("音频", "初始化音频错误");
		}
	}

	/**
	 * 初始化因音频播放器
	 */
	private void initAutioPlayer() {
		samplerate = getAudioSamplerate();
		samplerate = samplerate / 2; // 单声道
		// 声音文件一秒钟buffer的大小
		mAudioMinBufSize = AudioTrack.getMinBufferSize(samplerate, AudioFormat.CHANNEL_CONFIGURATION_STEREO, AudioFormat.ENCODING_PCM_16BIT);

		mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, // 指定在流的类型
				// STREAM_ALARM：警告声
				// STREAM_MUSCI：音乐声，例如music等
				// STREAM_RING：铃声
				// STREAM_SYSTEM：系统声音
				// STREAM_VOCIE_CALL：电话声音

				samplerate,// 设置音频数据的采样率
				AudioFormat.CHANNEL_CONFIGURATION_STEREO,// 设置输出声道为双声道立体声
				AudioFormat.ENCODING_PCM_16BIT,// 设置音频数据块是8位还是16位
				mAudioMinBufSize, AudioTrack.MODE_STREAM);// 设置模式类型，在这里设置为流类型
		// AudioTrack中有MODE_STATIC和MODE_STREAM两种分类。
		// STREAM方式表示由用户通过write方式把数据一次一次得写到audiotrack中。
		// 这种方式的缺点就是JAVA层和Native层不断地交换数据，效率损失较大。
		// 而STATIC方式表示是一开始创建的时候，就把音频数据放到一个固定的buffer，然后直接传给audiotrack，
		// 后续就不用一次次得write了。AudioTrack会自己播放这个buffer中的数据。
		// 这种方法对于铃声等体积较小的文件比较合适。
	}

	/** 设置是否可以Mp3文件是否予许被缓存 */
	public void setCatch(boolean blg) {
		this.isCatch = blg;
	}

	/** 播放 */
	public void play() {
		try {
			new Thread(new Runnable() {
				@Override
				public void run() {
					playPCM();
				}
			}).start();
			if (mAudioTrack.getPlayState() != AudioTrack.PLAYSTATE_PLAYING)
				mAudioTrack.play();
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	/** 播放PCM音频流*/
	private synchronized void playPCM() {
		try {
			if (audioBuffer == null) {
				if (isCatch) {
					audioBuffer = new ArrayList<short[]>();
				}
				while (true) {
					if (mAudioTrack.getPlayState() != AudioTrack.PLAYSTATE_PAUSED) {
						if (isCatch) {
							buff = new short[length];
						} else if (buff == null) {
							buff = new short[length];
						}
						int ret = getAudioBuf(buff, mAudioMinBufSize);
						mAudioTrack.write(buff, 0, mAudioMinBufSize);
						if (isCatch) {
							audioBuffer.add(buff);
						}
						if (ret == 0) {
							break;
						}
					}
				}
			} else {
				for (int i = 0; i < audioBuffer.size(); i++) {
					if (mAudioTrack.getPlayState() != AudioTrack.PLAYSTATE_PAUSED) {
						mAudioTrack.write(audioBuffer.get(i), 0, mAudioMinBufSize);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public native int initAudioPlayer(String path, int start);

	public native int getAudioBuf(short[] buff, int len);

	public native int getAudioSamplerate();

	public native void closeAduioFile();

	static {
		System.loadLibrary("mad");
	}

	@Override
	protected void finalize() throws Throwable {
		mAudioTrack.stop();
		mAudioTrack.release();// 关闭并释放资源
		closeAduioFile();
		super.finalize();
	}
}
