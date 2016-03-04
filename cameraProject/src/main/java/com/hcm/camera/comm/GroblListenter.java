package com.hcm.camera.comm;

import android.content.Intent;
import android.util.Log;

import com.gci.nutil.Base64;
import com.gci.nutil.activity.GciActivityManager;
import com.hcm.camera.bluetooth.BluetoothDriverManager;
import com.hcm.camera.bluetooth.BluetoothMessageManager;
import com.hcm.camera.data.GroupVarManager;
import com.hcm.camera.data.net.NetServer;
import com.hcm.camera.data.net.OnResponseListener;
import com.hcm.camera.net.model.AppSendModel;
import com.hcm.camera.net.model.Commun.CommunMsgType;
import com.hcm.camera.net.model.Commun.ResponseMessageModel;
import com.hcm.camera.net.model.system.RecPushPic;
import com.hcm.camera.net.model.system.ResponseBluetoohCmd;
import com.hcm.camera.net.model.system.SystemMsgType;
import com.hcm.camera.net.model.video.ResponseEndVideo;
import com.hcm.camera.net.model.video.ResponsePushVideo;
import com.hcm.camera.net.model.video.SendVideoConfig;
import com.hcm.camera.net.model.video.VideoMsgType;
import com.hcm.camera.ui.CameraVideoActivity;
import com.hcm.camera.ui.ChatActivity;
import com.hcm.camera.ui.DecoderVideoActivity;
import com.hcm.camera.ui.MainActivity;
import com.hcm.camera.ui.autocamera.CameraManager;

public class GroblListenter {
	public GroblListenter() {
		addVideoListener();
		addVideoEndListener();
		addPushMessageListenter();
		addPushVideoConfig();
		addBluetoohCmd();
		addBluetoohResponseCmd();
		addPicPush();
	}

	private void addVideoListener() {
		NetServer.getIntance().getSocket().AddListener(VideoMsgType.MSG_PUSH_VIDEO, VideoMsgType.MSG_PUSH_VIDEO, 0, new OnResponseListener() {
			@Override
			public void res(String json, Object sender) {
				ResponsePushVideo push = CommTool.getInstanceByJson(json, ResponsePushVideo.class);
				Intent intent = new Intent(GciActivityManager.getInstance().getLastActivity(), CameraVideoActivity.class);
				intent.putExtra("TaskId", push.TaskId);
				GciActivityManager.getInstance().getLastActivity().startActivity(intent);
			}
		});
	}

	private void addVideoEndListener() {
		NetServer.getIntance().getSocket().AddListener(VideoMsgType.MSG_END_VIDEO, VideoMsgType.MSG_END_VIDEO, 0, new OnResponseListener() {
			@Override
			public void res(String json, Object sender) {
				ResponseEndVideo v = CommTool.getInstanceByJson(json, ResponseEndVideo.class);
				if (v != null) {
					if (GciActivityManager.getInstance().getLastActivity().getClass().getName().equals(DecoderVideoActivity.class.getName())) {
						GciActivityManager.getInstance().getLastActivity().finish();
					} else if (GciActivityManager.getInstance().getLastActivity().getClass().getName().equals(CameraVideoActivity.class.getName())) {
						GciActivityManager.getInstance().getLastActivity().finish();
					}
				}
			}
		});
	}

