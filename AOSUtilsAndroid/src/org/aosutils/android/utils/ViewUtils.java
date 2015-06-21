package org.aosutils.android.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

public class ViewUtils {
	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	public static Drawable getDrawable(int resourceId, Context context) {
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
			return context.getResources().getDrawable(resourceId);
		}
		else {
			return context.getDrawable(resourceId);
		}		
	}
	
	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	public static void setBackground(Drawable drawable, View view) {
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
			view.setBackgroundDrawable(drawable);
		}
		else {
			view.setBackground(drawable);
		}		
	}
}
