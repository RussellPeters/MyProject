/*
 * Copyright (C) 2011-2014 GUIGUI Simon, fyhertz@gmail.com
 * 
 * This file is part of libstreaming (https://github.com/fyhertz/libstreaming)
 * 
 * Spydroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.hcm.camera.video;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.gci.nutil.thread.WorkThreadFatory;
import com.hcm.camera.data.RTPBuffModel;
import com.hcm.camera.data.VideoGciBuffs;
import com.hcm.camera.video.encoder.AvcEncoder;
import com.hcm.camera.video.encoder.JpegEncode;
import com.hcm.camera.video.gl.SurfaceView;
import com.hcm.camera.video.stream.exception.CameraInUseException;
import com.hcm.camera.video.stream.exception.ConfNotSupportedException;
import com.hcm.camera.video.stream.exception.InvalidSurfaceException;
import com.shouyanwang.h264encoder;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.media.CamcorderProfile;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;

/**
 * Don't use this class directly.
 */
public abstract class VideoStream extends MediaStream {

	protected final static String TAG = "VideoStream";

	protected VideoQuality mRequestedQuality = VideoQuality.DEFAULT_VIDEO_QUALITY.clone();
	protected VideoQuality mQuality = mRequestedQuality.clone();
	protected SurfaceHolder.Callback mSurfaceHolderCallback = null;
	protected SurfaceView mSurfaceView = null;
	protected SharedPreferences mSettings = null;
	protected int mVideoEncoder, mCameraId = 0;
	protected int mRequestedOrientation = 0, mOrientation = 0; // mOrientation
																// = 0
	protected Camera mCamera;
	protected Thread mCameraThread;
	protected Looper mCameraLooper;

	protected boolean mCameraOpenedManually = true;
	protected boolean mFlashEnabled = false;
	protected boolean mSurfaceReady = false;
	protected boolean mUnlocked = false;
	protected boolean mPreviewStarted = false;

	protected String mMimeType;
	protected String mEncoderName;
	protected int mEncoderColorFormat;
	protected int mCameraImageFormat;
	protected int mMaxFps = 0;

	private AvcEncoder avcCodec;
	private byte[] yuv420;
	private VideoGciBuffs myMMbuff;
	private JpegEncode jpegencode;
	private h264encoder encoder;

	/**
	 * Don't use this class directly. Uses CAMERA_FACING_BACK by default.
	 */
	public VideoStream() {
		this(CameraInfo.CAMERA_FACING_BACK);
	}

	/**
	 * Don't use this class directly
	 * 
	 * @param camera
	 *            Can be either CameraInfo.CAMERA_FACING_BACK or
	 *            CameraInfo.CAMERA_FACING_FRONT
	 */
	@SuppressLint("InlinedApi")
	public VideoStream(int camera) {
		super();
		setCamera(camera);
	}

	/**
	 * Sets the camera that will be used to capture video. You can call this
	 * method at any time and changes will take effect next time you start the
	 * stream.
	 * 
	 * @param camera
	 *            Can be either CameraInfo.CAMERA_FACING_BACK or
	 *            CameraInfo.CAMERA_FACING_FRONT
	 */
	public void setCamera(int camera) {
		CameraInfo cameraInfo = new CameraInfo();
		int numberOfCameras = Camera.getNumberOfCameras();
		for (int i = 0; i < numberOfCameras; i++) {
			Camera.getCameraInfo(i, cameraInfo);
			if (cameraInfo.facing == camera) {
				mCameraId = i;
				break;
			}
		}
	}

	/**
	 * Switch between the front facing and the back facing camera of the phone.
	 * If {@link #startPreview()} has been called, the preview will be briefly
	 * interrupted. If {@link #start()} has been called, the stream will be
	 * briefly interrupted. You should not call this method from the main thread
	 * if you are already streaming.
	 * 
	 * @throws IOException
	 * @throws RuntimeException
	 **/
	public void switchCamera() throws RuntimeException, IOException {
		if (Camera.getNumberOfCameras() == 1)
			throw new IllegalStateException("Phone only has one camera !");
		boolean streaming = mStreaming;
		boolean previewing = mCamera != null && mCameraOpenedManually;
		mCameraId = (mCameraId == CameraInfo.CAMERA_FACING_BACK) ? CameraInfo.CAMERA_FACING_FRONT : CameraInfo.CAMERA_FACING_BACK;
		setCamera(mCameraId);
		stopPreview();
		mFlashEnabled = false;
		if (previewing)
			startPreview();
		if (streaming)
			start();
	}

	public int getCamera() {
		return mCameraId;
	}

