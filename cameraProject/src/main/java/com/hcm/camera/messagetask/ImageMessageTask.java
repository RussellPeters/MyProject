package com.hcm.camera.messagetask;

import java.io.File;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;

import com.gci.nutil.base.BaseActivity;
import com.hcm.camera.comm.ImageUntil;
import com.hcm.camera.data.GroupVarManager;
import com.hcm.camera.net.model.Commun.SendMessageModel;
import com.hcm.camera.netfile.DownLoadFile;
import com.hcm.camera.netfile.DownLoadFile.DownLoadFileLisenter;
import com.hcm.camera.ui.adapter.ChatsAdapter.ViewMessageHolder;

public class ImageMessageTask {
	private ViewMessageHolder hoder;
	private SendMessageModel model;
	private DownLoadFile down;
	private BaseActivity base;

	public ImageMessageTask(BaseActivity b, ViewMessageHolder h, SendMessageModel obj) {
		this.hoder = h;
		this.model = obj;
		this.base = b;
	}

	public void doTask() {
		down = new DownLoadFile(GroupVarManager.getIntance().getDownLoadUrl(model.PicFileName), GroupVarManager.getIntance().getFilePath(base, model.PicFileName), model.PicFileName, base, new DownLoadFileLisenter() {
			@Override
			public void OnStart() {
			}

			@Override
			public void OnFisish(final String filePath, String fileName, File file) {
				final Bitmap bit = ImageUntil.getIntance(base).creatBitmap(filePath);
				base.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (hoder != null) {
							hoder.pb_sending.setVisibility(View.GONE);
							hoder.iv_sendPicture.setImageBitmap(bit);
							model.PicBitMap = bit;
							model.PicFilePath = filePath;
							model.isLoadOk = true;
						}
					}
				});
			}

			@Override
			public void OnDownLoading(int postion) {
			}

			@Override
			public void OnDownLoadError(int errcode, String errmessage) {
			}
		});

		down.start();
	}
}
