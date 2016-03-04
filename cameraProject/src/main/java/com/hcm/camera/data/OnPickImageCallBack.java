package com.hcm.camera.data;

import android.graphics.Bitmap;

public abstract class OnPickImageCallBack {
	public abstract void callback(String path, Bitmap map);
	public  void error(int errorcode,String errormessage) {}
}
