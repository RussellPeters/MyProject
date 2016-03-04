package com.hcm.camera.data.net;

public interface OnNetWorkLinstener {
	public void onLinkSuccess();
	public void onTimeOut();
	public void onNeedReLogin();
	public void onReLine();
}