	/**
	 * Sets a Surface to show a preview of recorded media (video). You can call
	 * this method at any time and changes will take effect next time you call
	 * {@link #start()}.
	 */
	public synchronized void setSurfaceView(SurfaceView view) {
		mSurfaceView = view;
		if (mSurfaceHolderCallback != null && mSurfaceView != null && mSurfaceView.getHolder() != null) {
			mSurfaceView.getHolder().removeCallback(mSurfaceHolderCallback);
		}
		if (mSurfaceView.getHolder() != null) {
			mSurfaceHolderCallback = new Callback() {
				@Override
				public void surfaceDestroyed(SurfaceHolder holder) {
					mSurfaceReady = false;
					stopPreview();
					Log.d(TAG, "Surface destroyed !");
				}

				@Override
				public void surfaceCreated(SurfaceHolder holder) {
					mSurfaceReady = true;
				}

				@Override
				public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
					Log.d(TAG, "Surface Changed !");
				}
			};
			mSurfaceView.getHolder().addCallback(mSurfaceHolderCallback);
			mSurfaceReady = true;
		}
	}

//	/** Turns the LED on or off if phone has one. */
//	public synchronized void setFlashState(boolean state) {
//		// If the camera has already been opened, we apply the change
//		// immediately
//		if (mCamera != null) {
//
//			if (mStreaming && mMode == MODE_MEDIARECORDER_API) {
//				lockCamera();
//			}
//
//			Parameters parameters = mCamera.getParameters();
//
//			// We test if the phone has a flash
//			if (parameters.getFlashMode() == null) {
//				// The phone has no flash or the choosen camera can not toggle
//				// the flash
//				throw new RuntimeException("Can't turn the flash on !");
//			} else {
//				parameters.setFlashMode(state ? Parameters.FLASH_MODE_TORCH : Parameters.FLASH_MODE_OFF);
//				try {
//					mCamera.setParameters(parameters);
//					mFlashEnabled = state;
//				} catch (RuntimeException e) {
//					mFlashEnabled = false;
//					throw new RuntimeException("Can't turn the flash on !");
//				} finally {
//					if (mStreaming && mMode == MODE_MEDIARECORDER_API) {
//						unlockCamera();
//					}
//				}
//			}
//		} else {
//			mFlashEnabled = state;
//		}
//	}

	/** Toggle the LED of the phone if it has one. */
