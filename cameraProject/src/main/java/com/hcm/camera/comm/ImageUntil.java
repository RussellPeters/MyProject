package com.hcm.camera.comm;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


import com.gci.nutil.AppUtil;
import com.gci.nutil.DensityUtil;
import com.gci.nutil.ViewUtil;
import com.gci.nutil.base.BaseActivity;
import com.gci.nutil.base.callbackinterface.OnActivityResultListener;
import com.hcm.camera.data.OnPickImageCallBack;


import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.BitmapFactory.Options;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.app.Activity;

public class ImageUntil {

	private static ImageUntil _m;
	private BaseActivity _base;

	private OnPickImageCallBack _onPickImageLis;

	private OnPickImageCallBack _onCarmreaLis;

	private File pictureFile;

	private int reWidth = 0;
	private int reHeight = 0;

	private ImageUntil() {
	}

	// public static ImageUntil getIntance() {
	// if (_m == null) {
	// _m = new ImageUntil();
	// }
	// return _m;
	// }

	public static ImageUntil getIntance(BaseActivity base) {
		if (_m == null) {
			_m = new ImageUntil();
		}
		_m.setBaseActivity(base);
		return _m;
	}

	public void setBaseActivity(BaseActivity b) {
		this._base = b;
	}

	private static final int PICK_IMG = 1001;
	private static final int CAMERA_IMAGE = 1002;

	/**
	 * 选择一张图片
	 * 
	 * @param con
	 * @param requestPicChoose
	 * @param resultlis
	 *            Activity收到返回数据时回调
	 */
	public void pickImage(OnPickImageCallBack lis) {
		_base.addResultMessageLinstener(pickImageLis);
		this._onPickImageLis = lis;
		Intent intent = new Intent();
		intent.setType("image/*");
		intent.setAction(Intent.ACTION_PICK);
		_base.startActivityForResult(intent, PICK_IMG);
	}

