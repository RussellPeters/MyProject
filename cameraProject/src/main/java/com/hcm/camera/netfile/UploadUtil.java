package com.hcm.camera.netfile;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

import android.util.Log;

public class UploadUtil {
	private static final String TAG = "uploadFile";
	private static final String CHARSET = "utf-8";

	private File _file;
	private String _uploadurl;
	private String _serverKey;
	private boolean _isUploading;
	private UploadFileListener _lis;
	private int TIME_OUT = 60 * 1000;
	private Thread t = new Thread(new Runnable() {
		@Override
		public void run() {

			_isUploading = true;
			String result = null;
			String BOUNDARY = UUID.randomUUID().toString();
			String PREFIX = "--", LINE_END = "\r\n";
			String CONTENT_TYPE = "multipart/form-data";
			try {
				URL url = new URL(_uploadurl);
				HttpURLConnection conn = (HttpURLConnection) url
						.openConnection();
				conn.setReadTimeout(TIME_OUT);
				conn.setConnectTimeout(TIME_OUT);
				conn.setDoInput(true);
				conn.setDoOutput(true);
				conn.setUseCaches(false);
				conn.setRequestMethod("POST");
				conn.setRequestProperty("Charset", CHARSET);
				conn.setRequestProperty("connection", "keep-alive");
				conn.setRequestProperty("Content-Type", CONTENT_TYPE
						+ ";boundary=" + BOUNDARY);

				if (_file != null) {
					DataOutputStream dos = new DataOutputStream(conn
							.getOutputStream());
					StringBuffer sb = new StringBuffer();
					sb.append(PREFIX);
					sb.append(BOUNDARY);
					sb.append(LINE_END);
					sb.append("Content-Disposition: form-data; name=\""
							+ _serverKey + "\"; filename=\"" + _file.getName()
							+ "\"" + LINE_END);
					sb.append("Content-Type: application/octet-stream; charset="
							+ CHARSET + LINE_END);
					sb.append(LINE_END);
					dos.write(sb.toString().getBytes());
					InputStream is = new FileInputStream(_file);
					byte[] bytes = new byte[1024];
					int len = 0;

					long filesize = _file.length();
					long sumUploadSize = 0;

					int curpostion = 0;

					while ((len = is.read(bytes)) != -1) {
						dos.write(bytes, 0, len);
						sumUploadSize += len;
						int postion = (int) ((sumUploadSize / filesize) * 100);
						if (postion > curpostion) {
							if (_lis != null)
								_lis.OnUploading(postion, _file);
						}
						curpostion = postion;
					}
					is.close();
					dos.write(LINE_END.getBytes());
					byte[] end_data = (PREFIX + BOUNDARY + PREFIX + LINE_END)
							.getBytes();
					dos.write(end_data);
					dos.flush();

					int res = conn.getResponseCode();
					// if(res==200)
					// {
					Log.e(TAG, "request success");
					InputStream input = conn.getInputStream();
					StringBuffer buffer = new StringBuffer();
					InputStreamReader inputReader = new InputStreamReader(input);
					BufferedReader bufferReader = new BufferedReader(
							inputReader);
					String str = new String("");
					while ((str = bufferReader.readLine()) != null) {
						buffer.append(str);
					}
					result = buffer.toString();
					if (_lis != null) {
						_lis.OnFisish(_file, result);
					}
				}
			} catch (Exception e) {
				if (_lis != null)
					_lis.OnError(0, e.getMessage());
			}
		}
	});

	public int getTIME_OUT() {
		return TIME_OUT;
	}

	public void setTIME_OUT(int tIME_OUT) {
		TIME_OUT = tIME_OUT;
	}

	public UploadUtil(File file, String serverKey, String uploadurl) {
		this._file = file;
		this._uploadurl = uploadurl;
		this._serverKey = serverKey;
	}
	
	

	public void setUploadFileListener(UploadFileListener lis) {
		this._lis = lis;
	}

	public void start() {
		if (_isUploading)
			return;
		if (this._lis != null)
			this._lis.OnStart();
		t.start();
	}
}