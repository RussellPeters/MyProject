package com.hcm.camera.comm;

import java.lang.reflect.Type;
import java.util.Hashtable;
import java.util.UUID;

import android.content.Context;
import android.os.Environment;
import android.os.PowerManager;
import android.util.Log;

import com.google.gson.Gson;

public class CommTool {

	private static Hashtable<String, Hashtable<String, Integer>> _worksegdict = new Hashtable<String, Hashtable<String, Integer>>();

	public static byte[] getAddressByName(String ip) {
		String[] ipStr = ip.split("\\.");
		byte[] ipBuf = new byte[4];
		for (int i = 0; i < 4; i++) {
			ipBuf[i] = (byte) (Integer.parseInt(ipStr[i]) & 0xff);
		}
		return ipBuf;
	}

	/** 通过 功能编号 获取当前的 作业序号 流水 */
	public static int getWorkSegByFunctionNo(String funtionno, String messageno) {
		Hashtable<String, Integer> mssagenohas = null;
		int curworkseg = 1;
		if (_worksegdict.containsKey(funtionno)) {
			mssagenohas = _worksegdict.get(funtionno);
		} else {
			mssagenohas = new Hashtable<String, Integer>();
			_worksegdict.put(funtionno, mssagenohas);
		}
		if (mssagenohas.containsKey(messageno)) {
			curworkseg = mssagenohas.get(messageno);
		} else {
			mssagenohas.put(messageno, 1);
		}

		curworkseg += 1;
		mssagenohas.put(messageno, curworkseg);
		return curworkseg;
	}

	public static <T> T getInstanceByJson(String json, Class<T> c) {
		T result = null;
		try {
			result = (T) gson.fromJson(json, c);
		} catch (Exception e) {
			Log.e("Tag", e.getMessage());
			e.printStackTrace();
		}
		return result;
	}

	public static Gson gson = new Gson();

	// 获取锁
	public static PowerManager.WakeLock acquireWakeLock(Context context) {
		PowerManager powerManager = (PowerManager) (context
				.getSystemService(Context.POWER_SERVICE));
		PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
				PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag"
						+ context.getClass().getName());
		wakeLock.acquire();
		return wakeLock;
	}

	// 释放锁
	public static void releaseWakeLock(PowerManager.WakeLock wakeLock) {
		if (wakeLock != null && wakeLock.isHeld()) {
			wakeLock.release();
		}
	}
	
	public static String getGUID(){
		 UUID uid = UUID.randomUUID();
		 return uid.toString();
	}
	
	public static boolean hasSDCard() {
		return Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState());
	}
}
