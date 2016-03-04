package com.hcm.camera.netfile;

import java.io.File;

public interface UploadFileListener {
	public void OnStart();

	public void OnUploading(int postion, File file);

	public void OnError(int errcode, String errormessage);

	public void OnFisish(File file, String requestStr);
}
