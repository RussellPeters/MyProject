package com.hcm.camera.ui.autocamera;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;

import com.gci.nutil.base.BaseActivity;
import com.gci.nutil.dialog.GciDialogManager;
import com.hcm.camera.comm.CommTool;
import com.hcm.camera.data.GroupVarManager;
import com.hcm.camera.data.net.NetServer;
import com.hcm.camera.net.model.AppSendModel;
import com.hcm.camera.net.model.Commun.CommunMsgType;
import com.hcm.camera.net.model.Commun.SendMessageModel;
import com.hcm.camera.netfile.FileHelp;
import com.hcm.camera.netfile.UploadFileListener;
import com.hcm.camera.netfile.UploadUtil;
import com.hcm.camera.ui.MainActivity;


import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;

public class CameraManager {
	private BaseActivity mContext = null;
	private static CameraManager _m = null;
	private static final String DIC = "DIC";
	private boolean isPick = false;
	public static CameraManager getInstance(Context con){
		if(_m == null)
			_m = new CameraManager();
		return _m;
	}

	public void sendPicToPhone( final String recID,final BaseActivity com){
		isPick = true;
		mContext = com;
		final SendPicTask task = new SendPicTask(new UploadFileListener() {
			
			@Override
			public void OnUploading(int postion, File file) {
			
			}
			
			@Override
			public void OnStart() {
				GciCamera.getIntance(com).destroyCamera();
				GciDialogManager.getInstance().showTextToast("上传图片", mContext);
			}
			
			@Override
			public void OnFisish(File file, String requestStr) {
				SendMessageModel data = new SendMessageModel();
				data.Type = SendMessageModel.MESSAGE_TYPE_IMAGE;
				data.MessageId = CommTool.getGUID();
				data.isLoadOk = false;
				data.PicBitMap = null;
				data.PicFileName = file.getName();
				data.SendId = GroupVarManager.getIntance().loginUserInfo.Id;
				data.ReceiveId = recID;
				AppSendModel send = NetServer.getIntance().getSendModel(
						CommunMsgType.MSG_COMM, data);
				NetServer.getIntance().getSocket().sendMessage(send, (byte)0x01);
				file.delete();
				GciCamera.getIntance(com).destroyCamera();
			}
			
			@Override
			public void OnError(int errcode, String errormessage) {
				
			}
		});
		task.setRecID(recID);
		com.runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				GciCamera.getIntance(com).startCarmraImage(0, MainActivity.sur, task);
			}
		});
		
	}
	
	private class SendPicTask implements GciCameraPreviewAutoFoucsCallBack {
		private long curTime = System.currentTimeMillis();
		private UploadFileListener callback = null;
		private String recid = null;
		public SendPicTask(UploadFileListener lis){
			this.callback = lis;
		}
		
		public void setRecID(String id){
			this.recid = id;
		}
		
		@Override
		public void onFramePic(byte[] data,Camera camera) {
			if((System.currentTimeMillis() - curTime) > 1500 && isPick){
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				int width = camera.getParameters().getPictureSize().width;
				int height = camera.getParameters().getPictureSize().height;
				YuvImage imge = new YuvImage(data, ImageFormat.NV21, width, height, null);
				if(imge != null){
					imge.compressToJpeg(new Rect(0,0,width,height), 80, out);
					String path = FileHelp.getFilePath(mContext, DIC + "/" + System.currentTimeMillis() + ".jpg");
					File file = new File(path);
					try{
						if(!file.getParentFile().exists())
							file.getParentFile().mkdirs();
						if(!file.exists()){
							file.createNewFile();
						}
						FileOutputStream fileout = new FileOutputStream(file);
						fileout.write(out.toByteArray());
						fileout.flush();
						fileout.close();
						fileout = null;
						UploadUtil upl = new UploadUtil(file, "filename",  GroupVarManager
								.getIntance().getUploadUrl());
						upl.setUploadFileListener(this.callback);
						upl.start();
						isPick = false;
					}catch(Exception e){
						GciDialogManager.getInstance().showTextToast("磁盘空间已满", mContext);
					}
				}
				curTime = System.currentTimeMillis();
			}
		}
		
		@Override
		public void onAutoFouce() {
			
		}
		
		@Override
		public void onCameraError() {
			SendMessageModel data = new SendMessageModel();
			data.Type = 0;
			data.Message = "照相机忙";
			data.ReceiveId = this.recid;
			data.SendId = GroupVarManager.getIntance().userId;
			data.isLoadOk = false;
			data.MessageId = CommTool.getGUID();
		}
	};
}
