package com.hcm.camera.ui.adapter;

import java.io.File;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gci.libmad.NativeMP3Decoder;
import com.gci.nutil.base.BaseActivity;
import com.gci.nutil.base.BaseGciAdapter;
import com.hcm.camera.R;
import com.hcm.camera.data.GroupVarManager;
import com.hcm.camera.messagetask.ImageMessageTask;
import com.hcm.camera.net.model.Commun.SendMessageModel;
import com.hcm.camera.netfile.DownLoadFile;
import com.hcm.camera.netfile.DownLoadFile.DownLoadFileLisenter;
import com.hcm.camera.ui.ShowBigImageAcitvity;

public class ChatsAdapter extends BaseGciAdapter<SendMessageModel, String> {

	private LayoutInflater inflater = null;

	private MediaPlayer mp;

	private BaseActivity context;

	public ChatsAdapter(ListView arg0, BaseActivity con) {
		super(arg0, con);
		inflater = LayoutInflater.from(con);
		context = con;
	}

	public class ViewMessageHolder {
		public int type;

		public boolean is_send = true;
		/** 消息内容 */
		public TextView tv_chatcontent;
		/** 消息失败->图片 */
		public ImageView msg_status;

		public ProgressBar pb_sending;

		public ImageView iv_voice;

		public TextView tv_length;

		public ImageView iv_unread_voice;

		public ImageView iv_sendPicture;
	}

