package com.hcm.camera.data;

import java.util.LinkedList;

import android.content.Context;

import com.gci.nutil.base.BaseActivity;
import com.hcm.camera.net.model.Commun.SendMessageModel;
import com.hcm.camera.net.model.Commun.VideoSharePreference;
import com.hcm.camera.net.model.user.UserInfo;


public class GroupVarManager {

	private static GroupVarManager _p = null;

	private BaseActivity _con;

	private String httpurl = "";

	public String userId = "";

	public char[] getUserIdChars() {
		return this.userId.toCharArray();
	}

	public UserInfo loginUserInfo;

	public LinkedList<SendMessageModel> chats = new LinkedList<SendMessageModel>();

	public int Seq = 0;

	public static GroupVarManager getIntance(BaseActivity base) {
		if (_p == null) {
			_p = new GroupVarManager();
			_p._con = base;
		}
		return _p;
	}

	public static GroupVarManager getIntance() {
		if (_p == null) {
			_p = new GroupVarManager();
		}
		return _p;
	}

	public String getUploadUrl() {
		if ("".equals(httpurl))
			httpurl = VideoSharePreference.getInstance(_con).getWebAddress();
		return httpurl + "File/UploadFile?";
	}

	public String getDownLoadUrl(String filename) {
		if ("".equals(httpurl))
			httpurl = VideoSharePreference.getInstance(_con).getWebAddress();
		return httpurl + "File/GetFileFromDisk?filename=" + filename;
	}
	
	public String getFilePath(Context content,String filename){
		return content.getApplicationContext().getFilesDir().getAbsolutePath() + "/" + filename;
	}
}
