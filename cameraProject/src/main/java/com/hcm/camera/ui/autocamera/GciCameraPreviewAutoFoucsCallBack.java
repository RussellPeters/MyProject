package com.hcm.camera.ui.autocamera;

import android.hardware.Camera;

public interface GciCameraPreviewAutoFoucsCallBack {
	public void onFramePic(byte[] data,Camera camera);
	public void onAutoFouce();
	public void onCameraError();
}
