package com.hcm.camera.net.model.Commun;

import android.graphics.Bitmap;

public class SendMessageModel {
	public int Type;

	public String SendId;

	public String ReceiveId;

	public String Message;

	public int Seq = 0;

	public String MessageId;

	public transient boolean isLoadOk = true;

	public int Audio_Timer = 0;
	
	public transient String Audio_Path;
	
	public String FileName;
	
	public String PicFileName;
	
	public transient boolean isDownLoading = false;
	
	public transient String PicFilePath;
	public transient Bitmap PicBitMap;
	
	public transient static final int MESSAGE_TYPE_MESSAGE = 0;
	public transient static final int MESSAGE_TYPE_AUDIO = 1;
	public transient static final int MESSAGE_TYPE_IMAGE = 2;
}
