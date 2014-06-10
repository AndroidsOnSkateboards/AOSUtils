package org.aosutils.android.youtube;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.util.HashMap;

import org.aosutils.net.HttpUtils;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

public class YtApiClientStreams {
	/*
	 * Known formats:
	 * 17 - Video+Audio - 144p
	 * 18 - Video+Audio - 360p
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
	 * 140 - Audio only - Medium
	 * 141 - Audio only - High
	 * 
	 * Found lots of info at: http://users.ohiohills.com/fmacall/YTCRACK.TXT
	 * 
	 */
	
	public enum StreamType {
		VIDEO_AND_AUDIO, AUDIO_ONLY;
	}
	private enum ConnectionType {
		SLOW, MEDIUM, FAST;
	}
	
	public static String findStream(String videoId, Context context, StreamType streamType) throws FileNotFoundException, MalformedURLException, IOException {
		String[] recommendedFormats = getRecommendedFormats(context, streamType);
		return findRecommendedUrl(videoId, recommendedFormats);
	}
	
	// Returns recommended formats, ordered from most recommended to least recommended
	private static String[] getRecommendedFormats(Context context, StreamType streamType) {
		if (streamType == StreamType.AUDIO_ONLY) {
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
				// Device supports Audio-only stream; use "medium" quality audio stream (only one ever tested)
				return new String[] { "140", "18", "17" };
			}
			else {
				return new String[] { "18", "17" };
			}
		}
		else {
			ConnectionType connectionType = getConnectionType(context);
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
	
	private static String findRecommendedUrl(String videoId, String[] recommendedFormats) throws FileNotFoundException, MalformedURLException, IOException {
		HashMap<String, String> urls = getFormatsFromDesktopSite(videoId);
		if (urls.size() == 0) {
			urls = getFormatsFromVideoInformation(videoId);
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
	
	private static HashMap<String, String> getFormatsFromDesktopSite(String videoId) throws FileNotFoundException, MalformedURLException, IOException {
		String uri = "http://www.youtube.com/watch?v=" + videoId;
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");
		String page = HttpUtils.get(uri, headers, YtApiConstants.HttpTimeout);
		
		HashMap<String, String> formats = new HashMap<String, String>();
		
		String[] mapNames = { "url_encoded_fmt_stream_map", "adaptive_fmts" };
		
		for (String mapName : mapNames) {
			String map = String.format("\"%s\": \"", mapName);
			int pos = page.indexOf(map);
			if (pos != -1) {
				int begin = pos + map.length();
				int end = page.indexOf("\"", begin);
				
				String urlEncodedFmtStreamMap = page.substring(begin, end).replace("\\u0026", "&");
				formats.putAll(parseUrls(urlEncodedFmtStreamMap));
			}
		}
		
		return formats;
	}
	
	private static HashMap<String, String> getFormatsFromVideoInformation(String videoId) throws FileNotFoundException, MalformedURLException, IOException {
		String uri = "http://www.youtube.com/get_video_info?&video_id=" + videoId;
		String page = HttpUtils.get(uri, null, YtApiConstants.HttpTimeout);
		
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
				
				String urlEncodedFmtStreamMap = URLDecoder.decode(page.substring(begin, end), YtApiConstants.CharacterEncoding);
				formats.putAll(parseUrls(urlEncodedFmtStreamMap));
			}
		}
		
		return formats;
	}
	
	private static HashMap<String, String> parseUrls(String urlEncodedFmtStreamMap) throws FileNotFoundException, MalformedURLException, IOException {
		HashMap<String, String> formats = new HashMap<String, String>();
			
		String[] urls = TextUtils.split(urlEncodedFmtStreamMap, ",");
		for (String url : urls) {
			String[] params = TextUtils.split(url, "&");
			
			HashMap<String, String> paramMap = new HashMap<String, String>();
			
			for (String set : params) {
				String[] setParts = TextUtils.split(set, "=");
				String key = setParts[0].replace("u0026", "");
				String value = URLDecoder.decode(setParts[1], "UTF-8");
				
				paramMap.put(key, value);
				
				//Log.v("SSA", key + ":" + value);
			}
			
			//Log.v("SSA", paramMap.get("itag") + "," + paramMap.get("quality") + ": ");
			//Log.v("SSA", paramMap.get("url"));
			formats.put(paramMap.get("itag"), paramMap.get("url"));
		}
		
		return formats;
	}
}
