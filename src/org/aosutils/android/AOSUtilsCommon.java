package org.aosutils.android;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Locale;

import org.aosutils.StringUtils;
import org.aosutils.net.HttpUtils;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.Service;
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
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
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
	
	public static void submitBugReportPopup(final String subject, final String emailAddress, final boolean includeNetworkInfo, final Context context) {
		final StringBuilder publicIpAddress = new StringBuilder();
		new Thread(new Runnable() {
			@Override
			public void run() {
				publicIpAddress.append(HttpUtils.getPublicIpAddress(null));
			}
		}).start();
		
		final EditText editText = new EditText(context);
		editText.setImeOptions(EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE);
		editText.setMinLines(5);
		editText.setGravity(Gravity.TOP);
		confirm(StringUtils.toTitleCase(context.getString(R.string.help_BugReport)), editText, context, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String issue = editText.getText().toString();
				if (!issue.equals("")) {
					// Hopefully the IP address was retrieved while the user was typing in the "issue". Otherwise it will be obtained later.
					submitBugReport(subject, emailAddress, issue, includeNetworkInfo, publicIpAddress.toString(), context);
				}
			}
		});
	}
	
	public static void submitBugReport(String subject, String emailAddress, String bodyText, boolean includeNetworkInfo, String publicIpAddress, Context context) {
		String body = "";
		if (bodyText == null || bodyText.equals("")) {
			body += context.getString(R.string.help_BugReport) + "\n\n";
		}
		else {
			body += bodyText;
		}
		
		body += "\n\n\n\n";
		body += "System info: " + "\n";
		body += "-------------" + "\n";
		body += "App Version: " + String.format("%s %s (%s)", getAppName(context), getAppVersionName(context), getAppVersionCode(context)) + "\n";
		body += "System Language: " + Locale.getDefault().toString()  + " / " + Locale.getDefault().getDisplayLanguage() + "\n";
		body += "Device Manufacturer: " + (Build.MANUFACTURER.equals(Build.BRAND) ? Build.MANUFACTURER : String.format("%s / %s", Build.MANUFACTURER, Build.BRAND)) + "\n";
		body += "Model: " + Build.MODEL + "\n";
		body += "Device: " + (Build.DEVICE.equals(Build.PRODUCT) ? Build.DEVICE : String.format("%s / %s", Build.DEVICE, Build.PRODUCT)) + "\n";
		body += "Android Version: " + String.format("%s (API Level %s)", Build.VERSION.RELEASE, Build.VERSION.SDK_INT) + "\n";
		body += "OS Build: " + Build.DISPLAY + "\n";
		body += "Kernel: " + System.getProperty("os.version") + "\n";
		
		if (includeNetworkInfo) {		
			TelephonyManager lTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
			String simOperator = (lTelephonyManager == null || lTelephonyManager.getSimCountryIso().equals("") ? "null" : String.format("%s - %s (%s)", lTelephonyManager.getSimOperatorName(), lTelephonyManager.getSimCountryIso(), new Locale("", lTelephonyManager.getSimCountryIso()).getDisplayName()));
			String networkOperator = (lTelephonyManager == null || lTelephonyManager.getNetworkCountryIso().equals("") ? "null" : String.format("%s - %s (%s)", lTelephonyManager.getNetworkOperatorName(), lTelephonyManager.getNetworkCountryIso(), new Locale("", lTelephonyManager.getNetworkCountryIso()).getDisplayName()));
			
			body += "Wifi: " + isOnWifi(context) + "\n";
			body += "Phone Type: " + (lTelephonyManager == null ? "null" : String.format("%s (%s)", Integer.toString(lTelephonyManager.getPhoneType()), getTelephonyManagerDescription("PHONE_TYPE_", lTelephonyManager.getPhoneType()))) + "\n";
			if (!simOperator.equals(networkOperator)) {
				body += "SIM Operator: " + simOperator + "\n";
			}
			body += "Network Operator: " + networkOperator + "\n";
			body += "Network Type: " + (lTelephonyManager == null ? "null" : String.format("%s (%s)", Integer.toString(lTelephonyManager.getNetworkType()), getTelephonyManagerDescription("NETWORK_TYPE_", lTelephonyManager.getNetworkType()))) + "\n";
			body += "Public IP Address: " + publicIpAddress + "\n";
		}
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("message/rfc822");
		intent.putExtra(Intent.EXTRA_EMAIL, new String[] { emailAddress });
		intent.putExtra(Intent.EXTRA_SUBJECT, subject);
		intent.putExtra(Intent.EXTRA_TEXT, body);
		try {
		    context.startActivity(Intent.createChooser(intent, subject));
		} catch (ActivityNotFoundException e) {
			AOSUtilsCommon.toast(context.getString(R.string.error_NoEmailApps), context);
		}
	}
	
	// Will provide a description of phone type (Eg. GSM) or network type (Eg. LTE) via reflection to be compatible with all Android API Levels
	private static String getTelephonyManagerDescription(String constantPrefix, int constantValue) {
		ArrayList<String> matchingValues = new ArrayList<String>();
		
		Field[] fields = TelephonyManager.class.getFields();
		for (Field field : fields) {
			if (field.getName().startsWith(constantPrefix)) {
				try {
					if (field.getInt(null) == constantValue) {
						String matchingValue = field.getName().replace(constantPrefix, "");
						matchingValues.add(matchingValue);
					}
				} catch (Exception e) {
					
				} 
			}
		}
		
		return TextUtils.join(",  ", matchingValues);
	}
}
