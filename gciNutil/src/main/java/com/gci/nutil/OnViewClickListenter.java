package com.gci.nutil;

import android.view.View;
import android.view.View.OnClickListener;

public abstract class OnViewClickListenter implements OnClickListener {

	private int _step = -1;

	public OnViewClickListenter(int step) {
		this._step = step;
	}

	private long curClickMills = System.currentTimeMillis();

	public abstract void onClick(boolean isPass, View view);

	@Override
	public void onClick(View view) {
		if ((System.currentTimeMillis() - curClickMills) > _step) {
			curClickMills = System.currentTimeMillis();
			onClick(true, view);
		}
	}

}
