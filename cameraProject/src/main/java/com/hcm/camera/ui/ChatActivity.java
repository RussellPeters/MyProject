package com.hcm.camera.ui;

import java.io.File;
import java.security.acl.Group;
import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.opengl.Visibility;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings.Global;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.style.BulletSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.fanxin.app.widget.PasteEditText;
import com.gci.nutil.AppUtil;
import com.gci.nutil.OnViewClickListenter;
import com.gci.nutil.activity.GciActivityManager;
import com.gci.nutil.base.BaseActivity;
import com.gci.nutil.base.BaseGciAdapter;
import com.gci.nutil.base.callbackinterface.OnMessageBoxClickListener;
import com.gci.nutil.comm.CommonTool;
import com.gci.nutil.control.pulluprefash.PullToRefreshListView;
import com.gci.nutil.dialog.GciDialogManager;
import com.gci.nutil.dialog.GciDialogManager2;
import com.hcm.camera.R;
import com.hcm.camera.bluetooth.BluetoothDriverManager;
import com.hcm.camera.comm.CommTool;
import com.hcm.camera.comm.ImageUntil;
import com.hcm.camera.data.GroupVarManager;
import com.hcm.camera.data.OnPickImageCallBack;
import com.hcm.camera.data.net.NetServer;
import com.hcm.camera.data.net.OnResponseListener;
import com.hcm.camera.net.model.AppSendModel;
import com.hcm.camera.net.model.Commun.CommunMsgType;
import com.hcm.camera.net.model.Commun.ResponseMessageModel;
import com.hcm.camera.net.model.Commun.SendMessageModel;
import com.hcm.camera.net.model.system.RecPushPic;
import com.hcm.camera.net.model.system.SystemMsgType;
import com.hcm.camera.net.model.video.SendVideo;
import com.hcm.camera.net.model.video.VideoMsgType;
import com.hcm.camera.netfile.UploadFileListener;
import com.hcm.camera.netfile.UploadUtil;
import com.hcm.camera.ui.adapter.ChatsAdapter;
import com.kubility.demo.MP3Recorder;


public class ChatActivity extends BaseActivity {
	private ImageView iv_back; // �˳�������
	private ImageView iv_setting;
	public String userid = "";

	private PasteEditText et_sendmessage;
	private Button btn_send;
	private Button btn_set_mode_voice;
	private Button btn_more;
	private TextView tv_audio;
	private Button btn_set_mode_keyboard;
	private RelativeLayout recording_container;
	private ImageView mic_image;
	private LinearLayout ll_btn_container;

	private ImageView btn_take_picture;

	private ImageView btn_picture;

	private ImageView btn_cmd;

	private int messageType = 0;

	public ChatsAdapter adapter;
	private ListView list;

	private MP3Recorder mp3;

	@Override
	protected void onCreate(Bundle changestate) {
		super.onCreate(changestate);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		// ������������ͼ
		setContentView(R.layout.activity_chat);

		initControl();

		initListenter();

		initData();

		// 判断是否是下位机，如果是下位机，设置蓝牙监听
		if (GroupVarManager.getIntance().loginUserInfo.UserType == 0)
			BluetoothDriverManager.getIntance().setHandlerCallBacl();
	}

	private void initControl() {
		iv_setting = GetControl(R.id.iv_setting);
		iv_back = GetControl(R.id.iv_back);
		et_sendmessage = GetControl(R.id.et_sendmessage);
		btn_send = GetControl(R.id.btn_send);
		list = GetControl(R.id.list);
		btn_set_mode_voice = GetControl(R.id.btn_set_mode_voice);
		btn_more = GetControl(R.id.btn_more);
		tv_audio = GetControl(R.id.tv_audio);
		btn_set_mode_keyboard = GetControl(R.id.btn_set_mode_keyboard);
		recording_container = GetControl(R.id.recording_container);
		mic_image = GetControl(R.id.mic_image);
		ll_btn_container = GetControl(R.id.ll_btn_container);
		btn_take_picture = GetControl(R.id.btn_take_picture);
		btn_picture = GetControl(R.id.btn_picture);
		btn_cmd = GetControl(R.id.btn_cmd);

		if (GroupVarManager.getIntance().loginUserInfo.UserType == 0)
			iv_setting.setVisibility(View.GONE);
		// AnimationDrawable ani = (AnimationDrawable)
		// mic_image.getBackground();
		// ani.start();

		adapter = new ChatsAdapter(list, this);

		if (GroupVarManager.getIntance().loginUserInfo.UserType == 0)
			((LinearLayout) findViewById(R.id.blueCmd)).setVisibility(View.GONE);
	}

