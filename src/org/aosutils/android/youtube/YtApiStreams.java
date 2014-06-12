package org.aosutils.android.youtube;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.regex.Pattern;

import org.aosutils.android.AOSUtilsCommon;
import org.aosutils.android.R;
import org.aosutils.net.HttpUtils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

public class YtApiStreams {
	/*
	 * Known formats:
	 * 17 - Video+Audio - 144p   - ~3MB average
	 * 18 - Video+Audio - 360p   - ~12MB average
	 * 22 - Video+Audio - 720p
	 * 37 - Video+Audio - 1080p
	 * 
	 * 133 - Video only - 240p
	 * 134 - Video only - 360p
	 * 135 - Video only - 480p
	 * 136 - Video only - 720p
	 * 137 - Video only - 1080p
	 * 
	 * 139 - Audio only - Low
	 * 140 - Audio only - Medium - ~3MB average
	 * 141 - Audio only - High
	 * 
	 * Found lots of info at: http://users.ohiohills.com/fmacall/YTCRACK.TXT
	 * 
	 */
	
	public enum StreamType {
		VIDEO_AND_AUDIO, AUDIO_ONLY, AUDIO_ONLY_LOW_QUALITY;
	}
	private enum ConnectionType {
		SLOW, MEDIUM, FAST;
	}
	
	public static String findStream(String videoId, Context context, StreamType streamType) throws FileNotFoundException, MalformedURLException, IOException {
		String[] recommendedFormats = getRecommendedFormats(context, streamType);
		return findRecommendedUrl(videoId, recommendedFormats, context);
	}
	