	@Override
	public View getListView(int postion, View contentview, ViewGroup arg2, Context arg3, final SendMessageModel binobj) {
		boolean isAlone = false;
		ViewMessageHolder vh = null;
		if (contentview != null) {
			vh = (ViewMessageHolder) contentview.getTag();
			if (vh.type == binobj.Type && (vh.is_send == binobj.SendId.equals(GroupVarManager.getIntance().userId))) {
				isAlone = true;
			}
		}

		if (!isAlone) {
			vh = new ViewMessageHolder();
			switch (binobj.Type) {
			case SendMessageModel.MESSAGE_TYPE_MESSAGE:
				if (binobj.SendId.equals(GroupVarManager.getIntance().userId)) {
					contentview = inflater.inflate(R.layout.row_sent_message, null);
					vh.is_send = true;
					vh.msg_status = (ImageView) contentview.findViewById(R.id.msg_status);
					vh.pb_sending = (ProgressBar) contentview.findViewById(R.id.pb_sending);
					vh.tv_chatcontent = (TextView) contentview.findViewById(R.id.tv_chatcontent);
					vh.type = binobj.Type;
				} else {
					contentview = inflater.inflate(R.layout.row_received_message, null);
					vh.is_send = false;
					vh.msg_status = (ImageView) contentview.findViewById(R.id.msg_status);
					vh.tv_chatcontent = (TextView) contentview.findViewById(R.id.tv_chatcontent);
					vh.type = binobj.Type;
				}
				break;
			case SendMessageModel.MESSAGE_TYPE_AUDIO:
				if (binobj.SendId.equals(GroupVarManager.getIntance().userId)) {
					contentview = inflater.inflate(R.layout.row_sent_voice, null);
					vh.is_send = true;
					vh.msg_status = (ImageView) contentview.findViewById(R.id.msg_status);
					vh.pb_sending = (ProgressBar) contentview.findViewById(R.id.pb_sending);
					vh.iv_voice = (ImageView) contentview.findViewById(R.id.iv_voice);
					vh.tv_length = (TextView) contentview.findViewById(R.id.tv_length);
					vh.type = binobj.Type;
				} else {
					contentview = inflater.inflate(R.layout.row_received_voice, null);
					vh.is_send = false;
					vh.msg_status = (ImageView) contentview.findViewById(R.id.msg_status);
					vh.tv_chatcontent = (TextView) contentview.findViewById(R.id.tv_chatcontent);
					vh.tv_length = (TextView) contentview.findViewById(R.id.tv_length);
					vh.iv_voice = (ImageView) contentview.findViewById(R.id.iv_voice);
					vh.iv_unread_voice = (ImageView) contentview.findViewById(R.id.iv_unread_voice);
					vh.pb_sending = (ProgressBar) contentview.findViewById(R.id.pb_sending);
					vh.type = binobj.Type;
				}
				break;
			case SendMessageModel.MESSAGE_TYPE_IMAGE:
				if (binobj.SendId.equals(GroupVarManager.getIntance().userId)) {
					contentview = inflater.inflate(R.layout.row_sent_picture, null);
					vh.is_send = true;
					vh.pb_sending = (ProgressBar) contentview.findViewById(R.id.pb_sending);
					vh.type = binobj.Type;
					vh.iv_sendPicture = (ImageView) contentview.findViewById(R.id.iv_sendPicture);
				} else {
					contentview = inflater.inflate(R.layout.row_received_picture, null);
					vh.is_send = false;
					vh.pb_sending = (ProgressBar) contentview.findViewById(R.id.pb_sending);
					vh.type = binobj.Type;
					vh.iv_sendPicture = (ImageView) contentview.findViewById(R.id.iv_sendPicture);
					vh.type = binobj.Type;
				}
				break;
			default:
				break;
			}
			contentview.setTag(vh);
		}

		final ViewMessageHolder temp = vh;
		switch (binobj.Type) {
		case SendMessageModel.MESSAGE_TYPE_MESSAGE:
			vh.tv_chatcontent.setText(binobj.Message);
			break;
		case SendMessageModel.MESSAGE_TYPE_AUDIO:
			vh.tv_length.setText(binobj.Audio_Timer + "");
			vh.iv_voice.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View view) {
					if (binobj.SendId.equals(GroupVarManager.getIntance().userId)) {
						playAudio(view, binobj);
					} else {
						if (binobj.Audio_Path == null || "".equals(binobj.Audio_Path)) {
							temp.pb_sending.setVisibility(View.VISIBLE);
							DownLoadFile down = new DownLoadFile(GroupVarManager.getIntance().getDownLoadUrl(binobj.FileName), GroupVarManager.getIntance().getFilePath(context, binobj.FileName), binobj.FileName, context, new DownLoadFileLisenter() {

								@Override
								public void OnStart() {
								}

								@Override
								public void OnFisish(final String filePath, final String fileName, File file) {
									binobj.Audio_Path = filePath;
									binobj.FileName = fileName;
									binobj.isLoadOk = true;
									context.runOnUiThread(new Runnable() {
										@Override
										public void run() {
											temp.pb_sending.setVisibility(View.GONE);
											temp.iv_unread_voice.setVisibility(View.GONE);
											SendMessageModel m = ChatsAdapter.this.selectOrDefault(binobj.MessageId);
										}
									});
									playAudio(view, binobj);
								}

								@Override
								public void OnDownLoading(int postion) {
								}

								@Override
								public void OnDownLoadError(int errcode, String errmessage) {
								}
							});
							down.start();
						} else {
							playAudio(view, binobj);
						}
					}

				}
			});
			break;
		case SendMessageModel.MESSAGE_TYPE_IMAGE:
			if (binobj.PicBitMap != null) {
				vh.iv_sendPicture.setImageBitmap(binobj.PicBitMap);
			} else if (!binobj.isDownLoading) {
				ImageMessageTask task = new ImageMessageTask(context, vh, binobj);
				task.doTask();
			}
			vh.iv_sendPicture.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View arg0) {
					if (binobj.PicFilePath != null && binobj.PicBitMap != null) {
						Intent intent = new Intent(context, ShowBigImageAcitvity.class);
						intent.putExtra("FilePath", binobj.PicFilePath);
						context.startActivity(intent);
					}
				}
			});
			break;
		default:
			break;
		}

		if (vh.pb_sending != null) {
			if (!binobj.isLoadOk)
				vh.pb_sending.setVisibility(View.VISIBLE);
			else
				vh.pb_sending.setVisibility(View.GONE);
		}

		return contentview;
	}

	private void playAudio(View view, SendMessageModel binobj) {
		File file = new File(binobj.Audio_Path);
		if (file.exists()) {
			try {
				if (view.getTag() != null) {
					MediaPlayer m = (MediaPlayer) view.getTag();
					if (!m.isPlaying()) {
						m.stop();
						m.start();
						return;
					}
				}
				mp = new MediaPlayer();
				mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
				mp.setDataSource(binobj.Audio_Path);
				mp.prepare();
				mp.start();
				view.setTag(mp);
			} catch (Exception e) {
				// TODO: handle exception
			}
		} else {
			Log.e("Tag", binobj.Audio_Path + "不存在");
		}
	}

	@Override
	protected void selectPostion(SendMessageModel arg0, int arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean setListItemWhere(SendMessageModel obj, String seq) {
		return (!obj.isLoadOk && obj.MessageId.equals(seq));
	}

}
