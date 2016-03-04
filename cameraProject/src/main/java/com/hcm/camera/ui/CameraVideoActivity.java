package com.hcm.camera.ui;

import java.io.IOException;
import java.net.InetAddress;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.media.MediaRecorder.AudioSource;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.StrictMode;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.Window;
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
import com.hcm.camera.net.model.video.VideoMsgType;
import com.hcm.camera.video.H264Stream;
import com.hcm.camera.video.VideoManager;
import com.hcm.camera.video.audio.AudioPcm;
import com.hcm.camera.video.gl.SurfaceView;

public class CameraVideoActivity extends BaseActivity {

	private SurfaceView suiface;
	private H264Stream stream;
	private ImageView iv_back;
	private AudioPcm pcm;

	private PowerManager.WakeLock wakeLock = null;
	private String TaskId = "";

	@Override
	protected void onCreate(Bundle changestate) {
		super.onCreate(changestate);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		// ������������ͼ
		setContentView(R.layout.cameravideo);

		try {
			TaskId = this.getIntent().getStringExtra("TaskId");
		} catch (Exception e) {
		}
		if ("".equals(TaskId)) {
			finish();
			return;
		}

		initControl();

		initListenter();

		initCamera();

		// GciDialogManager2.getInstance().showLoading("���ڳ�ʼ��", this, null);

		new Handler().postDelayed(run, 1500);
	}

	@Override
	protected void onResume() {
		super.onResume();
		wakeLock = CommTool.acquireWakeLock(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (wakeLock != null) {
			CommTool.releaseWakeLock(wakeLock);
			wakeLock = null;
		}
	}

	private Runnable run = new Runnable() {
		@Override
		public void run() {
			VideoManager.getIntance().start(suiface);
		}
	};

	private void initControl() {
		suiface = GetControl(R.id.suifaceview);
		iv_back = GetControl(R.id.iv_back);
		// view = GetControl(R.id.recc);

		// medaview = GetControl(R.id.codecsuifaceview);
		// decoderview = GetControl(R.id.decodecsuifaceview);
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
				end.TaskId = TaskId;
				AppSendModel send = NetServer.getIntance().getSendModel(VideoMsgType.MSG_END_VIDEO, end);
				NetServer.getIntance().getSocket().sendMessage(send, (byte) 1);
				finish();
			}

			@Override
			public void onClickCanl() {
			}
		}, CameraVideoActivity.this);
	}

	private void initCamera() {

	}

	@Override
	public void finish() {
		VideoManager.getIntance().stop();
		super.finish();
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
