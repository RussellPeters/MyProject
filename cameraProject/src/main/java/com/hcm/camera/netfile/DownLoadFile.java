package com.hcm.camera.netfile;

import android.content.*;
import android.os.*;
import android.util.*;

import java.io.*;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

import org.apache.http.util.ByteArrayBuffer;

/**
 * �ļ����ع�����
 * */
public class DownLoadFile extends Thread {

	private String _filepath;
	private String _serverurl;
	private String _filename;
	private Context _con;
	private DownLoadFileLisenter _lis;
	private FileOutputStream fout;

	private byte[] _buffs = new byte[1024];

	private boolean _isLoading;

	public interface DownLoadFileLisenter {
		public void OnStart();

		public void OnDownLoading(int postion);

		public void OnDownLoadError(int errcode, String errmessage);

		public void OnFisish(String filePath, String fileName, File file);
	}

	public DownLoadFile(String serverUrl, String filePath, String fileName,
			Context context, DownLoadFileLisenter lis) {
		this._filename = fileName;
		this._filepath = filePath;
		this._con = context;
		this._serverurl = serverUrl;
		this._lis = lis;
	}

	@Override
	public synchronized void start() {
		if (_isLoading)
			return;
		super.start();
	}

	@Override
	public void run() {
		try {
			URL url = new URL(this._serverurl);
			HttpURLConnection urlCon = (HttpURLConnection) url.openConnection();
			urlCon.setConnectTimeout(10000);
			urlCon.setReadTimeout(10000);
			long startTime = System.currentTimeMillis();
			Log.d("FileDownloader", "download begining");
			Log.d("FileDownloader", "download url:" + url);
			Log.d("FileDownloader", "downloaded file name:" + this._filename);

			/* Open a connection to that URL. */
			URLConnection ucon = url.openConnection();
			_isLoading = true;
			/*
			 * * Define InputStreams to read from the URLConnection.
			 */
			InputStream is = ucon.getInputStream();
			BufferedInputStream bis = new BufferedInputStream(is);

			File file = new File(this._filepath);
			if (!file.exists())
				file.createNewFile();

			fout = new FileOutputStream(file);
			int current = 0;
			while ((current = bis.read(_buffs, 0, _buffs.length)) != -1
					&& !Thread.interrupted()) {
				fout.write(_buffs, 0, current);
			}
			if (Thread.interrupted()) {
				if (this._lis != null) {
					this._lis.OnDownLoadError(-1, "取消");
				}
			}

			if (this._lis != null)
				this._lis.OnFisish(this._filepath, this._filename, file);
		} catch (IOException e) {
			_isLoading = false;
			if (this._lis != null) {
				this._lis.OnDownLoadError(0, e.getMessage());
			}
			Log.d("ImageManager", "Error: " + e);
		} finally {
			if (fout != null) {
				try {
					fout.flush();
					fout.close();
				} catch (IOException e) {
				}
			}
		}
		super.run();
	}

	/**
	 * ��SD���ϴ���Ŀ¼
	 * 
	 * @param dirName
	 * @return
	 */
	// public static File createSDDir(String dirName) {
	// try {
	// File dir = new File(Environment.getExternalStorageDirectory()
	// .getCanonicalFile() + "/" + dirName);
	// if (!dir.exists()) {
	// dir.mkdir();
	// }
	// return dir;
	// } catch (Exception ex) {
	// return null;
	// }
	// }

	public File createSDFile(String fileName) throws IOException {
		File file = new File(Environment.getExternalStorageDirectory()
				.getCanonicalFile() + "/" + fileName);
		file.createNewFile();
		return file;
	}

	public void writeFileData(String fileName, byte[] bytes, Context context) {
		try {
			FileOutputStream fout = context.openFileOutput(fileName,
					context.MODE_PRIVATE);
			fout.write(bytes);
			fout.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
