package com.hcm.camera.ui;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gci.nutil.activity.GciActivityManager;
import com.gci.nutil.base.BaseActivity;
import com.gci.nutil.control.pulluprefash.ListHeaderView;
import com.gci.nutil.control.pulluprefash.OnPullUpUpdateTask;
import com.gci.nutil.control.pulluprefash.OnUpdateTask;
import com.gci.nutil.control.pulluprefash.PullToRefreshListView;
import com.gci.nutil.control.pulluprefash.RefreshableListView;
import com.gci.nutil.dialog.GciDialogManager;
import com.gci.nutil.dialog.GciDialogManager2;
import com.hcm.camera.R;
import com.hcm.camera.comm.CommTool;
import com.hcm.camera.comm.GroblListenter;
import com.hcm.camera.data.GroupVarManager;
import com.hcm.camera.data.net.NetServer;
import com.hcm.camera.data.net.OnResponseListener;
import com.hcm.camera.net.model.AppSendModel;
import com.hcm.camera.net.model.user.ResponseRefash;
import com.hcm.camera.net.model.user.SendRefash;
import com.hcm.camera.net.model.user.UserInfo;
import com.hcm.camera.net.model.user.UserMsgType;
import com.hcm.camera.ui.adapter.ContactAdapter;

public class MainActivity extends BaseActivity {

	private RelativeLayout fragment_container; // ?????????
	private ImageView iv_back; // ?????????
	private PullToRefreshListView lv_list;

	private View homeView;

	private ContactAdapter contacta;

	private LayoutInflater layoutInflater;

	private PullToRefreshListView listview;

	private LinearLayout main_bottom;

	private ContactAdapter mAdapterContact;

	private GroblListenter grobl = new GroblListenter();

	private TextView tv_online;

	public static SurfaceView sur;

	@Override
	protected void onCreate(Bundle changestate) {
		super.onCreate(changestate);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		// ?????????????
		setContentView(R.layout.activity_mian_temp);

		layoutInflater = LayoutInflater.from(this);

		initControl();

		initListenter();

		initData();

		// if (GroupVarManager.getIntance().loginUserInfo.UserType == 0) {
		// BluetoothDriverManager.getIntance().start(VideoSharePreference.getInstance(MainActivity.this).getBT()
		// );
		// }
	}

	private void initControl() {
		fragment_container = GetControl(R.id.fragment_container);
		iv_back = GetControl(R.id.iv_back);
		homeView = layoutInflater.inflate(R.layout.fragment_home, null);
		listview = (PullToRefreshListView) homeView.findViewById(R.id.list);
		mAdapterContact = new ContactAdapter(listview, this);
		main_bottom = GetControl(R.id.main_bottom);
		tv_online = GetControl(R.id.tv_online);

		if (GroupVarManager.getIntance().loginUserInfo.UserType == 0)
			main_bottom.setVisibility(View.GONE);

		if (GroupVarManager.getIntance().loginUserInfo.UserType == 0)
			tv_online.setText("下位机:" + GroupVarManager.getIntance().loginUserInfo.Id);
		else
			tv_online.setText("上位机" + GroupVarManager.getIntance().loginUserInfo.Id);

		sur = GetControl(R.id.tempSur);
	}

	private void initListenter() {
		listview.setOnPullUpUpdateTask(new OnPullUpUpdateTask() {
			@Override
			public void updateUI(RefreshableListView arg0, ListHeaderView arg1) {
			}

			@Override
			public void updateBackground(RefreshableListView arg0, ListHeaderView arg1) {
			}

			@Override
			public void onUpdateStart(RefreshableListView list, final ListHeaderView head) {

			}
		});

		listview.setOnPullDownUpdateTask(new OnUpdateTask() {
			@Override
			public void updateUI(RefreshableListView listview, ListHeaderView view) {
			}

			@Override
			public void updateBackground(RefreshableListView listview, ListHeaderView view) {
			}

			@Override
			public void onUpdateStart(final RefreshableListView listview, final ListHeaderView view) {
				reflashDataItem(new Runnable() {
					public void run() {
						listview.closePullToNormal(listview, view);
					}
				});
			}
		});
	}

	private void exit() {

	}

	private void initData() {
		RelativeLayout.LayoutParams relLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		fragment_container.addView(homeView, relLayoutParams);

		GciDialogManager2.getInstance().showLoading("正在载入数据", this, fragment_container);

		reflashDataItem(null);
	}

	private void reflashDataItem(final Runnable responseRunnable) {
		SendRefash refash = new SendRefash();
		refash.Id = GroupVarManager.getIntance().userId;
		refash.type = GroupVarManager.getIntance().loginUserInfo.UserType;

		AppSendModel send = NetServer.getIntance().getSendModel(UserMsgType.MSG_REFLASH_NEXT_PHONE, refash);

		NetServer.getIntance().getSocket().AddMessagePairListener(send, new OnResponseListener() {
			@Override
			public void res(String json, Object sender) {
				final ResponseRefash rdata = CommTool.gson.fromJson(json, ResponseRefash.class);
				try {
					Thread.sleep(300);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (rdata.usrs != null) {
							addData(rdata.usrs);
						}
						MainActivity.this.canelLoading();
						if (responseRunnable != null)
							responseRunnable.run();
					}
				});
			}
		});
	}

	private void addData(List<UserInfo> lst) {
		mAdapterContact.clear();
		mAdapterContact.addDataList(lst);
		mAdapterContact.refash();
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
