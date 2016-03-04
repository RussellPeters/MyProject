package com.hcm.camera.ui;

import java.io.IOException;
import java.util.UUID;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.gci.nutil.OnViewClickListenter;
import com.gci.nutil.base.BaseActivity;
import com.gci.nutil.base.callbackinterface.OnComfireListener;
import com.gci.nutil.comm.SharePreference;
import com.gci.nutil.dialog.GciDialogManager2;
import com.hcm.camera.R;
import com.hcm.camera.bluetooth.BluetoothDriverManager;
import com.hcm.camera.bluetooth.DeviceListActivity;
import com.hcm.camera.comm.CommTool;
import com.hcm.camera.data.GroupVarManager;
import com.hcm.camera.data.net.NetServer;
import com.hcm.camera.data.net.OnResponseListener;
import com.hcm.camera.net.model.AppSendModel;
import com.hcm.camera.net.model.Commun.VideoSharePreference;
import com.hcm.camera.net.model.user.ResponseLogin;
import com.hcm.camera.net.model.user.SendLogin;
import com.hcm.camera.net.model.user.UserMsgType;
import com.hcm.camera.net.model.video.UserData;

public class LoginActivity extends BaseActivity {
	private ImageView iv_back; // 锟矫伙拷锟斤拷锟斤拷锟斤拷
	private EditText et_usertel;
	private EditText et_password;
	private Button btn_login;

	private EditText ed_ipaddress;
	private EditText ed_webaddress;
	private Button btn_logins;
	private CheckBox Apple;
	private ImageView iv_cmd;
	private Button btn_Bts;

	private static final int REQUEST_CONNECT_DEVICE = 1;
	private UserData userdata = new UserData();

	@Override
	protected void onCreate(Bundle changestate) {
		super.onCreate(changestate);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		// 锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷图
		setContentView(R.layout.activity_login);

		initControl();

		initListenter();
		//
		NetServer.getIntance().Start(this);

		// 开启蓝牙通讯管理
		BluetoothDriverManager.getIntance().start(VideoSharePreference.getInstance(LoginActivity.this).getBT());

	}

	private void initControl() {
		iv_back = GetControl(R.id.iv_back);
		et_usertel = GetControl(R.id.et_usertel);
		et_password = GetControl(R.id.et_password);
		btn_login = GetControl(R.id.btn_login);
		btn_logins = GetControl(R.id.btn_logins);
		ed_ipaddress = GetControl(R.id.ed_ipaddress);
		ed_webaddress = GetControl(R.id.ed_webaddress);
		Apple = GetControl(R.id.Apple);
		btn_Bts = GetControl(R.id.btn_Bts);
		iv_cmd = GetControl(R.id.iv_cmd);

		String ip = "112.124.116.86";
		String weburl = "http://112.124.116.86:8008/";
		if ("".equals(VideoSharePreference.getInstance(this).getIpAddress())) {
			VideoSharePreference.getInstance(this).setIpAddress(ip);
		}

		if ("".equals(VideoSharePreference.getInstance(this).getWebAddress()))
			VideoSharePreference.getInstance(this).setWebAddress(weburl);

		ip = VideoSharePreference.getInstance(this).getIpAddress();
		weburl = VideoSharePreference.getInstance(this).getWebAddress();

		ed_ipaddress.setText(ip);
		ed_webaddress.setText(weburl);

		String datajson = VideoSharePreference.getInstance(this).getUserData();
		if (!"".equals(datajson)) {
			userdata = CommTool.gson.fromJson(datajson, UserData.class);
		}

		et_usertel.setText(userdata.UserId);
		et_password.setText(userdata.Password);
		Apple.setChecked(userdata.IsUp);
	}

	private void initListenter() {
		btn_login.setOnClickListener(btnLoginClick);
		iv_back.setOnClickListener(btnExit);
		btn_logins.setOnClickListener(btnTo);

		btn_Bts.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Intent serverIntent = new Intent(LoginActivity.this, DeviceListActivity.class); // 跳转程序设置
				startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE); // 设置返回宏定义
				int i = R.id.iv_avatar;
			}
		});

		iv_cmd.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(LoginActivity.this, CmdActivity.class);
				startActivity(intent);
			}
		});
	}

	private OnViewClickListenter btnLoginClick = new OnViewClickListenter(1000) {
		@Override
		public void onClick(boolean isPass, View view) {

			// Intent intent = new Intent(LoginActivity.this,
			// CameraVideoActivity.class);
			// startActivity(intent);
			// return;
			final SendLogin data = new SendLogin();
			data.Id = et_usertel.getText().toString();
			data.PassWrod = et_password.getText().toString();
			data.type = Apple.isChecked() ? 1 : 0;

			GroupVarManager.getIntance().userId = data.Id;

			AppSendModel send = NetServer.getIntance().getSendModel(UserMsgType.MSG_LOGIN, data);
			NetServer.getIntance().getSocket().AddMessagePairListener(send, new OnResponseListener() {
				@Override
				public void res(String json, Object sender) {
					ResponseLogin info = CommTool.getInstanceByJson(json, ResponseLogin.class);
					GroupVarManager.getIntance().loginUserInfo = info.LoginUser;
					runOnUiThread(new Runnable() {
						public void run() {
							userdata.UserId = data.Id;
							userdata.Password = data.PassWrod;
							userdata.IsUp = Apple.isChecked();
							VideoSharePreference.getInstance(LoginActivity.this).setUserData(CommTool.gson.toJson(userdata));

							Intent intent = new Intent(LoginActivity.this, MainActivity.class);
							startActivity(intent);
							// finish();
						}
					});
				}
			});
		}
	};

	private OnViewClickListenter btnTo = new OnViewClickListenter(1000) {
		@Override
		public void onClick(boolean isPass, View view) {
			if ("".equals(ed_ipaddress.getText().toString())) {
				GciDialogManager2.getInstance().showTextToast("请填写IP地址", LoginActivity.this);
				return;
			}
			VideoSharePreference.getInstance(LoginActivity.this).setIpAddress(ed_ipaddress.getText().toString());
			VideoSharePreference.getInstance(LoginActivity.this).setWebAddress(ed_webaddress.getText().toString());
			NetServer.getIntance().Restart(LoginActivity.this);
			GciDialogManager2.getInstance().showTextToast("修改完成", LoginActivity.this);
		}
	};

	private OnViewClickListenter btnExit = new OnViewClickListenter(1500) {
		@Override
		public void onClick(boolean isPass, View view) {
			GciDialogManager2.getInstance().showComfire("提示", "退出?", null, true, new OnComfireListener() {

				@Override
				public void onClickOK() {
				}

				@Override
				public void onClickCanl() {
				}
			}, LoginActivity.this);
		}
	};

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE: // 连接结果，由DeviceListActivity设置返回
			// 响应返回结果
			if (resultCode == Activity.RESULT_OK) { // 连接成功，由DeviceListActivity设置返回
				// MAC地址，由DeviceListActivity设置返回
				String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				VideoSharePreference.getInstance(LoginActivity.this).setBT(address);
				BluetoothDriverManager.getIntance().start(address);
			}
			break;
		default:
			break;
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			ExitSystem();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
}
