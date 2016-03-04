package com.hcm.camera.bluetooth;

import java.util.ArrayList;

public class BluetoothMessageManager {
	private static BluetoothMessageManager _b = null;
	private StringBuilder _strb = new StringBuilder(3000);
	private ArrayList<OnBluetoothMessageChange> _lst = new ArrayList<OnBluetoothMessageChange>();

	public static BluetoothMessageManager getInstance() {
		if (_b == null) {
			_b = new BluetoothMessageManager();
		}
		return _b;
	}

	public void addCallback(OnBluetoothMessageChange callback) {
		this._lst.add(callback);
	}
	
	public void removeCallback(OnBluetoothMessageChange callback){
		this._lst.remove(callback);
	}
	
	public void addMessage(String str){
		for(OnBluetoothMessageChange itm:_lst){
			itm.onChange(this._strb, str);
		}
		this._strb.append(str + "\n");
	}
}
