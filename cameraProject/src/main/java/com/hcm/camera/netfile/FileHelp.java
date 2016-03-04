package com.hcm.camera.netfile;

import java.io.File;

import android.content.Context;
import android.os.Environment;

import com.gci.nutil.comm.CommonTool;

public class FileHelp {
	public static String getFilePath(Context mContext, String FileName) {
		File file = null;
		if (hasSdCard()) {
			file = mContext.getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
		} else {
			file = mContext.getApplicationContext().getFilesDir();
		}
		return file.getAbsolutePath() + "/" + FileName;
	}
	public static boolean hasSdCard(){
		return android.os.Environment.MEDIA_MOUNTED.equals(android.os.Environment.getExternalStorageState());
	}
}
