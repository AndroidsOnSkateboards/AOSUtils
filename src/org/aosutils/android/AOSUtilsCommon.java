package org.aosutils.android;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class AOSUtilsCommon {
	public static String getAppName(Context context) {
		PackageManager packageManager = context.getPackageManager();
		ApplicationInfo appInfo = null;
		try {
			appInfo = packageManager.getApplicationInfo(context.getPackageName(), 0);
		} catch (NameNotFoundException e) {
			
		}
		return appInfo == null ? "null" : packageManager.getApplicationLabel(appInfo).toString();
	}
	public static String getAppVersionName(Context context) {
		PackageInfo appPackage = getAppPackage(context);
		return appPackage.versionName;
	}
	public static int getAppVersionCode(Context context) {
		PackageInfo appPackage = getAppPackage(context);
		return appPackage.versionCode;
	}
	public static PackageInfo getAppPackage(Context context) {
		PackageInfo appPackage = null;
		try {
			appPackage = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return appPackage;
	}
	
	public static boolean supportsCustomNotification() {
		return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB;
	}
	
	public static boolean isOnWifi(Context context) {
		WifiManager lWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		return lWifiManager == null ? false : lWifiManager.isWifiEnabled() && lWifiManager.getConnectionInfo() != null && lWifiManager.getConnectionInfo().getIpAddress() != 0;
	}
	
	public static SharedPreferences getDefaultSharedPreferences(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context);
	}
	
	public static void showKeyboard(EditText editText) {
		 InputMethodManager imm = (InputMethodManager) editText.getContext().getSystemService(Service.INPUT_METHOD_SERVICE);
		 imm.showSoftInput(editText, 0);
	}
	public static void hideKeyboard(EditText editText) {
		InputMethodManager imm = (InputMethodManager) editText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
	}
	
	public static void toast(String message, Context context) {
		Toast msg = Toast.makeText(context, message, Toast.LENGTH_LONG);
		msg.setGravity(Gravity.CENTER, msg.getXOffset() / 2, msg.getYOffset() / 2);
		msg.show();
	}
	
	public static void alert(String title, String message, Context context) {
		new AlertDialog.Builder(context).setTitle(title).setMessage(message).setPositiveButton(android.R.string.ok, null).show();
	}
	
	public static Dialog confirm(String title, String message, Context context, DialogInterface.OnClickListener onConfirmListener) {
		return confirmBuilder(title, context, onConfirmListener)
			.setMessage(message)
			.show();
	}
	
	public static Dialog confirm(String title, View view, Context context, DialogInterface.OnClickListener onConfirmListener) {
		Builder dialogBuilder = confirmBuilder(title, context, onConfirmListener)
			.setView(view);
		
		if (view instanceof TextView) {
			ScrollView scrollView = new ScrollView(context);
			scrollView.addView(view);
			dialogBuilder.setView(scrollView);
		}

		AlertDialog dialog = dialogBuilder.create();
		
		if (view instanceof EditText) {
			dialog.getWindow().setSoftInputMode(
					WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE |
					WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
			);
		}
		
		dialog.show();
		return dialog;
	}
	
	private static Builder confirmBuilder(String title, Context context, DialogInterface.OnClickListener onConfirmListener) {
		Builder builder = new AlertDialog.Builder(context)
		.setTitle(title)
		.setNegativeButton(android.R.string.cancel, null)
		.setPositiveButton(android.R.string.ok, onConfirmListener);
		
		return builder;
	}
}
