package com.hcm.camera.net.model.Commun;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class VideoSharePreference {
	private static VideoSharePreference _m;
	private SharedPreferences share;
	private String name;
	private static final String IP_ADDRESS = "IpAddress";
	private static final String WEB_ADDRESS = "WebAddress";
	private static final String BT_ADDRESS = "";
	private static final String USER_DATA = "UserData";
	
	public static VideoSharePreference getInstance(Context context) {
		if (_m == null) {
			_m = new VideoSharePreference(context);
		}
		return _m;
	}

	private VideoSharePreference(Context context) {
		name = context.getPackageName() + "_share";
		share = context.getSharedPreferences(name, context.MODE_PRIVATE);
	}

	public void setIpAddress(String ip) {
		Editor edit = share.edit();
		edit.putString(IP_ADDRESS, ip);
		edit.commit();
	}

	public String getIpAddress() {
		return share.getString(IP_ADDRESS, "");
	}

	public void setWebAddress(String weburl) {
		Editor edit = share.edit();
		edit.putString(WEB_ADDRESS, weburl);
		edit.commit();
	}

	public String getWebAddress() {
		return share.getString(WEB_ADDRESS, "");
	}
	
	
	public String getBT(){
		return share.getString(BT_ADDRESS, "");
	}
	
	public void setBT(String address){
		Editor edit = share.edit();
		edit.putString(BT_ADDRESS, address);
		edit.commit();
	}
	
	public String getUserData(){
		return share.getString(USER_DATA, "");
	}
	
	public void setUserData(String data){
		Editor edit = share.edit();
		edit.putString(USER_DATA, data);
		edit.commit();
	}
}
