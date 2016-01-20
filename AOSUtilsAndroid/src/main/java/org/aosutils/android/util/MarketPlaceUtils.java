package org.aosutils.android.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;

public class MarketPlaceUtils {
	public enum MarketPlace { GooglePlay, Amazon, Other }
	
	public static String getMarketPlace(Context context) {
		String installerId = null;
		
		try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR) {
				installerId = getInstallerId(context);
			}
		}
		catch (Exception e) { }
		
		if ("com.android.vending".equals(installerId) || "com.google.android.feedback".equals(installerId)) {
			return MarketPlace.GooglePlay.toString();
		}
		else if ("com.amazon.venezia".equals(installerId)) {
		    return MarketPlace.Amazon.toString(); 
		}
		else {
			return installerId;
		}
	}
	
	@TargetApi(Build.VERSION_CODES.ECLAIR)
	private static String getInstallerId(Context context) {
		return context.getPackageManager().getInstallerPackageName(context.getPackageName());
	}
}
