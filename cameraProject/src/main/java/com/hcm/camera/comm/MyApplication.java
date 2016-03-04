package com.hcm.camera.comm;

import com.gci.nutil.base.BaseApplication;
import com.hcm.camera.data.net.NetServer;
import com.hcm.camera.data.net.OnResponseListener;
import com.hcm.camera.net.model.AppSendModel;
import com.hcm.camera.net.model.system.SendLog;
import com.hcm.camera.net.model.system.SystemMsgType;

public class MyApplication extends BaseApplication {
	@Override
	public void OnSystemError(String str) {
		SendLog data = new SendLog();
		data.ErrorTitle = "崩溃性质的异常";
		data.ErrInfo = str;
		if(!NetServer.getIntance().getSocket().isLostLine){
			AppSendModel send = NetServer.getIntance().getSendModel(SystemMsgType.MSG_LOG, data);
			NetServer.getIntance().getSocket().AddMessagePairListener(send, new OnResponseListener() {
				@Override
				public void res(String json, Object sender) {
				}
			});
		}
	}
}