	private void addPushMessageListenter() {
		NetServer.getIntance().getSocket().AddListener(CommunMsgType.MSG_PUSH_COMM, CommunMsgType.MSG_PUSH_COMM, 0, new OnResponseListener() {
			@Override
			public void res(String json, Object sender) {
				Log.e("Tag", json);
				final ResponseMessageModel res = CommTool.getInstanceByJson(json, ResponseMessageModel.class);
				if (res != null) {
					Class<?> cura = GciActivityManager.getInstance().getLastActivity().getClass();
					if (cura.getName().equals(ChatActivity.class.getName())) {
						final ChatActivity chat = (ChatActivity) GciActivityManager.getInstance().getLastActivity();
						if (chat.userid.equals(res.SendId)) {
							chat.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									chat.addItemAndUpdateUI(res);
								}
							});
							return;
						}
					}

					GciActivityManager.getInstance().getActivityByClassName(MainActivity.class.getName());

					GroupVarManager.getIntance().chats.add(res);
					// 假设是上位机
					if (GroupVarManager.getIntance().loginUserInfo.UserType == 0) {
						Intent intent = new Intent(GciActivityManager.getInstance().getLastActivity(), ChatActivity.class);
						intent.putExtra("UserId", res.SendId);
						GciActivityManager.getInstance().getLastActivity().startActivity(intent);
					}
				}
			}
		});
	}

	private void addPushVideoConfig() {
		NetServer.getIntance().getSocket().AddListener(VideoMsgType.MSG_PUSH_VIDEO_CONFIG, VideoMsgType.MSG_PUSH_VIDEO_CONFIG, 0, new OnResponseListener() {
			@Override
			public void res(String json, Object sender) {
				GciActivityManager.getInstance().getLastActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						GciActivityManager.getInstance().getLastActivity().canelLoading();
					}
				});
				if (!GciActivityManager.getInstance().getLastActivity().getClass().getName().equals(DecoderVideoActivity.class.getName())) {
					SendVideoConfig config = CommTool.getInstanceByJson(json, SendVideoConfig.class);
					Intent intent = new Intent(GciActivityManager.getInstance().getLastActivity(), DecoderVideoActivity.class);
					intent.putExtra("config", config);
					GciActivityManager.getInstance().getLastActivity().startActivity(intent);
				}
			}
		});
	}

	private void addBluetoohCmd() {
		NetServer.getIntance().getSocket().AddListener(SystemMsgType.MSG_PUSH_CMD, SystemMsgType.MSG_PUSH_CMD, 0, new OnResponseListener() {
			@Override
			public void res(String json, Object sender) {
				ResponseBluetoohCmd cmd = CommTool.gson.fromJson(json, ResponseBluetoohCmd.class);
				try {
					BluetoothDriverManager.getIntance().setSendServer(true);
					BluetoothDriverManager.getIntance().setUserId(cmd.SendId);
					BluetoothDriverManager.getIntance().sendMessage(Base64.decodeToByteArray(cmd.cmd));
					cmd.cmd = "下位机(" + GroupVarManager.getIntance().userId + ")->收到命令";
					String temp = cmd.ReceiveId;
					cmd.ReceiveId = cmd.SendId;
					cmd.SendId = temp;
					AppSendModel send = NetServer.getIntance().getSendModel(SystemMsgType.MSG_RESPONSE_CMD, cmd);
					NetServer.getIntance().getSocket().sendMessage(send, (byte) 0x01);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}

	private void addBluetoohResponseCmd() {
		NetServer.getIntance().getSocket().AddListener(SystemMsgType.MSG_RESPONSE_CMD, SystemMsgType.MSG_RESPONSE_CMD, 0, new OnResponseListener() {
			@Override
			public void res(String json, Object sender) {
				ResponseBluetoohCmd obj = CommTool.gson.fromJson(json, ResponseBluetoohCmd.class);
				BluetoothMessageManager.getInstance().addMessage(obj.cmd);
			}
		});
	}

	private void addPicPush() {
		NetServer.getIntance().getSocket().AddListener(SystemMsgType.MSG_PIC, SystemMsgType.MSG_PIC, 0, new OnResponseListener() {
			@Override
			public void res(String json, Object sender) {
				RecPushPic obj = CommTool.gson.fromJson(json, RecPushPic.class);
				CameraManager.getInstance(GciActivityManager.getInstance().getLastActivity()).sendPicToPhone(obj.SendId, GciActivityManager.getInstance().getLastActivity());
			}
		});
	}
}