	public void camreaImage(OnPickImageCallBack lis) {
		_base.addResultMessageLinstener(camreaImage);
		this._onCarmreaLis = lis;
		String filepath = "";
		if (CommTool.hasSDCard()) {
			String SDCardRoot = Environment.getExternalStorageDirectory()
					.getAbsolutePath() + File.separator;
			File file = new File(SDCardRoot + "BigImage/");
			if (!file.exists())
				file.mkdirs();
			filepath = file.getAbsolutePath() + "/" + CommTool.getGUID()
					+ ".jpg";
		} else {
			if (lis != null)
				lis.error(0, "没有SD卡");
			return;
		}
		pictureFile = new File(filepath);
		Intent mIntent = new Intent();
		mIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
		mIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(pictureFile));
		mIntent.putExtra(MediaStore.Images.Media.ORIENTATION, 0);
		_base.startActivityForResult(mIntent, CAMERA_IMAGE);
	}

	private OnActivityResultListener pickImageLis = new OnActivityResultListener(
			PICK_IMG) {
		@Override
		public void onResult(int resultcode, Intent data) {

			if (resultcode != android.app.AliasActivity.RESULT_OK) {
				return;
			}
			String filepath = null;
			if ("file".equals(data.getData().getScheme())) {
				// 有些低版本机型返回的Uri模式为file
				filepath = data.getData().getPath();
			} else {
				// Uri模型为content
				String[] proj = { MediaStore.Images.Media.DATA };
				Cursor cursor = _base.getContentResolver().query(
						data.getData(), proj, null, null, null);
				cursor.moveToFirst();
				int idx = cursor
						.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
				filepath = cursor.getString(idx);
				cursor.close();
			}
			if (!(filepath == null) && _onPickImageLis != null) {
				Bitmap bitmap = creatBitmap(filepath);
				_onPickImageLis.callback(filepath, bitmap);
			}
		}
	};

	private OnActivityResultListener camreaImage = new OnActivityResultListener(
			CAMERA_IMAGE) {
		@Override
		public void onResult(int resultcode, Intent intent) {
			if (resultcode != android.app.AliasActivity.RESULT_OK) {
				return;
			}
			if (!(pictureFile == null) && _onCarmreaLis != null) {
				updateGallery(pictureFile.getAbsolutePath());
				yasuoBitMap(pictureFile.getAbsolutePath());
				Bitmap bitmap = creatBitmap(pictureFile.getAbsolutePath());
				_onCarmreaLis.callback(pictureFile.getAbsolutePath(), bitmap);
			}
		}
	};

	private void updateGallery(String filename) {
		MediaScannerConnection.scanFile(_base, new String[] { filename }, null,
				new MediaScannerConnection.OnScanCompletedListener() {

					@Override
					public void onScanCompleted(String path, Uri uri) {

					}
				});
	}

	// public Bitmap createSmallBitMap(String fileSrc){
	// // 获取图片的宽和高
	// Options options = new Options();
	// options.inJustDecodeBounds = true;
	// Bitmap img = BitmapFactory.decodeFile(fileSrc, options);
	// // 压缩图片
	// options.inSampleSize = Math.max(1, (int) Math.ceil(Math.max(
	// (double) options.outWidth / 1024f,
	// (double) options.outHeight / 1024f)));
	// options.inJustDecodeBounds = false;
	// img = BitmapFactory.decodeFile(fileSrc, options);
	//
	// // 部分手机会对图片做旋转，这里检测旋转角度
	// int degree = readPictureDegree(fileSrc);
	// if (degree != 0) {
	// // 把图片旋转为正的方向
	// img = rotateImage(degree, img);
	// }
	// return img;
	// }

	public int getSmallInSampleSize(Options options, int w, int h) {
		int result = 1;
		final int height = options.outHeight;
		final int width = options.outWidth;

		if (height > h || width > h) {
			final int heightRatio = Math.round((float) height / (float) h);
			final int widthRatio = Math.round((float) width / (float) w);
			result = heightRatio < widthRatio ? heightRatio : widthRatio;
		}
		return result;
	}

	public void yasuoBitMap(String fileSrc) {
		Options options = new Options();
		options.inJustDecodeBounds = true;
		Bitmap img = BitmapFactory.decodeFile(fileSrc, options);
		if (options.outWidth > 1024 || options.outHeight > 1024) {
			if (options.outWidth > options.outHeight) {
				options.inSampleSize = Math.max(1, (int) Math.ceil(Math.max(
						(double) options.outWidth / 1024f,
						(double) options.outHeight / 1024f)));
			} else {
				options.inSampleSize = Math.max(1, (int) Math.ceil(Math.max(
						(double) options.outWidth / 1024f,
						(double) options.outHeight / 1024f)));
			}
		}
		
		options.inJustDecodeBounds = false;
		img = BitmapFactory.decodeFile(fileSrc, options);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		img.compress(Bitmap.CompressFormat.JPEG, 70, baos);
		byte[] bytes = baos.toByteArray();
		try {
			FileOutputStream fis = new FileOutputStream(fileSrc,false);
			fis.write(bytes);
			fis.flush();
			fis.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public Bitmap creatBitmap(String fileSrc) {
		// 获取图片的宽和高
		Options options = new Options();
		options.inJustDecodeBounds = true;
		Bitmap img = BitmapFactory.decodeFile(fileSrc, options);

		if (options.outWidth > options.outHeight) {
			options.inSampleSize = Math.max(1, (int) Math.ceil(Math.max(
					(double) options.outWidth
							/ (float) DensityUtil.dp2px(_base, 220),
					(double) options.outHeight
							/ (float) DensityUtil.dp2px(_base, 110))));
		} else {
			options.inSampleSize = Math.max(1, (int) Math.ceil(Math.max(
					(double) options.outWidth
							/ (float) DensityUtil.dp2px(_base, 100),
					(double) options.outHeight
							/ (float) DensityUtil.dp2px(_base, 200))));
		}

		options.inJustDecodeBounds = false;
		img = BitmapFactory.decodeFile(fileSrc, options);

		// 部分手机会对图片做旋转，这里检测旋转角度
		int degree = readPictureDegree(fileSrc);
		if (degree != 0) {
			// 把图片旋转为正的方向
			img = rotateImage(degree, img);
		}
		return img;
	}

	/**
	 * 将Bitmap原图压缩成指定格式
	 * 
	 * @param img
	 * @param format
	 * @param quality
	 * @return
	 */
	public byte[] getFormatImage(Bitmap img, CompressFormat format, int quality) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		// 可根据流量及网络状况对图片进行压缩
		img.compress(Bitmap.CompressFormat.JPEG, 100, baos);
		return baos.toByteArray();
	}

	/**
	 * 旋转图片
	 * 
	 * @param angle
	 * @param bitmap
	 * @return Bitmap
	 */
	public static Bitmap rotateImage(int angle, Bitmap bitmap) {
		// 图片旋转矩阵
		Matrix matrix = new Matrix();
		matrix.postRotate(angle);
		// 得到旋转后的图片
		Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0,
				bitmap.getWidth(), bitmap.getHeight(), matrix, true);
		return resizedBitmap;
	}

	/**
	 * 读取图片属性：旋转的角度
	 * 
	 * @param path
	 *            图片绝对路径
	 * @return degree旋转的角度
	 */
	public static int readPictureDegree(String path) {
		int degree = 0;
		try {
			ExifInterface exifInterface = new ExifInterface(path);
			int orientation = exifInterface.getAttributeInt(
					ExifInterface.TAG_ORIENTATION,
					ExifInterface.ORIENTATION_NORMAL);
			switch (orientation) {
			case ExifInterface.ORIENTATION_ROTATE_90:
				degree = 90;
				break;
			case ExifInterface.ORIENTATION_ROTATE_180:
				degree = 180;
				break;
			case ExifInterface.ORIENTATION_ROTATE_270:
				degree = 270;
				break;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return degree;
	}
}
