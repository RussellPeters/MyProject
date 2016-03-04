package com.hcm.camera.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.style.BulletSpan;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.gci.nutil.base.BaseActivity;
import com.hcm.camera.R;
import com.hcm.camera.bluetooth.BluetoothComment;
import com.hcm.camera.bluetooth.BluetoothMessageManager;
import com.hcm.camera.bluetooth.OnBluetoothMessageChange;
import com.hcm.camera.data.GroupVarManager;

public class CmdActivity extends BaseActivity {

	private TextView tv_message = null;
	private ScrollView sv_view = null;
	private ImageView iv_cmd = null;
	private ImageView iv_back = null;

	private boolean isSendServer = false;
	private String userid;

	@Override
	protected void onCreate(Bundle changestate) {
		super.onCreate(changestate);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.act_cmd);
		isSendServer = getIntent().getBooleanExtra("isSendServer", isSendServer);
		userid = getIntent().getStringExtra("userid");

		initControl();
		initLister();

		BluetoothMessageManager.getInstance().addCallback(new OnBluetoothMessageChange() {
			@Override
			public void onChange(StringBuilder stb, String message) {
				final String tmp = stb.toString() + message;
				CmdActivity.this.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						tv_message.setText(tmp);
						sv_view.fullScroll(ScrollView.FOCUS_DOWN);
					}
				});
			}
		});

	}

	private void initControl() {
		tv_message = GetControl(R.id.tv_message);
		sv_view = GetControl(R.id.sv_view);
		iv_cmd = GetControl(R.id.iv_cmd);
		iv_back = GetControl(R.id.iv_back);
	}

	private void initLister() {
		iv_back.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				finish();
			}
		});

		iv_cmd.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (isSendServer)
					BluetoothComment.getIntance().showCmd(CmdActivity.this, userid, GroupVarManager.getIntance().userId);
				else
					BluetoothComment.getIntance().showCmd(CmdActivity.this);
			}
		});
	}

}
