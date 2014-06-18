package org.aosutils.android;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

public class AOSUtilsCommon {
	//public final static String USER_AGENT_DESKTOP = "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)";
	public final static String USER_AGENT_DESKTOP = "Mozilla/5.0 (Windows NT 6.1; rv:31.0) Gecko/20100101 Firefox/31.0";
	
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
	
	public static void hideKeyboard(View view) {
		InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
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
		return confirmBuilder(title, context, onConfirmListener)
			.setView(view)
			.show();
	}
	
	private static Builder confirmBuilder(String title, Context context, DialogInterface.OnClickListener onConfirmListener) {
		Builder builder = new AlertDialog.Builder(context)
		.setTitle(title)
		.setNegativeButton(android.R.string.cancel, null)
		.setPositiveButton(android.R.string.ok, onConfirmListener);
		
		return builder;
	}
	
	public static void submitBugReport(String subject, String emailAddress, boolean includeNetworkInfo, Context context) {
		String body = context.getString(R.string.help_BugReport) + "\n\n\n\n\n\n";
		body += "System info: " + "\n";
		body += "-------------" + "\n";
		body += "App Version: " + String.format("%s %s (%s)", getAppName(context), getAppVersionName(context), getAppVersionCode(context)) + "\n";
		body += "Manufacturer: " + Build.MANUFACTURER + "\n";
		body += "Brand: " + Build.BRAND + "\n";
		body += "Device: " + Build.DEVICE + "\n";
		body += "Product: " + Build.PRODUCT + "\n";
		body += "Model: " + Build.MODEL + "\n";
		body += "Android Version: " + String.format("%s (API Level %s)", Build.VERSION.RELEASE, Build.VERSION.SDK_INT) + "\n";
		body += "OS Build: " + Build.DISPLAY + "\n";
		body += "Kernel: " + System.getProperty("os.version") + "\n";
		
		if (includeNetworkInfo) {
			TelephonyManager lTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
			
			body += "Wifi: " + isOnWifi(context) + "\n";
			body += "Phone Type: " + (lTelephonyManager == null ? "null" : Integer.toString(lTelephonyManager.getPhoneType())) + "\n";
			body += "Network Operator: " + (lTelephonyManager == null ? "null" : lTelephonyManager.getNetworkCountryIso() + " -" + lTelephonyManager.getNetworkOperatorName()) + "\n";
			body += "Network Type: " + (lTelephonyManager == null ? "null" : Integer.toString(lTelephonyManager.getNetworkType())) + "\n";
		}
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("message/rfc822");
		intent.putExtra(Intent.EXTRA_EMAIL, new String[] { emailAddress });
		intent.putExtra(Intent.EXTRA_SUBJECT, subject);
		intent.putExtra(Intent.EXTRA_TEXT, body);
		try {
		    context.startActivity(Intent.createChooser(intent, context.getString(R.string.action_SubmitBugReport)));
		} catch (ActivityNotFoundException e) {
			AOSUtilsCommon.toast(context.getString(R.string.error_NoEmailApps), context);
		}
	}
}