//	public synchronized void toggleFlash() {
//		setFlashState(!mFlashEnabled);
//	}

	/** Indicates whether or not the flash of the phone is on. */
	public boolean getFlashState() {
		return mFlashEnabled;
	}

	/**
	 * Sets the orientation of the preview.
	 * 
	 * @param orientation
	 *            The orientation of the preview
	 */
	public void setPreviewOrientation(int orientation) {
		mRequestedOrientation = orientation;
	}

	/**
	 * Sets the configuration of the stream. You can call this method at any
	 * time and changes will take effect next time you call {@link #configure()}
	 * .
	 * 
	 * @param videoQuality
	 *            Quality of the stream
	 */
	public void setVideoQuality(VideoQuality videoQuality) {
		mRequestedQuality = videoQuality.clone();
	}

	/**
	 * Returns the quality of the stream.
	 */
	public VideoQuality getVideoQuality() {
		return mRequestedQuality;
	}

	/**
	 * Some data (SPS and PPS params) needs to be stored when
	 * {@link #getSessionDescription()} is called
	 * 
	 * @param prefs
	 *            The SharedPreferences that will be used to save SPS and PPS
	 *            parameters
	 */
	public void setPreferences(SharedPreferences prefs) {
		mSettings = prefs;
	}

	/**
	 * Configures the stream. You need to call this before calling
	 * {@link #getSessionDescription()} to apply your configuration of the
	 * stream.
	 */
	public synchronized void configure() throws IllegalStateException, IOException {
		super.configure();
		mOrientation = mRequestedOrientation;
	}

	/**
	 * Starts the stream. This will also open the camera and dispay the preview
	 * if {@link #startPreview()} has not aready been called.
	 */
	public synchronized void start() throws IllegalStateException, IOException {
		if (!mPreviewStarted)
			mCameraOpenedManually = false;
		super.start();
		Log.d(TAG, "Stream configuration: FPS: " + mQuality.framerate + " Width: " + mQuality.resX + " Height: " + mQuality.resY);
	}

	/** Stops the stream. */
	public synchronized void stop() {
		if (mCamera != null) {
			if (mMode == MODE_MY_MEDACODE_API || mMode == MODE_MY_JPEG) {
				mCamera.setPreviewCallbackWithBuffer(null);
			}
			jpegencode.stop();
			super.stop();
			// We need to restart the preview
			if (!mCameraOpenedManually) {
				destroyCamera();
			}
			jpegencode = null;
		}
	}

	public synchronized void startPreview() throws CameraInUseException, InvalidSurfaceException, ConfNotSupportedException, RuntimeException {

		mCameraOpenedManually = true;
		if (!mPreviewStarted) {
			createCamera();
			updateCamera();
			try {
				mCamera.startPreview();
				mPreviewStarted = true;
			} catch (RuntimeException e) {
				destroyCamera();
				throw e;
			}
		}
	}

	/**
	 * Stops the preview.
	 */
	public synchronized void stopPreview() {
		mCameraOpenedManually = false;
		zStop();
		// stop();
	}

	/**
	 * Video encoding is done by a MediaCodec.
	 */
	protected void encodeWithMediaCodec() throws RuntimeException, IOException {
		if (mMode == MODE_MY_MEDACODE_API) {
			myMidaCodecMethod();
		}
	}

	public Camera.Parameters initJpegCamera() {
		createCamera();
		SurfaceHolder m_surfaceHolder = mSurfaceView.getHolder();
		m_surfaceHolder.setFixedSize(mQuality.resX, mQuality.resY);
		try {
			mCamera.setPreviewDisplay(m_surfaceHolder);
		} catch (IOException e) {
			Log.e("照相机", e.getMessage());
			e.printStackTrace();
		}
		Camera.Parameters parameters = mCamera.getParameters();
		parameters.setPreviewSize(mQuality.resX, mQuality.resY);
		parameters.setPictureSize(mQuality.resX, mQuality.resY);
		parameters.setPreviewFormat(ImageFormat.NV21);
		mCamera.setParameters(parameters);
		mCamera.setPreviewCallback(new PreviewCallback() {
			@Override
			public void onPreviewFrame(byte[] data, Camera arg1) {
				if (jpegencode != null)
					jpegencode.encodeFrame(data);
			}
		});
		mCamera.startPreview();
		return parameters;
	}

	@Override
	protected void encodeWithJPEG() throws IOException {
		try {
			Thread.sleep(200);
			Camera.Parameters parameters = initJpegCamera();
			try {
				if (jpegencode == null) {
					jpegencode = new JpegEncode(mQuality.resX, mQuality.resY, parameters.getPreviewFormat());
					jpegencode.start();
					mPacketizer.setDestination(mDestination, mRtpPort, mRtcpPort);
					mPacketizer.setJpengBuffer(jpegencode.gettBuffs()); // .setInputStream(new
					mPacketizer.start();
					mStreaming = true;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
		}
	}

	@Override
	protected void encodeWithX264() throws IOException {
		try {
			Log.e("Tag", "到了");
			createCamera();
			// updateCamera();
			// unlockCamera();
			SurfaceHolder m_surfaceHolder = mSurfaceView.getHolder();
			m_surfaceHolder.setFixedSize(mQuality.resX, mQuality.resY);
			encoder = new h264encoder(mQuality.resX, mQuality.resY);

			try {
				mCamera.setPreviewDisplay(m_surfaceHolder);
			} catch (IOException e) {
				Log.e("照相机", e.getMessage());
				e.printStackTrace();
			}
			Camera.Parameters parameters = mCamera.getParameters();
			parameters.setPreviewSize(mQuality.resX, mQuality.resY);
			parameters.setPictureSize(mQuality.resX, mQuality.resY);
			parameters.setPreviewFormat(ImageFormat.YV12);
			mCamera.setParameters(parameters);
			mCamera.setPreviewCallback(new PreviewCallback() {
				private long lasttime = 0;

				@Override
				public void onPreviewFrame(byte[] data, Camera arg1) {
					com.hcm.camera.data.net.ByteBuffer byteb = encoder.getInBuffs().getInBuff();
					if (byteb != null) {
						byteb.init();
						byteb.write(data, 0, data.length);
						Log.e("编码", "编码");
						encoder.getInBuffs().commitBuff();
						lasttime = System.currentTimeMillis();
					}
				}
			});
			mCamera.startPreview();
			encoder.start();
			mPacketizer.setDestination(mDestination, mRtpPort, mRtcpPort);
			mPacketizer.setEncoderBuffer(encoder.getOutBuffs()); // .setInputStream(new
			// MediaCodecInputStream(mMediaCodec));
			mPacketizer.start();
			mStreaming = true;
		} catch (Exception e) {
			Log.e("Tag", "软编码失败,启动视频录制");
			e.printStackTrace();
		}
	}

	// private byte[] outdata = new byte[mQuality.resX * mQuality.resY]

	/**
	 * ������º�� MidaCodec����
	 */
	protected void myMidaCodecMethod() {
		try {

			// if(true){
			// throw new Exception();
			// }

			myMMbuff = new VideoGciBuffs(20, mQuality.resX * mQuality.resY * 3 / 2);
			yuv420 = new byte[mQuality.resX * mQuality.resY * 3 / 2];

			createCamera();
			// updateCamera();
			SurfaceHolder m_surfaceHolder = mSurfaceView.getHolder();
			m_surfaceHolder.setFixedSize(mQuality.resX, mQuality.resY);

			avcCodec = new AvcEncoder(mQuality.resX, mQuality.resY, mQuality.framerate, mQuality.bitrate);

			yuv420 = new byte[mQuality.resX * mQuality.resY * 3 / 2];

			try {
				mCamera.setPreviewDisplay(m_surfaceHolder);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Camera.Parameters parameters = mCamera.getParameters();
			parameters.setPreviewSize(mQuality.resX, mQuality.resY);
			parameters.setPictureSize(mQuality.resX, mQuality.resY);
			parameters.setPreviewFormat(ImageFormat.YV12);
			mCamera.setParameters(parameters);
			mCamera.setPreviewCallback(new PreviewCallback() {
				private long lasttime = 0;

				@Override
				public void onPreviewFrame(byte[] data, Camera arg1) {
					// if(System.cu)
					RTPBuffModel buff = myMMbuff.getInBuff();
					if (buff != null) {
						int ret = avcCodec.offerEncoder(data, yuv420);
						buff.write(yuv420, 0, ret);
						myMMbuff.commitBuff();
						lasttime = System.currentTimeMillis();
					}
				}
			});
			mCamera.startPreview();

			mPacketizer.setDestination(mDestination, mRtpPort, mRtcpPort);
			mPacketizer.setMyBuff(myMMbuff); // .setInputStream(new
												// MediaCodecInputStream(mMediaCodec));
			mPacketizer.start();
			mStreaming = true;
		} catch (Exception e) {
			Log.e("Tag", "Codec失败,启动视频录制");
			mPacketizer.setMyBuff(null);
			lockCamera();
			destroyCamera();
			setVideoModel(MODE_MY_JPEG);
			try {
				start();
			} catch (IllegalStateException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}
	
	/**
	 * Returns a description of the stream using SDP. This method can only be
	 * called after {@link Stream#configure()}.
	 * 
	 * @throws IllegalStateException
	 *             Thrown when {@link Stream#configure()} wa not called.
	 */
	public abstract String getSessionDescription() throws IllegalStateException;

	/**
	 * Opens the camera in a new Looper thread so that the preview callback is
	 * not called from the main thread If an exception is thrown in this Looper
	 * thread, we bring it back into the main thread.
	 * 
	 * @throws RuntimeException
	 *             Might happen if another app is already using the camera.
	 */
	private void openCamera() throws RuntimeException {
		final Semaphore lock = new Semaphore(0);
		final RuntimeException[] exception = new RuntimeException[1];
		mCameraThread = new Thread(new Runnable() {
			@Override
			public void run() {
				Looper.prepare();
				mCameraLooper = Looper.myLooper();
				try {
					mCamera = Camera.open(mCameraId);
					// mCamera.setDisplayOrientation(90);
				} catch (RuntimeException e) {
					exception[0] = e;
				} finally {
					lock.release();
					Looper.loop();
				}
			}
		});
		mCameraThread.start();
		lock.acquireUninterruptibly();
		if (exception[0] != null)
			throw new CameraInUseException(exception[0].getMessage());
	}

	protected synchronized void createCamera() throws RuntimeException {
		if (mSurfaceView == null)
			throw new InvalidSurfaceException("Invalid surface !");
		if (mSurfaceView.getHolder() == null || !mSurfaceReady)
			throw new InvalidSurfaceException("Invalid surface !");

		if (mCamera == null) {
			openCamera();
			mUnlocked = false;
			mCamera.setErrorCallback(new Camera.ErrorCallback() {
				@Override
				public void onError(int error, Camera camera) {
					// On some phones when trying to use the camera facing front
					// the media server will die
					// Whether or not this callback may be called really depends
					// on the phone
					if (error == Camera.CAMERA_ERROR_SERVER_DIED) {
						// In this case the application must release the camera
						// and instantiate a new one
						Log.e(TAG, "Media server died !");
						// We don't know in what thread we are so stop needs to
						// be synchronized
						mCameraOpenedManually = false;
						stop();
					} else {
						Log.e(TAG, "Error unknown with the camera: " + error);
					}
				}
			});

			try {
				Parameters parameters = mCamera.getParameters();
				if (parameters.getFlashMode() != null) {
					parameters.setFlashMode(mFlashEnabled ? Parameters.FLASH_MODE_TORCH : Parameters.FLASH_MODE_OFF);
				}
				parameters.setRecordingHint(true);
				mCamera.setParameters(parameters);
				mCamera.setDisplayOrientation(mOrientation);
			} catch (RuntimeException e) {
				destroyCamera();
				throw e;
			}

		}
	}

	protected synchronized void destroyCamera() {
		if (mCamera != null) {
			if (mStreaming)
				super.stop();
			lockCamera();
			mCamera.stopPreview();
			try {
				mCamera.release();
			} catch (Exception e) {
				Log.e(TAG, e.getMessage() != null ? e.getMessage() : "unknown error");
			}
			mCamera = null;
			mCameraLooper.quit();
			mUnlocked = false;
			mPreviewStarted = false;
		}
	}

	protected synchronized void updateCamera() throws RuntimeException {
		if (mPreviewStarted) {
			mPreviewStarted = false;
			mCamera.stopPreview();
		}

		Parameters parameters = mCamera.getParameters();
		mQuality = VideoQuality.determineClosestSupportedResolution(parameters, mQuality);
		int[] max = VideoQuality.determineMaximumSupportedFramerate(parameters);
		parameters.setPreviewFormat(mCameraImageFormat);
		parameters.setPreviewSize(mQuality.resX, mQuality.resY);
		parameters.setPreviewFpsRange(max[0], max[1]);

		try {
			mCamera.setParameters(parameters);
			mCamera.setDisplayOrientation(mOrientation);
			mCamera.startPreview();
			mPreviewStarted = true;
		} catch (RuntimeException e) {
			destroyCamera();
			throw e;
		}
	}

	protected void lockCamera() {
		if (mUnlocked) {
			Log.d(TAG, "Locking camera");
			try {
				mCamera.reconnect();
			} catch (Exception e) {
				Log.e(TAG, e.getMessage());
			}
			mUnlocked = false;
		}
	}

	protected void unlockCamera() {
		if (!mUnlocked) {
			Log.d(TAG, "Unlocking camera");
			try {
				mCamera.unlock();
			} catch (Exception e) {
				Log.e(TAG, e.getMessage());
			}
			mUnlocked = true;
		}
	}

	/**
	 * Computes the average frame rate at which the preview callback is called.
	 * We will then use this average framerate with the MediaCodec. Blocks the
	 * thread in which this function is called.
	 */
	private void measureFramerate() {
		final Semaphore lock = new Semaphore(0);

		final Camera.PreviewCallback callback = new Camera.PreviewCallback() {
			int i = 0, t = 0;
			long now, oldnow, count = 0;

			@Override
			public void onPreviewFrame(byte[] data, Camera camera) {
				i++;
				now = System.nanoTime() / 1000;
				if (i > 3) {
					t += now - oldnow;
					count++;
				}
				if (i > 20) {
					mQuality.framerate = (int) (1000000 / (t / count) + 1);
					lock.release();
				}
				oldnow = now;
			}
		};

		mCamera.setPreviewCallback(callback);

		try {
			lock.tryAcquire(2, TimeUnit.SECONDS);
			Log.d(TAG, "Actual framerate: " + mQuality.framerate);
			if (mSettings != null) {
				Editor editor = mSettings.edit();
				editor.putInt(PREF_PREFIX + "fps" + mRequestedQuality.framerate + "," + mCameraImageFormat + "," + mRequestedQuality.resX + mRequestedQuality.resY, mQuality.framerate);
				editor.commit();
			}
		} catch (InterruptedException e) {
		}

		mCamera.setPreviewCallback(null);

	}

	private void swapYV12toI420(byte[] yv12bytes, byte[] i420bytes, int width, int height) {
		System.arraycopy(yv12bytes, 0, i420bytes, 0, width * height);
		System.arraycopy(yv12bytes, width * height + width * height / 4, i420bytes, width * height, width * height / 4);
		System.arraycopy(yv12bytes, width * height, i420bytes, width * height + width * height / 4, width * height / 4);
	}

	public synchronized void zStop() {
		if (mCamera != null) {
			mCamera.setPreviewCallback(null);
			mCamera.release();
			mCamera = null;
		}
	}

	public synchronized void zStart(SurfaceView view) {
		setSurfaceView(view);
		initJpegCamera();
	}

}
