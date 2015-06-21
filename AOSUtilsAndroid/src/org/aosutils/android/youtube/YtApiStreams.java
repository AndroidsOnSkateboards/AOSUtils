package org.aosutils.android.youtube;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.aosutils.android.AOSUtilsCommon;
import org.aosutils.android.R;
import org.aosutils.net.HttpStatusCodeException;
import org.aosutils.net.HttpUtils;

import android.content.Context;
import android.content.SharedPreferences;
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
		/* - VIDEO_ONLY streams don't seem to work; Tested on Android 2.3, Android 4.4; Also commented out below.. */
		VIDEO_AND_AUDIO, VIDEO_AND_AUDIO_LOW_QUALITY, AUDIO_ONLY_OR_VIDEO, AUDIO_ONLY_OR_VIDEO_LOW_QUALITY; //, VIDEO_ONLY;
	}
	public enum ConnectionType {
		UNUSABLE, SLOW, MEDIUM, FAST;
	}
	
	public static class StreamResult {
		public String streamUrl;
		public Exception desktopSiteException;
	}
	
	public static StreamResult findStream(String videoId, Context context, StreamType streamType) throws FileNotFoundException, MalformedURLException, IOException {
		String[] recommendedFormats = getRecommendedFormats(context, streamType);
		StreamResult streamResult = findRecommendedUrl(videoId, recommendedFormats, context);
		return streamResult;
	}
	
	// Returns recommended formats, ordered from most recommended to least recommended
	private static String[] getRecommendedFormats(Context context, StreamType streamType) {
		ConnectionType connectionType = getConnectionType(context);
		
		if (streamType == StreamType.AUDIO_ONLY_OR_VIDEO) {
			if (deviceSupportsAudioOnlyStreams()) {
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
		else if (streamType == StreamType.AUDIO_ONLY_OR_VIDEO_LOW_QUALITY) {
			if (deviceSupportsAudioOnlyStreams()) {
				// Device supports Audio-only stream; use "medium" quality audio stream (only one ever tested)
				return new String[] { "140", "17" };
			}
			else {
				// Only video streams supported
				return new String[] { "17" };
			}
		}
		/* - VIDEO_ONLY streams don't seem to work; Tested on Android 2.3, Android 4.4
		else if (streamType == StreamType.VIDEO_ONLY) {
			if (connectionType == ConnectionType.FAST) {
				return new String[] { "136", "135", "134", "133" };
			}
			else if (connectionType == ConnectionType.MEDIUM) {
				return new String[] { "134", "133", "135" };
			}
			else {
				return new String[] { "133", "134", "135" };
			}
		}
		*/
		else if (streamType == StreamType.VIDEO_AND_AUDIO_LOW_QUALITY) {
			return new String[] { "17" };
		}
		else { // Default: Video+Audio
			if (connectionType == ConnectionType.FAST) {
				return new String[] { /* untested: "22", */ "18", "17" };
			}
			else if (connectionType == ConnectionType.MEDIUM) {
				return new String[] { "18", "17" };
			}
			else {
				return new String[] { "17" };
			}
		}
	}
	
	public static boolean deviceSupportsAudioOnlyStreams() {
		return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2;
	}
	
	public static ConnectionType getConnectionType(Context context) {
		// For a full list of networks, see class: android.telephony.TelephonyManager
		
		TelephonyManager lTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		
		// If we have a fast connection (WiFi or 3G), then use a medium quality YouTube video
		if (
			(AOSUtilsCommon.isOnWifi(context)) ||
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
					lTelephonyManager.getNetworkType() != 4 /* CDMA#4, should be considered UNUSABLE */ &&
					lTelephonyManager.getNetworkType() != 11 /* iDEN, should be considered UNUSABLE */
				)
			) {
			return ConnectionType.MEDIUM;
		}
		else if (
				lTelephonyManager.getDataState() == TelephonyManager.DATA_CONNECTED &&
					(
						lTelephonyManager.getNetworkType() >= 2 /* EDGE/CDMA#2 */
					)
				) {
				return ConnectionType.SLOW;
		}
		else {
			/*
			 * 0 - NONE/UNKNOWN
			 * 1 - GSM/GPRS
			 * 4 - CDMA#4
			 * 11 - iDEN
			 * 
			 */
			return ConnectionType.UNUSABLE;
		}
	}
	
	private static StreamResult findRecommendedUrl(String videoId, String[] recommendedFormats, Context context) throws FileNotFoundException, MalformedURLException, IOException {
		StreamResult streamResult = new StreamResult();
		
		HashMap<String, String> urls = new HashMap<String, String>();
		
		try {
			urls = getFormatsFromDesktopSite(videoId, context);
		}
		catch (Exception e) {
			streamResult.desktopSiteException = e;
		}
	
		if (urls.size() == 0) {			
			// Fallback
			urls = getFormatsFromVideoInformation(videoId, context);
		}
		
		for (String recommendedFormat : recommendedFormats) {
			if (urls.containsKey(recommendedFormat)) {
				streamResult.streamUrl = urls.get(recommendedFormat);
				break;
			}
		}
		
		// Recommended format wasn't found, look for any format
		if (streamResult.streamUrl == null && urls.size() > 0) {
			// Generally sorted by highest format first
			streamResult.streamUrl = urls.get(0);
		}
		
		// Finally, give up
		return streamResult;
	}
	
	// HTTP rarely works; Sometimes it takes a few retries. YouTube is shutting it down..
	// HTTPS is recommended, although Android 2.2 and below's MediaPlayer can't stream HTTPS URL's. 
	public static String getDesktopSite(String videoId, boolean useHttpsRecommended) throws FileNotFoundException, MalformedURLException, IOException {
		String protocol = useHttpsRecommended ? "https" : "http";
		String url = protocol + "://www.youtube.com/watch?v=" + videoId;
		HashMap<String, String> headers = new HashMap<String, String>();
		// -- With this header, even on an HTTP->HTTPS redirect with a "Referer" header, the streams would still come back as HTTPS, so leave this out
		//headers.put("User-Agent", AOSConstants.USER_AGENT_DESKTOP);
		
		// Android < 2.2 fails on YouTube's SSL cert, so force them to always trust it (we anyways aren't sending any secure information)
		boolean forceTrustSSLCert = android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.ECLAIR_MR1;
		
		int tries = 6;
		IOException exception = null;
		
		// Retries were put in place for the HTTP site. It's not so relevant when accessing the HTTPS site because that should work the first time
		for (int i=0; i<tries; i++) {
			try {
				String response = HttpUtils.request(url, headers, null, _YtApiConstants.HTTP_TIMEOUT, null, forceTrustSSLCert);
				return response;
			}
			catch (IOException e) {
				exception = e;
				
				if (e instanceof HttpStatusCodeException) {
					int statusCode = ((HttpStatusCodeException) e).getStatusCode();
					
					if (statusCode == 301 || statusCode == -1) { // YouTube is trying to redirect to HTTPS
						// Some older devices get HTTP status code -1, newer devices get 301 
						// Retry..
						
						if (!headers.containsKey("Referer") && url.contains("http://")) {
							// Redirect to HTTPS with an HTTP "Referer" header, so the resulting streams with be HTTP streams
							headers.put("Referer", url);
							url = url.replace("http://", "https://");
						}
						
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
						
					}
					else {
						e.printStackTrace();
						break;
					}
				}
				else {
					e.printStackTrace();
					break;
				}
			}
		}
		
		
		throw exception;
	}
	
	private static HashMap<String, String> getFormatsFromDesktopSite(String videoId, Context context) throws FileNotFoundException, MalformedURLException, IOException {
		String page;
		if (mediaPlayerSupportsHttps()) {
			page = getDesktopSite(videoId, true);
		}
		else {
			// Android's MediaPlayer doesn't support HTTPS... First try HTTP, then fall back to HTTPS
			try {
				page = getDesktopSite(videoId, false);
			}
			catch (Exception e) {
				page = getDesktopSite(videoId, true);
			}
		}
		page = page.replace("\\\"", "\"");
		
		// This page contains algorithm info, so check it against the current used algorithm and update if necessary
		String algorithm = getOrUpdateAlgorithm(page, context);
		
		// Parse formats
		HashMap<String, String> formats = new HashMap<String, String>();
		
		String[] mapNames = { "url_encoded_fmt_stream_map", "adaptive_fmts" };
		
		for (String mapName : mapNames) {
			String map = String.format("\"%s\":\"", mapName);
			int pos = page.indexOf(map);
			if (pos == -1) {
				map = String.format("\"%s\": \"", mapName);
				pos = page.indexOf(map);
			}
			
			if (pos != -1) {
				int begin = pos + map.length();
				int end = page.indexOf("\"", begin);
				
				String urlEncodedFmtStreamMap = page.substring(begin, end).replace("\\\\u0026", "&").replace("\\u0026", "&");
				
				formats.putAll(parseUrls(urlEncodedFmtStreamMap, algorithm));
			}
		}
		
		return formats;
	}
	
	private static HashMap<String, String> getFormatsFromVideoInformation(String videoId, Context context) throws FileNotFoundException, MalformedURLException, IOException {
		String uri = "http://www.youtube.com/get_video_info?video_id=" + videoId + "&el=vevo";
		String page = HttpUtils.get(uri, null, _YtApiConstants.HTTP_TIMEOUT);
		
		// This page doesn't contain algorithm info, so load it from preferences
		AlgorithmPreference algorithmPreference = AlgorithmPreference.get(context);
		String algorithm = algorithmPreference == null ? null : algorithmPreference.algorithm;
		
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
				
				String urlEncodedFmtStreamMap = URLDecoder.decode(page.substring(begin, end), _YtApiConstants.CHARACTER_ENCODING);
				HashMap<String, String> newFormats = parseUrls(urlEncodedFmtStreamMap, algorithm);
				formats.putAll(newFormats);
			}
		}
		
		return formats;
	}
	
	public static boolean mediaPlayerSupportsHttps() { // Android 3.1 or greater
		return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB_MR1;
	}
	
	private static String getOrUpdateAlgorithm(String youtubePageSource, Context context) {
		String algorithm = null;
		
		// Identify signature algorithm version
		String playerVersion = YtApiSignature.getCurrentVersion(youtubePageSource);
		
		if (playerVersion != null) {
			AlgorithmPreference algorithmPreference = AlgorithmPreference.get(context);
			
			if (algorithmPreference != null && algorithmPreference.version.equals(playerVersion)) {
				// Algorithm up to date
				algorithm = algorithmPreference.algorithm;
			}
			else {
				// Algorithm needs update
				try {
					algorithm = YtApiSignature.requestCurrentAlgorithm(youtubePageSource);
					AlgorithmPreference.set(playerVersion, algorithm, context);
				}
				catch (IOException e) {
					
				}
			}
		}
		
		return algorithm;
	}
	
	private static class AlgorithmPreference {
		String version;
		String algorithm;
		
		private static AlgorithmPreference get(Context context) {
			SharedPreferences preferences = AOSUtilsCommon.getDefaultSharedPreferences(context);
			String algorithmPrefName = context.getResources().getString(R.string.aosutils_pref_YoutubeSignatureAlgorithm);
			String algorithmPrefValue = preferences.getString(algorithmPrefName, null);
			
			if (algorithmPrefValue == null) {
				return null;
			}
			else {
				String[] prefValueParts = TextUtils.split(algorithmPrefValue, Pattern.quote("||"));
						
				AlgorithmPreference algorithmPreference = new AlgorithmPreference();
				algorithmPreference.version = prefValueParts[0];
				algorithmPreference.algorithm = prefValueParts[1];
				return algorithmPreference;
			}
		}
		
		private static void set(String version, String algorithm, Context context) {
			SharedPreferences preferences = AOSUtilsCommon.getDefaultSharedPreferences(context);
			String algorithmPrefName = context.getResources().getString(R.string.aosutils_pref_YoutubeSignatureAlgorithm);
			
			String prefValue = version + "||" + algorithm;
			preferences.edit().putString(algorithmPrefName, prefValue).commit();
		}
	}
	
	private static HashMap<String, String> parseUrls(String urlEncodedFmtStreamMap, String algorithm) throws FileNotFoundException, MalformedURLException, IOException {
		HashMap<String, String> formats = new HashMap<String, String>();
			
		String[] urls = TextUtils.split(urlEncodedFmtStreamMap, ",");
		for (String url : urls) {
			String[] params = TextUtils.split(url, "&");
			
			TreeMap<String, String> paramMap = new TreeMap<String, String>();
			
			for (String set : params) {
				String[] setParts = TextUtils.split(set, "=");
				String key = setParts[0].replace("u0026", "");
				String value = URLDecoder.decode(setParts[1], _YtApiConstants.CHARACTER_ENCODING);
				
				paramMap.put(key, value);
			}
			
			String itag = paramMap.get("itag");
			String feedUrl = paramMap.get("url");
			
			if (feedUrl != null) {
				if (!paramMap.containsKey("s")) { // Standard link
					formats.put(itag, feedUrl);
				}
				else { // Secure link, decode
					String signature = paramMap.get("s");
					
					if (algorithm != null) {
						String fixedSignature = YtApiSignature.decode(signature, algorithm);
						feedUrl = feedUrl + "&signature=" + fixedSignature;
						
						// Only store the format if the signature has been successfully decoded
						formats.put(itag, feedUrl);
					}
				}
			}
		}
		
		return formats;
	}
}
