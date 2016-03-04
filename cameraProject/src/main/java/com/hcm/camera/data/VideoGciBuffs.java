package com.hcm.camera.data;

import android.util.Log;

public class VideoGciBuffs {
	private RTPBuffModel[] _buff;
	private Object lock = new Object();

	private int min = 0;
	private int mout = 0;
	public int cat = 0;
	private int temp = 0;
	private int tt = 0;
	
	//private boolean isArrowInput = true;
	
	public VideoGciBuffs(int buffcount, int buffsize) {
		this.cat = buffcount;
		this.tt = buffcount;
		_buff = new RTPBuffModel[buffcount];
		for(int i = 0; i < _buff.length;i++){
			_buff[i] = new RTPBuffModel(buffsize);
		}
	}
	
	public void init(){
		synchronized (lock) {
			cat = tt;
			mout = 0;
			min = 0;
			temp = 0;
			for(int i = 0;i < this.tt;i++){
				_buff[i].init();
			}
		}
	}

	public RTPBuffModel getInBuff() {
		synchronized (lock) {
//			if(!isArrowInput)
//				return null;
			if (cat == 0){
				Log.e("Tag", "缓存爆炸");
				return null;
			}else {
				//isArrowInput = false;
				return _buff[min];
			}
		}
	}

	public void commitBuff() {
		synchronized (lock) {
			cat--;
			min++;
			if (min == tt) {
				min = 0;
			}
			//isArrowInput = true;
		}
	}
	/**
	 * ��ȡ����
	 * @return
	 */
	public RTPBuffModel getOutBuff() {
		synchronized (lock) {
			if (cat == tt) {
				return null;
			} else {
				cat++;
				temp = mout;
				mout++;
				if (mout == tt)
					mout = 0;
				return _buff[temp];
			}
		}
	}

}