	// Returns recommended formats, ordered from most recommended to least recommended
	private static String[] getRecommendedFormats(Context context, StreamType streamType) {
		ConnectionType connectionType = getConnectionType(context);
		
		if (streamType == StreamType.AUDIO_ONLY) {
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
				// Device supports Audio-only stream; use "medium" quality audio stream (only one ever found & tested)
				return connectionType == ConnectionType.FAST || connectionType == ConnectionType.MEDIUM ? 
						new String[] { "140", "18", "17" } :
						new String[] { "140", "17" };
			}
			else {
				// Only video streams supported
				return connectionType == ConnectionType.FAST || connectionType == ConnectionType.MEDIUM ? 
						new String[] { "18", "17" } :
						new String[] { "17" };
			}
		}
		else if (streamType == StreamType.AUDIO_ONLY_LOW_QUALITY) {
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
				// Device supports Audio-only stream; use "medium" quality audio stream (only one ever tested)
				return new String[] { "140", "17" };
			}
			else {
				// Only video streams supported
				return new String[] { "17" };
			}
		}
		else {
			if (connectionType == ConnectionType.FAST) {
				return new String[] { "22", "18", "17" };
			}
			else if (connectionType == ConnectionType.MEDIUM) {
				return new String[] { "18", "17" };
			}
			else {
				return new String[] { "17" };
			}
		}
	}
	
	private static ConnectionType getConnectionType(Context context) {
		// For a full list of networks, see class: android.telephony.TelephonyManager
		
		WifiManager lWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		TelephonyManager lTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		
		// If we have a fast connection (WiFi or 3G), then use a medium quality YouTube video
		if (
			(lWifiManager.isWifiEnabled() && lWifiManager.getConnectionInfo() != null && lWifiManager.getConnectionInfo().getIpAddress() != 0) ||
			(lTelephonyManager.getDataState() == TelephonyManager.DATA_CONNECTED && 
				(
					lTelephonyManager.getNetworkType() >= 13 /* LTE */ &&  
					lTelephonyManager.getNetworkType() != 15 /* HSPA+, should be considered MEDIUM */
				)
			)) {
			return ConnectionType.FAST;
		}
		else if (
			lTelephonyManager.getDataState() == TelephonyManager.DATA_CONNECTED &&
				(
					lTelephonyManager.getNetworkType() >= 3 /* UMTS / EVDO / HSPA */ &&  
					lTelephonyManager.getNetworkType() != 4 /* CDMA, should be considered SLOW */ &&
					lTelephonyManager.getNetworkType() != 11 /* iDEN, should be considered SLOW */
				)
			) {
			return ConnectionType.MEDIUM;
		}
		else {
			return ConnectionType.SLOW;
		}
	}
	
	private static String findRecommendedUrl(String videoId, String[] recommendedFormats, Context context) throws FileNotFoundException, MalformedURLException, IOException {
		HashMap<String, String> urls = getFormatsFromDesktopSite(videoId, context);
		if (urls.size() == 0) {
			urls = getFormatsFromVideoInformation(videoId, context);
		}
		
		for (String recommendedFormat : recommendedFormats) {
			if (urls.containsKey(recommendedFormat)) {
				return urls.get(recommendedFormat);
			}
		}
		
		// Recommended format wasn't found, look for any format
		if (urls.size() > 0) {
			// Generally sorted by highest format first
			return urls.get(0);
		}
		
		// Finally, give up
		return null;
	}
	
	private static HashMap<String, String> getFormatsFromDesktopSite(String videoId, Context context) throws FileNotFoundException, MalformedURLException, IOException {
		String uri = "http://www.youtube.com/watch?v=" + videoId;
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put("User-Agent", AOSUtilsCommon.USER_AGENT_DESKTOP);
		String page = HttpUtils.get(uri, headers, _YtApiConstants.HttpTimeout);
		
		// Identify signature algorithm version
		String playerVersion = YtApiSignature.getCurrentVersion(page);
		if (playerVersion != null) {
			SharedPreferences preferences = AOSUtilsCommon.getDefaultSharedPreferences(context);
			
			String prefName = context.getResources().getString(R.string.aosutils_pref_YoutubeSignatureAlgorithm);
			String prefValue = preferences.getString(prefName, null);
			
			if (prefValue == null || !(TextUtils.split(prefValue, Pattern.quote("||"))[0].equals(playerVersion))) {
				// Algorithm needs update
				try {
					String algorithm = YtApiSignature.requestCurrentAlgorithm(page);
					prefValue = playerVersion + "||" + algorithm;
					
					preferences.edit().putString(prefName, prefValue).commit();
				}
				catch (IOException e) {
					// Don't stop parsing formats if this fails
					e.printStackTrace();
				}
			}
		}
		
		String algorithm = loadAlgorithmFromPreferences(context);
		
		// Parse formats
		HashMap<String, String> formats = new HashMap<String, String>();
		
		String[] mapNames = { "url_encoded_fmt_stream_map", "adaptive_fmts" };
		
		for (String mapName : mapNames) {
			String map = String.format("\"%s\": \"", mapName);
			int pos = page.indexOf(map);
			if (pos != -1) {
				int begin = pos + map.length();
				int end = page.indexOf("\"", begin);
				
				String urlEncodedFmtStreamMap = page.substring(begin, end).replace("\\u0026", "&");
				formats.putAll(parseUrls(urlEncodedFmtStreamMap, algorithm));
			}
		}
		
		return formats;
	}
	
	private static HashMap<String, String> getFormatsFromVideoInformation(String videoId, Context context) throws FileNotFoundException, MalformedURLException, IOException {
		String uri = "http://www.youtube.com/get_video_info?&video_id=" + videoId;
		String page = HttpUtils.get(uri, null, _YtApiConstants.HttpTimeout);
		
		String algorithm = loadAlgorithmFromPreferences(context);
		
		// Parse formats
		HashMap<String, String> formats = new HashMap<String, String>();
		
		String[] mapNames = { "url_encoded_fmt_stream_map", "adaptive_fmts" };
		
		for (String mapName : mapNames) {
			String map = String.format("%s=", mapName);
			int pos = page.indexOf(map);
			if (pos != -1) {
				int begin = pos + map.length();
				int end = page.indexOf("&", begin);
				if (end == -1) {
					end = page.length();
				}
				
				String urlEncodedFmtStreamMap = URLDecoder.decode(page.substring(begin, end), _YtApiConstants.CharacterEncoding);
				formats.putAll(parseUrls(urlEncodedFmtStreamMap, algorithm));
			}
		}
		
		return formats;
	}
	
	private static String loadAlgorithmFromPreferences(Context context) {
		SharedPreferences preferences = AOSUtilsCommon.getDefaultSharedPreferences(context);
		String algorithmPrefName = context.getResources().getString(R.string.aosutils_pref_YoutubeSignatureAlgorithm);
		String algorithmPrefValue = preferences.getString(algorithmPrefName, null);
		String algorithm = algorithmPrefValue == null ? null : TextUtils.split(algorithmPrefValue, Pattern.quote("||"))[1];
		return algorithm;
	}
	
	private static HashMap<String, String> parseUrls(String urlEncodedFmtStreamMap, String algorithm) throws FileNotFoundException, MalformedURLException, IOException {		
		HashMap<String, String> formats = new HashMap<String, String>();
			
		String[] urls = TextUtils.split(urlEncodedFmtStreamMap, ",");
		for (String url : urls) {
			String[] params = TextUtils.split(url, "&");
			
			HashMap<String, String> paramMap = new HashMap<String, String>();
			
			for (String set : params) {
				String[] setParts = TextUtils.split(set, "=");
				String key = setParts[0].replace("u0026", "");
				String value = URLDecoder.decode(setParts[1], _YtApiConstants.CharacterEncoding);
				
				paramMap.put(key, value);
			}
			
			String itag = paramMap.get("itag");
			String feedUrl = paramMap.get("url");
			
			if (paramMap.containsKey("s")) { // Secure link, decode
				try {
					String signature = paramMap.get("s");
					
					if (algorithm != null) {
						feedUrl = YtApiSignature.decode(feedUrl, signature, algorithm);
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			formats.put(itag, feedUrl);
		}
		
		return formats;
	}
}