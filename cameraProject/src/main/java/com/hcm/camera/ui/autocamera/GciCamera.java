package com.hcm.camera.ui.autocamera;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Semaphore;

import com.hcm.camera.video.stream.exception.CameraInUseException;


import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Build;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

public class GciCamera {
	private static GciCamera _carmera = null;
	private Thread mCameraThread = null;
	private Looper mCameraLooper;
	private int mCameraId = -10086;
	private Camera mCamera;
	private CameraConfigurationManager config = null;
	private GciCameraPreviewAutoFoucsCallBack callback;
	public static final int CAMERA_FACE = 1;
	public static final int CAMREA_BACK = 2;
	public static final int CAMREA_DEFAULT = 0;

	private static CameraManager cameraManager;

	private static Context con;

	static final int SDK_INT; // Later we can use Build.VERSION.SDK_INT
	static {
		int sdkInt;
		try {
			sdkInt = Integer.parseInt(Build.VERSION.SDK);
		} catch (NumberFormatException nfe) {
			// Just to be safe
			sdkInt = 10000;
		}
		SDK_INT = sdkInt;
	}

	public static GciCamera getIntance(Context con) {
		if (_carmera == null) {
			_carmera = new GciCamera();
			GciCamera.con = con;
		}
		return _carmera;
	}

	private Callback mSurfaceViewCallBack = new Callback() {

		@Override
		public void surfaceDestroyed(SurfaceHolder arg0) {
			destroyCamera();
		}

		@Override
		public void surfaceCreated(SurfaceHolder m_surfaceHolder) {
			try {
				initCarmra(m_surfaceHolder);
			} catch (Exception e) {
			}
		}

		@Override
		public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2,
				int arg3) {
		}
	};

	public void startCarmraImage(int cameraModel, SurfaceView surface,
			GciCameraPreviewAutoFoucsCallBack callback) {
		try {
			switch (cameraModel) {
			case 1:
				this.mCameraId = getCameraFontId();
				break;
			}
			if (surface != null && surface.getHolder() != null) {
				if (surface.getHolder().isCreating())
					surface.getHolder().addCallback(mSurfaceViewCallBack);
				else
					initCarmra(surface.getHolder());
			} else {
				initCarmra(null);
			}
			this.callback = callback;
		} catch (Exception e) {
			if (this.callback != null) {
				this.callback.onCameraError();
			}
		}
	}

	private void initCarmra(SurfaceHolder m_surfaceHolder) throws Exception {
		try {
			createCamera();
			try {
				if (m_surfaceHolder != null)
					mCamera.setPreviewDisplay(m_surfaceHolder);
			} catch (IOException e) {
				e.printStackTrace();
			}
			Camera.Parameters parameters = mCamera.getParameters();
			parameters.setPreviewSize(640, 480);
			parameters.setPictureSize(640, 480);
			parameters.setPreviewFormat(ImageFormat.NV21);
			// parameters.setPictureFormat(ImageFormat.NV21);
			mCamera.setParameters(parameters);
			// if(config == null){
			// config = new CameraConfigurationManager(GciCamera.con);
			// }
			// config.setDesiredCameraParameters(mCamera);
			//
			mCamera.setPreviewCallback(new PreviewCallback() {
				@Override
				public void onPreviewFrame(byte[] data, Camera camera) {
					if (callback != null)
						callback.onFramePic(data, camera);
				}
			});

//			mCamera.autoFocus(new AutoFocusCallback() {
//				@Override
//				public void onAutoFocus(boolean success, Camera camera) {
//					if (callback != null)
//						callback.onAutoFouce();
//				}
//			});

			mCamera.startPreview();
		} catch (Exception e) {
			throw e;
		}
	}

	private void openCamera() throws RuntimeException {
		final Semaphore lock = new Semaphore(0);
		final RuntimeException[] exception = new RuntimeException[1];
		mCameraThread = new Thread(new Runnable() {
			@Override
			public void run() {
				Looper.prepare();
				mCameraLooper = Looper.myLooper();
				try {
					mCamera = Camera.open();
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

	private synchronized void createCamera() throws RuntimeException {
		if (mCamera == null) {
			openCamera();
			try {
				Parameters parameters = mCamera.getParameters();
				if (parameters.getFlashMode() != null) {
					parameters.setFlashMode(false ? Parameters.FLASH_MODE_TORCH
							: Parameters.FLASH_MODE_OFF);
				}
				parameters.setRecordingHint(true);
				mCamera.setParameters(parameters);
				mCamera.setDisplayOrientation(0);
			} catch (RuntimeException e) {
				destroyCamera();
				throw e;
			}

		}
	}

	public synchronized void destroyCamera() {
		if (mCamera != null) {
			mCamera.setPreviewCallbackWithBuffer(null);
			mCamera.stopPreview();
			try {
				mCamera.release();
			} catch (Exception e) {
			}
			mCamera = null;
			mCameraLooper.quit();
		}
	}

	private int getCameraFontId() {
		int carmraId = -10086;
		// if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
		CameraInfo info = new CameraInfo();
		for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
			Camera.getCameraInfo(i, info);
			if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
				carmraId = i;
				break;
			}
		}
		// }
		return carmraId;
	}
}
