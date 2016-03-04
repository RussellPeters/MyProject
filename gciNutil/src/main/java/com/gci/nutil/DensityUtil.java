package com.gci.nutil;

import android.content.Context;

public class DensityUtil {
	private static float density = 0f;
	private static float defaultDensity = 1.5f;// 楂樺垎杈ㄧ巼鐨勬墜鏈篸ensity鏅亶鎺ヨ繎1.5

	private DensityUtil() {
	}

	public static void setDensity(float density) {
		DensityUtil.density = density;
	}

	public static float getDensity(Context context) {
		// DisplayMetrics metrics = new DisplayMetrics();
		// Display display = ((Activity) context).getWindowManager()
		// .getDefaultDisplay();
		// display.getMetrics(metrics);
		// return metrics.density;
		return context.getResources().getDisplayMetrics().density;
	}

	public static int getScreenWidth(Context context) {
		// DisplayMetrics metrics = new DisplayMetrics();
		// Display display = ((Activity) context).getWindowManager()
		// .getDefaultDisplay();
		// display.getMetrics(metrics);
		// return metrics.widthPixels;
		return context.getResources().getDisplayMetrics().widthPixels;
	}

	public static int getScreenHeight(Context context) {
		// DisplayMetrics metrics = new DisplayMetrics();
		// Display display = ((Activity) context).getWindowManager()
		// .getDefaultDisplay();
		// display.getMetrics(metrics);
		// return metrics.heightPixels;
		return context.getResources().getDisplayMetrics().heightPixels;
	}

	/**
	 * @Title dp2px
	 * @Description 灏哾ip鎴杁p鍊艰浆鎹负px鍊硷紝淇濊瘉灏哄澶у皬涓嶅彉
	 * @author 闄堝浗瀹�	 * @date 2014骞�鏈�0鏃�涓嬪崍1:48:55
	 * @param dpValue
	 * @return
	 */
	public static int dp2px(Context context, float dpValue) {
		int px;
		if (density == 0) {
			if (context != null) {
				density = getDensity(context);
			}
			if (density == 0) {
				density = defaultDensity;
			}
		}
		px = (int) (dpValue * density + 0.5f);
//		XLog.i(TAG, "px = " + px);
		return px;
	}

	/**
	 * @Title px2dp
	 * @Description 灏唒x鍊艰浆鎹负dip鎴杁p鍊硷紝淇濊瘉灏哄澶у皬涓嶅彉
	 * @author 闄堝浗瀹�	 * @date 2014骞�鏈�0鏃�涓嬪崍1:48:30
	 * @param pxValue
	 * @return dp
	 */
	public static int px2dp(Context context, float pxValue) {
		int dp;
		if (density == 0) {
			if (context != null) {
				density = getDensity(context);
			}
			if (density == 0) {
				density = defaultDensity;
			}
		}
		dp = (int) (pxValue / density + 0.5f);
//		XLog.i(TAG, "dp = " + dp);
		return dp;
	}

	/**
	 * @Title px2sp
	 * @Description 灏唒x鍊艰浆鎹负sp鍊硷紝淇濊瘉鏂囧瓧澶у皬涓嶅彉
	 * @author 闄堝浗瀹�	 * @date 2014骞�鏈�0鏃�涓嬪崍2:06:26
	 * @param context
	 * @param pxValue
	 * @return sp
	 */
	public static int px2sp(Context context, float pxValue) {
		final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
		return (int) (pxValue / fontScale + 0.5f);
	}

	/**
	 * @Title sp2px
	 * @Description 灏唖p鍊艰浆鎹负px鍊硷紝淇濊瘉鏂囧瓧澶у皬涓嶅彉
	 * @author 闄堝浗瀹�	 * @date 2014骞�鏈�0鏃�涓嬪崍2:07:00
	 * @param context
	 * @param spValue
	 * @return px
	 */
	public static int sp2px(Context context, float spValue) {
		final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
		return (int) (spValue * fontScale + 0.5f);
	}

	/**
	 * @Title dp2sp
	 * @Description 灏哾p鍊艰浆鎹负sp鍊硷紝淇濊瘉鏂囧瓧澶у皬涓嶅彉
	 * @author 闄堝浗瀹�	 * @date 2014骞�鏈�0鏃�涓嬪崍2:07:09
	 * @param context
	 * @param dpValue
	 * @return sp
	 */
	public static int dp2sp(Context context, float dpValue) {
		if (density == 0) {
			density = getDensity(context);
		}
		return px2sp(context, dp2px(context, dpValue));
	}

	/**
	 * @Title sp2dp
	 * @Description 灏唖p鍊艰浆鎹负px鍊硷紝淇濊瘉鏂囧瓧澶у皬涓嶅彉
	 * @author 闄堝浗瀹�	 * @date 2014骞�鏈�0鏃�涓嬪崍2:07:45
	 * @param context
	 * @param spValue
	 * @return dp
	 */
	public static int sp2dp(Context context, float spValue) {
		if (density == 0) {
			density = getDensity(context);
		}
		return px2dp(context, sp2px(context, spValue));
	}
}