package com.hcm.camera.video.decoder;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import com.hcm.camera.comm.CommTool;
import com.hcm.camera.data.RTPBuffModel;
import com.hcm.camera.data.VideoGciBuffs;
import com.hcm.camera.data.net.NetServer;
import com.hcm.camera.video.VideoQuality;
import com.hcm.camera.video.audio.AudioQuality;
import com.zhutieju.testservice.H264Android;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Bitmap.Config;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaCodec.BufferInfo;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.SurfaceHolder.Callback;

@SuppressLint("WrongCall")
public class AvcDecoder extends SurfaceView implements SurfaceHolder.Callback {
	private MediaCodec audiodecoder;
	private int videoWidth = VideoQuality.DEFAULT_VIDEO_QUALITY.resX;
	private int videoHeight = VideoQuality.DEFAULT_VIDEO_QUALITY.resY;
	private int framerate = VideoQuality.DEFAULT_VIDEO_QUALITY.framerate;
	private int biterate = VideoQuality.DEFAULT_VIDEO_QUALITY.bitrate;
	/** 0=>H264,1=>Jpeg */
	private int encodetype = 0;

	private int mtu = 33000;

	private DatagramSocket socket;
	private DatagramSocket audioSocket;

	private InetAddress connAddress;
	private int videoport = 4399;
	private int audioport = 4400;

	private SurfaceHolder m_surfaceHolder;

	private Canvas mCanvas;
	// byte[] mPixel = new byte[videoWidth * videoHeight * 3 / 2];

	private ByteBuffer bitmap_buffer; // = ByteBuffer.wrap(mPixel);

	Bitmap videoBit = Bitmap.createBitmap(videoWidth, videoHeight, Config.RGB_565);

	private boolean isDecAccEnCoder = false;

	private AudioTrack audioTrack;

	private H264Android cdec;

	private VideoGciBuffs buffs = new VideoGciBuffs(8, videoWidth * videoHeight * 8);

	private boolean isStart = false;

	private Matrix m = null;

	// private
	public AvcDecoder(Context context) {
		this(context, null);
	}

