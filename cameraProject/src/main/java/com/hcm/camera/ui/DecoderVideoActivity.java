package com.hcm.camera.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import com.gci.nutil.base.BaseActivity;
import com.gci.nutil.base.callbackinterface.OnComfireListener;
import com.gci.nutil.dialog.GciDialogManager2;
import com.hcm.camera.R;
import com.hcm.camera.comm.CommTool;
import com.hcm.camera.data.GroupVarManager;
import com.hcm.camera.data.net.NetServer;
import com.hcm.camera.net.model.AppSendModel;
import com.hcm.camera.net.model.video.SendEndVideo;
import com.hcm.camera.net.model.video.SendVideoConfig;
import com.hcm.camera.net.model.video.VideoMsgType;
import com.hcm.camera.video.decoder.AvcDecoder;

public class DecoderVideoActivity extends BaseActivity {
	private AvcDecoder decoderview;
	private PowerManager.WakeLock wakeLock = null;
	private ImageView iv_back;
	private SendVideoConfig config;

	@Override
	protected void onCreate(Bundle changestate) {
		super.onCreate(changestate);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		// ������������ͼ
		setContentView(R.layout.decodervideo);
		try {
			config = (SendVideoConfig) this.getIntent().getSerializableExtra("config");
		} catch (Exception e) {
		}
		if (config == null) {
			finish();
			return;
		}

		initControl();

		initListenter();

		initCamera();

		// GciDialogManager2.getInstance().showLoading("���ڳ�ʼ��", this, null);

		new Handler().postDelayed(run, 1000);
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		wakeLock = CommTool.acquireWakeLock(this);
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		if (wakeLock != null) {
			CommTool.releaseWakeLock(wakeLock);
			wakeLock = null;
		}
	}

	private Runnable run = new Runnable() {
		@Override
		public void run() {
			decoderview.setEncodetype(config.EcoderType);
			decoderview.start();
		}
	};

	private void initControl() {
		decoderview = GetControl(R.id.dec_suiface);
		iv_back = GetControl(R.id.iv_back);
	}

	private void initListenter() {
		iv_back.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				exit();
			}
		});
	}

	private void exit() {
		GciDialogManager2.getInstance().showComfire("提示", "确定退出吗?", null, false, new OnComfireListener() {
			@Override
			public void onClickOK() {
				SendEndVideo end = new SendEndVideo();
				end.Id = GroupVarManager.getIntance().loginUserInfo.Id;
				end.PhoneType = GroupVarManager.getIntance().loginUserInfo.UserType;
				end.TaskId = config.TaskId;
				AppSendModel send = NetServer.getIntance().getSendModel(VideoMsgType.MSG_END_VIDEO, end);
				NetServer.getIntance().getSocket().sendMessage(send, (byte) 1);
				finish();
			}

			@Override
			public void onClickCanl() {
			}
		}, DecoderVideoActivity.this);
	}

	private void initCamera() {
	}

	@Override
	public void finish() {
		try {
			if (decoderview != null && decoderview.isStart()) {
				decoderview.stop();
				// decoderview.destroyDrawingCache();
				decoderview = null;
				super.finish();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			exit();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
}