	private void initListenter() {
		iv_back.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				ChatActivity.this.finish();
			}
		});

		btn_set_mode_keyboard.setOnClickListener(onAudioModeChangeClick);
		btn_set_mode_voice.setOnClickListener(onAudioModeChangeClick);

		et_sendmessage.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence str, int arg1, int arg2, int arg3) {
				if ("".equals(str.toString())) {
					btn_send.setVisibility(View.GONE);
					btn_more.setVisibility(View.VISIBLE);
				} else {
					btn_send.setVisibility(View.VISIBLE);
					btn_more.setVisibility(View.GONE);
				}
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
			}

			@Override
			public void afterTextChanged(Editable arg0) {
			}
		});

		iv_setting.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				SendVideo data = new SendVideo();
				data.NextPhoneId = userid;
				AppSendModel send = NetServer.getIntance().getSendModel(VideoMsgType.MSG_VIDEO_NEXT_PHONE, data);
				NetServer.getIntance().getSocket().AddMessagePairListener(send, new OnResponseListener() {
					@Override
					public void res(String json, Object sender) {
						GciDialogManager2.getInstance().showLoading("正在处理...", ChatActivity.this, null);
					}

					@Override
					public void onerror(String error, Object sender) {
						GciDialogManager.getInstance().showMessageBox("提示", error, false, new OnMessageBoxClickListener() {
							@Override
							public void onClickOK() {
								return;
							}
						}, ChatActivity.this, null);
					}
				});
			}
		});

		btn_send.setOnClickListener(onClickSendMessage);

		tv_audio.setOnTouchListener(onAudioTouch);

		btn_more.setOnClickListener(onAddMoreClick);

		btn_take_picture.setOnClickListener(onCameraImageClick);

		btn_picture.setOnClickListener(onPickImageClick);

		btn_cmd.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(ChatActivity.this, CmdActivity.class);
				intent.putExtra("isSendServer", true);
				intent.putExtra("userid", userid);
				startActivity(intent);
				// BluetoothComment.getIntance().showCmd(ChatActivity.this,
				// userid, GroupVarManager.getIntance().userId);
			}
		});
	}

	private OnClickListener onCameraImageClick = new OnClickListener() {
		@Override
		public void onClick(View arg0) {
			ImageUntil.getIntance(ChatActivity.this).camreaImage(onPickSendImage);
		}
	};

	private OnClickListener onPickImageClick = new OnClickListener() {
		@Override
		public void onClick(View arg0) {
			if (GroupVarManager.getIntance().loginUserInfo.UserType == 0)
				ImageUntil.getIntance(ChatActivity.this).pickImage(onPickSendImage);
			else {
				RecPushPic data = new RecPushPic();
				data.SendId = GroupVarManager.getIntance().userId;
				data.ReceiveId = userid;
				AppSendModel send = NetServer.getIntance().getSendModel(SystemMsgType.MSG_PIC, data);
				NetServer.getIntance().getSocket().sendMessage(send, (byte) 0x01);
			}
		}
	};

	private OnPickImageCallBack onPickSendImage = new OnPickImageCallBack() {
		@Override
		public void callback(String path, Bitmap map) {
			if (map != null) {
				SendMessageModel data = new SendMessageModel();
				data.Type = SendMessageModel.MESSAGE_TYPE_IMAGE;
				data.MessageId = CommTool.getGUID();
				data.isLoadOk = false;
				data.PicBitMap = map;
				data.PicFileName = new File(path).getName();
				data.SendId = GroupVarManager.getIntance().loginUserInfo.Id;
				data.ReceiveId = userid;
				addItemAndUpdateUI(data);
				sendImagePic(data, path);
			}
		}

		@Override
		public void error(int errorcode, String errormessage) {
			GciDialogManager.getInstance().showTextToast(errormessage, ChatActivity.this);
		}
	};

	private OnClickListener onAddMoreClick = new OnClickListener() {
		@Override
		public void onClick(View arg0) {
			if (ll_btn_container.getVisibility() == View.GONE)
				hideOrShowPanl(true);
			else
				hideOrShowPanl(false);
		}
	};

	private void hideOrShowPanl(boolean show) {
		if (show) {
			ll_btn_container.setVisibility(View.VISIBLE);
			if (et_sendmessage.hasFocus() && AppUtil.isSoftInout(ChatActivity.this)) {
				AppUtil.closeSoftInput(ChatActivity.this);
			}
		} else {
			ll_btn_container.setVisibility(View.GONE);
			if (et_sendmessage.hasFocus()) {
				AppUtil.showSoftInput(ChatActivity.this);
			}
		}
	}

	private OnViewClickListenter onClickSendMessage = new OnViewClickListenter(500) {
		@Override
		public void onClick(boolean isPass, View view) {
			if ("".equals(et_sendmessage.getText()))
				return;

			SendMessageModel data = new SendMessageModel();
			data.Type = messageType;
			switch (messageType) {
			case 0:
				data.Message = et_sendmessage.getText().toString();
				data.ReceiveId = userid;
				data.SendId = GroupVarManager.getIntance().userId;
				data.isLoadOk = false;
				data.MessageId = CommTool.getGUID(); // += // //
														// (++GroupVarManager.getIntance().Seq);
				break;
			default:
				break;
			}
			sendMessage(data);
			addItemAndUpdateUI(data);
		}
	};

	private OnClickListener onAudioModeChangeClick = new OnClickListener() {
		@Override
		public void onClick(View view) {
			if (view.getId() == R.id.btn_set_mode_voice) {
				btn_set_mode_keyboard.setVisibility(View.VISIBLE);
				tv_audio.setVisibility(View.VISIBLE);
				btn_more.setVisibility(View.VISIBLE);

				btn_set_mode_voice.setVisibility(View.GONE);
				et_sendmessage.setVisibility(View.GONE);
				btn_send.setVisibility(View.GONE);
			} else {
				btn_set_mode_voice.setVisibility(View.VISIBLE);
				et_sendmessage.setVisibility(View.VISIBLE);
				btn_send.setVisibility(View.VISIBLE);

				btn_set_mode_keyboard.setVisibility(View.GONE);
				tv_audio.setVisibility(View.GONE);
				btn_more.setVisibility(View.GONE);
			}
		}
	};

	private OnResponseListener messageResponse = new OnResponseListener() {
		@Override
		public void res(String json, Object obj) {
			ResponseMessageModel message = CommTool.getInstanceByJson(json, ResponseMessageModel.class);
			SendMessageModel send = adapter.selectOrDefault(message.MessageId);
			if (send != null) {
				switch (message.Type) {
				case 0:
					send.isLoadOk = true;
					break;
				case 1:
					send.isLoadOk = true;
					break;
				case 2:
					send.isLoadOk = true;
					break;
				default:
					break;
				}
				send.Seq = message.Seq;
				refalshAdapter(adapter);
			}
		}
	};

	private OnTouchListener onAudioTouch = new OnTouchListener() {
		@Override
		public boolean onTouch(View view, MotionEvent event) {
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				recording_container.setVisibility(View.VISIBLE);
				// if (!Environment.isExternalStorageEmulated()) {
				// GciDialogManager.getInstance().showTextToast("没有储存卡",
				// ChatActivity.this);
				// break;
				// }
				// Environment.getExternalStorageDirectory()
				if (mp3 == null) {
					String filename = System.currentTimeMillis() + ".mp3";
					mp3 = new MP3Recorder(getApplicationContext().getFilesDir().getAbsolutePath() + "/" + filename, 8000);
					mp3.setFilename(filename);
					mp3.start();
				}
				break;
			case MotionEvent.ACTION_UP:
				recording_container.setVisibility(View.GONE);
				if (mp3 != null)
					mp3.stop();
				SendMessageModel data = new SendMessageModel();
				data.Audio_Path = mp3.getFilePath();
				data.Audio_Timer = mp3.getTimeLength();
				data.isLoadOk = false;
				data.MessageId = CommTool.getGUID();
				data.SendId = GroupVarManager.getIntance().loginUserInfo.Id;
				data.ReceiveId = userid;
				data.Type = 1;
				data.FileName = mp3.getFilename();

				addItemAndUpdateUI(data);

				sendMP3Files(data);
				mp3 = null;
				break;
			case MotionEvent.ACTION_OUTSIDE:
				recording_container.setVisibility(View.GONE);
				if (mp3 != null)
					mp3.stop();
				break;
			}
			return true;
		}
	};

	private void sendImagePic(SendMessageModel data, String filePath) {
		File file = new File(filePath);
		if (file.exists()) {
			UploadUtil up = new UploadUtil(file, "filename", GroupVarManager.getIntance().getUploadUrl());
			MyUploadFileListener lis = new MyUploadFileListener();
			data.PicFilePath = filePath;
			lis.setData(data);
			up.setUploadFileListener(lis);
			up.start();
		}
	}

	private void sendMP3Files(SendMessageModel data) {
		File file = new File(data.Audio_Path);
		if (file.exists()) {
			UploadUtil up = new UploadUtil(file, "filename", GroupVarManager.getIntance().getUploadUrl());
			MyUploadFileListener lis = new MyUploadFileListener();
			lis.setData(data);
			up.setUploadFileListener(lis);
			up.start();
		}
	}

	private class MyUploadFileListener implements UploadFileListener {

		private SendMessageModel data;

		public SendMessageModel getData() {
			return data;
		}

		public void setData(SendMessageModel data) {
			this.data = data;
		}

		@Override
		public void OnUploading(int postion, File file) {
		}

		@Override
		public void OnStart() {
		}

		@Override
		public void OnFisish(File file, String requestStr) {
			sendMessage(data);
		}

		@Override
		public void OnError(int errcode, String errormessage) {
		}
	};

	private void exit() {

	}

	/**
	 * 刷新消息列表
	 * 
	 * @param adpter
	 */
	private void refalshAdapter(final BaseGciAdapter<?, ?> adpter) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				adpter.refash();
			}
		});
	}

	public void addItemAndUpdateUI(SendMessageModel data) {
		GroupVarManager.getIntance().chats.addLast(data);
		adapter.addDatatItem(data);
		runOnUiThread(new Runnable() {
			public void run() {
				adapter.refash();
				list.setSelection(list.getBottom());
				et_sendmessage.setText("");
			}
		});
	}

	private void sendMessage(SendMessageModel data) {
		AppSendModel send = NetServer.getIntance().getSendModel(CommunMsgType.MSG_COMM, data);
		NetServer.getIntance().getSocket().AddMessagePairListener(send, messageResponse);
	}

	private void initData() {
		userid = this.getIntent().getStringExtra("UserId");
		List<SendMessageModel> lst = new ArrayList<SendMessageModel>();
		for (int i = 0; i < GroupVarManager.getIntance().chats.size(); i++) {
			if (userid.equals(GroupVarManager.getIntance().chats.get(i).SendId) || userid.equals(GroupVarManager.getIntance().chats.get(i).ReceiveId)) {
				lst.add(GroupVarManager.getIntance().chats.get(i));
			}
		}
		adapter.addDataList(lst);
		adapter.refash();
		list.setSelection(list.getBottom());
	}

	@Override
	protected void onDestroy() {
		BluetoothDriverManager.getIntance().removeCallBack();
		super.onDestroy();
	}
}