	public AvcDecoder(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public AvcDecoder(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		m_surfaceHolder = this.getHolder();
		m_surfaceHolder.addCallback((Callback) this);
		cdec = new H264Android();
	}

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {

	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		holder.setSizeFromLayout();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (m == null) {
			m = new Matrix();
			int width = 0;
			int heigth = 0;
			float radio = (float) canvas.getWidth() / (float) canvas.getHeight();
			heigth = canvas.getHeight();
			width = (int) ((float) heigth * radio);
			m.postScale(((float) width / (float) videoWidth), ((float) heigth / (float) videoHeight));
		}
		canvas.drawBitmap(videoBit, m, null);
		// canvas.drawText("2013年5月2号", 0, 10, paint);
	}

	public void start() {
		this.isStart = true;
		try {
			connAddress = InetAddress.getByAddress(CommTool.getAddressByName("192.168.168.38"));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		avcH264();
		audioThread.start();
	}

	public boolean isStart() {
		return this.isStart;
	}

	public synchronized void stop() {
		try {

			// m_surfaceHolder.removeCallback(this);
			isStart = false;
			Thread.sleep(300);

			if (!h264DecoderThread.isInterrupted()) {
				h264DecoderThread.interrupt();
				h264DecoderThread = null;
			}
			if (!drowThread.isInterrupted()) {
				drowThread.interrupt();
				drowThread = null;
			}
			if (!audioThread.isInterrupted()) {
				audioThread.interrupt();
				audioThread = null;
			}

			if (audioTrack != null) {
				audioTrack.stop();
				audioTrack.release();
				audioTrack = null;
			}

			if (cdec != null) {
				cdec.releaseDecoder();
				cdec = null;
			}
			mpi = null;

			NetServer.getIntance().getSocket().videoGciBuffs.init();
			NetServer.getIntance().getSocket().audioGciBuffs.init();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void avcH264() {
		try {
			// socket = new DatagramSocket(videoport);
			cdec = new H264Android();

			// for (int i = 0; i < mPixel.length; i++) {
			// mPixel[i] = (byte) 0x00;
			// }

			cdec.initDecoder(videoWidth, videoHeight);
			if (!h264DecoderThread.isAlive())
				h264DecoderThread.start();
			if (!drowThread.isAlive())
				drowThread.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private ByteBuffer[] inputBuffers;
	private ByteBuffer[] outputBuffers;
	private ByteBuffer inputBuffer;
	private ByteBuffer outputBuffer;
	private byte[] audioBuff;
	private byte[] taskAudioBuff;
	private long WAIT_TIME = 500000;
	private int samplingRate = 8000;

	private Thread audioThread = new Thread(new Runnable() {
		@Override
		public void run() {

			boolean isfrist = false;
			int pos = 0;

			int mAudioMinBufSize = AudioTrack.getMinBufferSize(samplingRate,// AudioQuality.DEFAULT_AUDIO_QUALITY.samplingRate,
					AudioFormat.CHANNEL_OUT_MONO,// AudioFormat.CHANNEL_CONFIGURATION_STEREO,
					AudioFormat.ENCODING_PCM_16BIT);

			audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, // 指定在流的类型
					// STREAM_ALARM：警告声
					// STREAM_MUSCI：音乐声，例如music等
					// STREAM_RING：铃声
					// STREAM_SYSTEM：系统声音
					// STREAM_VOCIE_CALL：电话声音
					samplingRate,
					// AudioQuality.DEFAULT_AUDIO_QUALITY.samplingRate,//
					// 设置音频数据的采样率
					AudioFormat.CHANNEL_OUT_MONO, // AudioFormat.CHANNEL_CONFIGURATION_STEREO,//
													// 设置输出声道为双声道立体声
					AudioFormat.ENCODING_PCM_16BIT,// 设置音频数据块是8位还是16位
					mAudioMinBufSize, AudioTrack.MODE_STREAM);// 设置模式类型，在这里设置为流类型
			audioTrack.play();

			// audioBuff = new byte[mAudioMinBufSize * 4];
			taskAudioBuff = new byte[2048];

			// DatagramPacket audio_rtp_packet = new DatagramPacket(audioBuff,
			// audioBuff.length);

			if (isDecAccEnCoder) {
				audiodecoder = MediaCodec.createDecoderByType("audio/mp4a-latm");
				MediaFormat format = new MediaFormat();
				format.setString(MediaFormat.KEY_MIME, "audio/mp4a-latm");
				format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
				format.setInteger(MediaFormat.KEY_SAMPLE_RATE, AudioQuality.DEFAULT_AUDIO_QUALITY.samplingRate);
				format.setInteger(MediaFormat.KEY_BIT_RATE, AudioQuality.DEFAULT_AUDIO_QUALITY.bitRate);
				format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
				audiodecoder.configure(format, null, null, 0);
				audiodecoder.start();
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			try {
				audioSocket = new DatagramSocket(audioport);
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			while (isStart) {
				try {
					// audioSocket.receive(audio_rtp_packet);
					RTPBuffModel audiobuff = NetServer.getIntance().getSocket().audioGciBuffs.getOutBuff();
					if (audiobuff != null) {
						Log.e("Tag", "收到音频包");
						if (isDecAccEnCoder) {
							inputBuffers = audiodecoder.getInputBuffers();
							int inputBufferIndex = audiodecoder.dequeueInputBuffer(WAIT_TIME);
							if (inputBufferIndex >= 0) {
								inputBuffer = inputBuffers[inputBufferIndex];
								inputBuffer.clear();
								if (!isfrist) {
									isfrist = true;
									byte[] fristdata = new byte[2];
									fristdata[0] = 0x15;
									fristdata[1] = -120;
									inputBuffer.put(fristdata);
								} else {
									inputBuffer.put(audioBuff);
								}
								audiodecoder.queueInputBuffer(inputBufferIndex, 0, audioBuff.length, System.nanoTime() / 1000, 0);

								BufferInfo buffinfo = new BufferInfo();
								int outputBufferIndex = audiodecoder.dequeueOutputBuffer(buffinfo, WAIT_TIME);
								while (outputBufferIndex >= 0) {
									outputBuffer = outputBuffers[outputBufferIndex];
									outputBuffer.position(0);
									outputBuffer.get(taskAudioBuff);
									pos = buffinfo.size;

									audiodecoder.releaseOutputBuffer(outputBufferIndex, false);
									outputBufferIndex = audiodecoder.dequeueOutputBuffer(buffinfo, WAIT_TIME);
								}
							}
						} else {
							// Log.e("Tag", audiobuff.getDataPostion());
							System.arraycopy(audiobuff.getBuff(), 0, taskAudioBuff, 0, audiobuff.getDataPostion());
							pos = audiobuff.getDataPostion();// audio_rtp_packet.getLength();
						}
						// && audioTrack.getState() ==
						// AudioTrack.PLAYSTATE_PLAYING
						if (audioTrack != null) {
							audioTrack.write(taskAudioBuff, 0, pos);
							Log.e("Tag", "播放" + pos);
						}
					}
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					Log.e("VV", e1.getMessage());
				}
			}
			Log.e("音频", "出来了");
		}
	});

	private byte[] cdecbuff = new byte[mtu];

	private RTPBuffModel mpi;

	private Thread h264DecoderThread = new Thread(new Runnable() {
		@Override
		public void run() {
			// byte[] buffer = new byte[mtu];
			// DatagramPacket rtp_packet = new DatagramPacket(buffer,
			// buffer.length);
			int totelbyteCount = 0;

			RTPBuffModel buff = null;
			while (isStart) {
				try {
					// socket.receive(rtp_packet);
					buff = NetServer.getIntance().getSocket().videoGciBuffs.getOutBuff();
					if (buff != null) {
						mpi = buffs.getInBuff();
						totelbyteCount = buff.getDataPostion();// rtp_packet.getLength();
						if (encodetype == 0)
							System.arraycopy(buff.getBuff(), 12, cdecbuff, 0, totelbyteCount - 12);
						else
							System.arraycopy(buff.getBuff(), 17, cdecbuff, 0, totelbyteCount - 17);

						long t1 = System.currentTimeMillis();

						int resout = -1;
						if (encodetype == 0) {
							if (mpi != null) {
								mpi.init();
								if (cdec != null) {
									synchronized (cdec) {
										resout = cdec.dalDecoder(cdecbuff, totelbyteCount - 12, mpi.getBuff());
									}
								} else {
									Log.e("有问题", "没有消亡");
								}
							} else {
								continue;
							}
							if (resout > 0) {
								buffs.commitBuff();
							}
						} else {
							if (mpi != null) {
								mpi.init();
								mpi.write(cdecbuff, 0, totelbyteCount - 17);
								buffs.commitBuff();
								// Bitmap m =
								// BitmapFactory.decodeByteArray(data, offset,
								// length, opts)
							}
						}
						long t2 = System.currentTimeMillis();

						// NetServer.getIntance().getSocket().videoGciBuffs.
					}
					Thread.sleep(1);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					// Log.e("Tag", e.getMessage());
				}
			}
			Log.e("Video", "出来了");
		}
	});

	private Thread drowThread = new Thread(new Runnable() {
		@Override
		public void run() {
			while (isStart) {
				try {
					long t1 = System.currentTimeMillis();
					RTPBuffModel m = buffs.getOutBuff();
					if (m != null) {
						if (encodetype == 0) {
							videoBit = Bitmap.createBitmap(videoWidth, videoHeight, Config.RGB_565);
							bitmap_buffer = ByteBuffer.wrap(m.getBuff());
							// videoBit = Bitmap.createBitmap(videoBit, x, y,
							// width,
							// height)
							videoBit.copyPixelsFromBuffer(bitmap_buffer);
							bitmap_buffer.position(0);
						} else {
							videoBit = BitmapFactory.decodeByteArray(m.getBuff(), 0, m.datapostion);
						}
						synchronized (m_surfaceHolder) {
							mCanvas = m_surfaceHolder.lockCanvas();
							if (mCanvas != null) {
								onDraw(mCanvas);
							}
							Thread.sleep(2);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					try {
						m_surfaceHolder.unlockCanvasAndPost(mCanvas);
					} catch (Exception e) {
					}
				}
			}
		}
	});

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
		// TODO Auto-generated method stub

	}

	private void surfaceDestroyed() {
		isStart = false;
	}

	public int getVideoWidth() {
		return videoWidth;
	}

	public void setVideoWidth(int videoWidth) {
		this.videoWidth = videoWidth;
	}

	public int getVideoHeight() {
		return videoHeight;
	}

	public void setVideoHeight(int videoHeight) {
		this.videoHeight = videoHeight;
	}

	public InetAddress getConnAddress() {
		return connAddress;
	}

	public void setConnAddress(InetAddress connAddress) {
		this.connAddress = connAddress;
	}

	public int getAudioport() {
		return audioport;
	}

	public void setAudioport(int audioport) {
		this.audioport = audioport;
	}

	public boolean isDecAccEnCoder() {
		return isDecAccEnCoder;
	}

	public void setDecAccEnCoder(boolean isDecAccEnCoder) {
		this.isDecAccEnCoder = isDecAccEnCoder;
	}

	public int getEncodetype() {
		return encodetype;
	}

	public void setEncodetype(int encodetype) {
		this.encodetype = encodetype;
	}

}
