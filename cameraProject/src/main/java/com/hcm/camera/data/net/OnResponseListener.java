package com.hcm.camera.data.net;

public abstract class OnResponseListener {

	public abstract void res(String json, Object sender);

	public void onerror(String error, Object sender) {
	}

	public void onerror(int errorcode, String error, Object sender) {
	}
}
