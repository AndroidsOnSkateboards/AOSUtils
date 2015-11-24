package org.aosutils.android.social;

import org.aosutils.android.AOSUtilsCommon;
import org.aosutils.android.OAuthLoginActivity;
import org.aosutils.android.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.webkit.CookieManager;

public class SocialCommon {
	
	protected static void oauthLogin(int requestCode, String url, String[] paramsToReturn, Activity activity) {
    	Intent myIntent = new Intent(activity, OAuthLoginActivity.class);
		
    	myIntent.putExtra(OAuthLoginActivity.URL, url);
		myIntent.putExtra(OAuthLoginActivity.PARAMS_TO_RETURN, paramsToReturn);
		activity.startActivityForResult(myIntent, requestCode);
    }
	
	public static boolean isLoggedIn(Context context) {
		SharedPreferences preferences = AOSUtilsCommon.getDefaultSharedPreferences(context);
		return preferences.contains(context.getString(R.string.aosutils_pref_CookiesStored));
	}
	
	public static void logout(Context context) {
		Editor editor = AOSUtilsCommon.getDefaultSharedPreferences(context).edit();
		editor.remove(context.getString(R.string.aosutils_pref_CookiesStored));
		
		editor.remove(context.getString(R.string.aosutils_pref_FacebookApiToken));
		editor.remove(context.getString(R.string.aosutils_pref_TwitterApiToken));
		editor.remove(context.getString(R.string.aosutils_pref_TwitterApiTokenSecret));
		editor.commit();
		
		CookieManager.getInstance().removeAllCookie();
	}
}
