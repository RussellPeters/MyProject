package com.hcm.camera.ui.adapter;

import com.gci.nutil.activity.GciActivityManager;
import com.gci.nutil.base.BaseGciAdapter;
import com.hcm.camera.R;
import com.hcm.camera.net.model.user.UserInfo;
import com.hcm.camera.ui.ChatActivity;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class ContactAdapter extends BaseGciAdapter<UserInfo, String> {
	private LayoutInflater inflater = null;

	public ContactAdapter(ListView arg0, Context con) {
		super(arg0, con);
		inflater = LayoutInflater.from(con);
	}

	public class ViewHolder {
		/** 图片 */
		public ImageView iv_avatar;
		/** 消息数 */
		public TextView tv_unread;
		/** 昵称 */
		public TextView tv_name;
		/** 最后一条消息 */
		public TextView tv_content;
		/** 消息日期 */
		public TextView tv_time;
	}

	@Override
	public View getListView(int postion, View convertView, ViewGroup arg2, Context arg3, UserInfo binObj) {
		ViewHolder vh;
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.item_conversation_single, null);
			vh = new ViewHolder();
			vh.iv_avatar = (ImageView) convertView.findViewById(R.id.iv_avatar);
			vh.tv_unread = (TextView) convertView.findViewById(R.id.tv_unread);
			vh.tv_name = (TextView) convertView.findViewById(R.id.tv_name);
			vh.tv_content = (TextView) convertView.findViewById(R.id.tv_content);
			vh.tv_time = (TextView) convertView.findViewById(R.id.tv_time);
			convertView.setTag(vh);
		} else {
			vh = (ViewHolder) convertView.getTag();
		}
		vh.tv_name.setText(binObj.UserName);
		vh.tv_unread.setText("3");
		if (binObj.IsOnline)
			vh.tv_time.setText("在线");
		else
			vh.tv_time.setText("离线");
		return convertView;
	}

	@Override
	protected void selectPostion(UserInfo obj, int i) {
		Log.e("点击", "点击了" + "_" + i);
		Log.e("点击", "点击了" + "_" + GciActivityManager.getInstance().getLastActivity().getClass().getName());
		Intent intent = new Intent(GciActivityManager.getInstance().getLastActivity(), ChatActivity.class);
		intent.putExtra("UserId", obj.Id);
		GciActivityManager.getInstance().getLastActivity().startActivity(intent);
	}

	@Override
	public boolean setListItemWhere(UserInfo info, String id) {
		return id.equals(info.Id);
	}
}
