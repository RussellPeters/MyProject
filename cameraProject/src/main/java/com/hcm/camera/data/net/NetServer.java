package com.hcm.camera.data.net;

import android.content.Context;

import com.gci.nutil.activity.GciActivityManager;
import com.gci.nutil.dialog.GciDialogManager;
import com.google.gson.Gson;
import com.hcm.camera.comm.CommTool;
import com.hcm.camera.net.model.AppSendModel;
import com.hcm.camera.net.model.Commun.VideoSharePreference;

public class NetServer {
	private static NetServer _m;
	private NetWorkSocket socket;
	private Gson gson = new Gson();
	private String Ip = "";
	private Context con;

	private NetServer() {
	}

	public static NetServer getIntance() {
		if (_m == null) {
			_m = new NetServer();
		}
		return _m;
	}

	public NetWorkSocket getSocket() {
		return this.socket;
	}

	public void Start(Context context) {
		Ip = VideoSharePreference.getInstance(context).getIpAddress();
		_m.socket = new NetWorkSocket(Ip);
		new Thread(new Runnable() {
			@Override
			public void run() {
				_m.socket.StartServer(new OnNetWorkLinstener() {

					@Override
					public void onTimeOut() {
						GciDialogManager.getInstance().showTextToast("网络连接不稳定", GciActivityManager.getInstance().getLastActivity());
					}

					@Override
					public void onReLine() {
						GciDialogManager.getInstance().showTextToast("网络已连接", GciActivityManager.getInstance().getLastActivity());
					}

					@Override
					public void onNeedReLogin() {
						// TODO Auto-generated method stub

					}

					@Override
					public void onLinkSuccess() {
						// TODO Auto-generated method stub

					}
				});
			}
		}).start();
	}

	public void Restart(Context context) {
		Start(context);
	}

	public AppSendModel getSendModel(String Cmd, Object data) {
		String json = gson.toJson(data);
		AppSendModel result = new AppSendModel();
		result.MsgType = Cmd;
		result.JsonString = json;
		result.Seq = CommTool.getWorkSegByFunctionNo(Cmd, Cmd);
		return result;
	}
}
