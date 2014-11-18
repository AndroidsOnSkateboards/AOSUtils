package org.aosutils.android;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Locale;

import org.aosutils.StringUtils;
import org.aosutils.android.loadingtask.LoadingTask;
import org.aosutils.api.MicrosoftAzure;
import org.aosutils.net.HttpUtils;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

public class BugReport {
	public static void submitBugReportPopup(final String subject, final String toEmailAddress, final ArrayList<Uri> attachments, final boolean includeNetworkInfo, final Activity loadingTaskActivity, final String bingTranslateClientId, final String bingTranslateClientSecret) {
		final StringBuilder publicIpAddressBuilder = new StringBuilder();
		new Thread(new Runnable() {
			@Override
			public void run() {
				publicIpAddressBuilder.append(HttpUtils.getPublicIpAddress(null));
			}
		}).start();
		
		final EditText editText = new EditText(loadingTaskActivity);
		editText.setImeOptions(EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE);
		editText.setMinLines(5);
		editText.setGravity(Gravity.TOP);
		AOSUtilsCommon.confirm(StringUtils.toTitleCase(loadingTaskActivity.getString(R.string.help_BugReport)), editText, loadingTaskActivity, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String issue = editText.getText().toString();
				if (!issue.equals("")) {
					// Hopefully the IP address was retrieved while the user was typing in the "issue".
					String publicIpAddress = publicIpAddressBuilder.toString();
					
					boolean systemLanguageIsEnglish = Locale.getDefault().toString().startsWith("en");
					
					if (!systemLanguageIsEnglish && bingTranslateClientId != null && bingTranslateClientSecret != null) {
						// Translate
						TranslateIssueTask task = new TranslateIssueTask(subject, toEmailAddress, issue, attachments, includeNetworkInfo, publicIpAddress, loadingTaskActivity, bingTranslateClientId, bingTranslateClientSecret);
						task.execute();
					}
					else {
						// Don't translate
						submitBugReport(subject, toEmailAddress, issue, attachments, includeNetworkInfo, publicIpAddress, loadingTaskActivity);
					}
				}
			}
		});
	}
	
	private static class TranslateIssueTask extends LoadingTask<Object, Object, Object> {
		private String subject;
		private String toEmailAddress;
		private String issue;
		private ArrayList<Uri> attachments;
		private boolean includeNetworkInfo;
		private String publicIpAddress;
		private String bingTranslateClientId;
		private String bingTranslateClientSecret;
		
		public TranslateIssueTask(String subject, String toEmailAddress, String issue, ArrayList<Uri> attachments, boolean includeNetworkInfo, String publicIpAddress, Activity loadingTaskActivity, String bingTranslateClientId, String bingTranslateClientSecret) {
			super(loadingTaskActivity);
			this.subject = subject;
			this.toEmailAddress = toEmailAddress;
			this.issue = issue;
			this.attachments = attachments;
			this.includeNetworkInfo = includeNetworkInfo;
			this.publicIpAddress = publicIpAddress;
			this.bingTranslateClientId = bingTranslateClientId;
			this.bingTranslateClientSecret = bingTranslateClientSecret;
		}
		
		@Override
		protected Object doInBackground(Object... params) {
			try {
				return MicrosoftAzure.translate(issue, null, "en", bingTranslateClientId, bingTranslateClientSecret).trim();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Object result) {
			super.onPostExecute(result);
			
			String translatedIssue = (String) result;
			String bodyText = issue;
			
			bodyText += "\n\n" + "English:" + "\n";
			bodyText += translatedIssue == null ? "(problem with translation)" : translatedIssue;
			
			submitBugReport(subject, toEmailAddress, bodyText, attachments, includeNetworkInfo, publicIpAddress, getActivity());
		}
		
	}
	
	public static void submitBugReport(String subject, String toEmailAddress, String bodyText, ArrayList<Uri> attachments, boolean includeNetworkInfo, String publicIpAddress, Context context) {
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
		body += "App Version: " + String.format("%s %s (%s)", AOSUtilsCommon.getAppName(context), AOSUtilsCommon.getAppVersionName(context), AOSUtilsCommon.getAppVersionCode(context)) + "\n";
		body += "System Language: " + Locale.getDefault().toString() + " / " + Locale.getDefault().getDisplayLanguage(Locale.US) + "\n";
		body += "Device Manufacturer: " + (Build.MANUFACTURER.equals(Build.BRAND) ? Build.MANUFACTURER : String.format("%s / %s", Build.MANUFACTURER, Build.BRAND)) + "\n";
		body += "Model: " + Build.MODEL + "\n";
		body += "Device: " + (Build.DEVICE.equals(Build.PRODUCT) ? Build.DEVICE : String.format("%s / %s", Build.DEVICE, Build.PRODUCT)) + "\n";
		body += "Android Version: " + String.format("%s (API Level %s)", Build.VERSION.RELEASE, Build.VERSION.SDK_INT) + "\n";
		body += "OS Build: " + Build.DISPLAY + "\n";
		body += "Kernel: " + System.getProperty("os.version") + "\n";
		
		if (includeNetworkInfo) {		
			TelephonyManager lTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
			
			body += "Wifi: " + AOSUtilsCommon.isOnWifi(context) + "\n";
			body += "Phone Type: " + (lTelephonyManager == null ? "null" : String.format("%s (%s)", Integer.toString(lTelephonyManager.getPhoneType()), getTelephonyManagerDescription("PHONE_TYPE_", lTelephonyManager.getPhoneType()))) + "\n";
			body += "Network Operator: " + (lTelephonyManager == null || lTelephonyManager.getNetworkCountryIso().equals("") ? "null" : String.format("%s - %s (%s)", lTelephonyManager.getNetworkOperatorName(), lTelephonyManager.getNetworkCountryIso(), new Locale("", lTelephonyManager.getNetworkCountryIso()).getDisplayName(Locale.US))) + "\n";
			body += "Network Type: " + (lTelephonyManager == null ? "null" : String.format("%s (%s)", Integer.toString(lTelephonyManager.getNetworkType()), getTelephonyManagerDescription("NETWORK_TYPE_", lTelephonyManager.getNetworkType()))) + "\n";
			body += "Public IP Address: " + publicIpAddress + "\n";
		}
		
		Intent intent;
		if (attachments == null) {
			intent = new Intent(Intent.ACTION_SEND);
			intent.setType("message/rfc822");
		}
		else {
			intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
			intent.setType("message/rfc822");
			intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, attachments);
		}
		
		intent.putExtra(Intent.EXTRA_EMAIL, new String[] { toEmailAddress });
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
