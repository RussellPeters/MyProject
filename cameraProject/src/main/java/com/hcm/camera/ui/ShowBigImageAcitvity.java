package com.hcm.camera.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.gci.nutil.base.BaseActivity;
import com.hcm.camera.R;
import com.hcm.camera.ui.picview.PhotoView;

public class ShowBigImageAcitvity extends BaseActivity {
	private ImageView iv_back;
	private String filePath = "";
	private ProgressBar pb_load_local;
	private PhotoView image;

	@Override
	protected void onCreate(Bundle changestate) {
		super.onCreate(changestate);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		// ������������ͼ
		setContentView(R.layout.activity_show_big_image);

		initControl();

		initListenter();

		try {
			filePath = this.getIntent().getStringExtra("FilePath");
			if (filePath == null)
				filePath = changestate.getString("FilePath");
		} catch (Exception e) {
			// TODO: handle exception
		}

		if (filePath == null) {
			finish();
			return;
		}
		initImage();
	}

	private void initImage() {
		Bitmap bit = BitmapFactory.decodeFile(filePath);
		image.setImageBitmap(bit);
		pb_load_local.setVisibility(View.GONE);
	}

	private void initControl() {
		iv_back = GetControl(R.id.iv_back);
		image = GetControl(R.id.image);
		pb_load_local = GetControl(R.id.pb_load_local);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putString("FilePath", filePath);
		super.onSaveInstanceState(outState);
	}

	private void initListenter() {
		iv_back.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				finish();
			}
		});
	}
}
