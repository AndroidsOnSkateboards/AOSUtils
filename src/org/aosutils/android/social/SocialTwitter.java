package org.aosutils.android.social;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.aosutils.android.AOSUtilsCommon;
import org.aosutils.android.LoadingTask;
import org.aosutils.android.R;
import org.aosutils.net.HttpUtils;
import org.aosutils.net.HttpUtils.HTTPUnauthorizedException;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.backported.Base64;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.text.TextUtils;

public class SocialTwitter {
	private class TokenDoesntMatchException extends Exception {
		public TokenDoesntMatchException() {
			super("Token doesn't match original request");
		}

		private static final long serialVersionUID = -626591933810532515L;
	}
	
	private static final int HTTP_TIMEOUT = 5000;
	private static final String CHARACTER_ENCODING = "UTF-8";
	
	private static final String OAUTH_RETURN_REQUEST_TOKEN = "oauth_token";
	private static final String OAUTH_RETURN_REQUEST_VERIFIER = "oauth_verifier";
	
	private static final String OAUTH_AUTHORIZE_URL = "https://api.twitter.com/oauth/authorize"; 
	private static final String OAUTH_REQUEST_TOKEN_URL = "https://api.twitter.com/oauth/request_token";
	private static final String OAUTH_ACCESS_TOKEN_URL = "https://api.twitter.com/oauth/access_token";
	private static final String OAUTH_TWEET_URL = "https://api.twitter.com/1.1/statuses/update.json";
	
	
	private String apiKey;
	private String apiSecret;
	private int activityRequestCodeForLogin;
	
	private String pendingTweet;
	private String pendingRequestToken;
	
	public SocialTwitter(String apiKey, String apiSecret, int activityRequestCodeForLogin) {
		this.apiKey = apiKey;
		this.apiSecret = apiSecret;
		this.activityRequestCodeForLogin = activityRequestCodeForLogin;
	}
	
	private void getRequestToken(Activity activity) {
		new GetRequestTokenTask(activity).execute();
	}
	private class GetRequestTokenTask extends AuthTask<Object, Object, Object> {
		public GetRequestTokenTask(Activity activity) {
			super(activity);
		}
		
		@Override
		protected String url() {
			return OAUTH_REQUEST_TOKEN_URL;
		}
		
		@Override
		protected void addToAuthHeader(TreeMap<String, String> parameters) throws UnsupportedEncodingException {
			String oauth_callback = OAUTH_AUTHORIZE_URL;
			parameters.put("oauth_callback", oauth_callback);
		}
		
		@Override
		protected void addToBody(TreeMap<String, String> parameters) { }
		
		@Override
		protected void onPostSuccess(Map<String, String> response) {
			//Log.v("SS", "token_secret: " + response.get("oauth_token_secret"));
			
			if (! "true".equals(response.get("oauth_callback_confirmed"))) {
				AOSUtilsCommon.alert(null, getActivity().getResources().getString(R.string.error_Unknown), getActivity());
			}
			else {
				pendingRequestToken = response.get("oauth_token");
				String url = OAUTH_AUTHORIZE_URL + "?oauth_token=" + pendingRequestToken;
				String[] paramsToReturn = { OAUTH_RETURN_REQUEST_TOKEN, OAUTH_RETURN_REQUEST_VERIFIER };
				SocialCommon.oauthLogin(activityRequestCodeForLogin, url, paramsToReturn, getActivity());
			}
		}
	}
	
	public void onLoginSuccessful(Intent data, Activity activity, Runnable onSuccessfulPostRunnable) {
		String oAuthToken = data.getExtras().getString(SocialTwitter.OAUTH_RETURN_REQUEST_TOKEN);
		String oAuthVerifier = data.getExtras().getString(SocialTwitter.OAUTH_RETURN_REQUEST_VERIFIER);
		
		new GetAccessTokenTask(oAuthToken, oAuthVerifier, activity, onSuccessfulPostRunnable).execute();
	}
	
	private class GetAccessTokenTask extends AuthTask<Object, Object, Object> {
		String requestToken;
		String requestVerifier;
		Runnable onSuccessfulTweetRunnable;
		
		public GetAccessTokenTask(String requestToken, String requestVerifier, Activity activity, Runnable onSuccessfulPost) {
			super(activity);
			this.requestToken = requestToken;
			this.requestVerifier = requestVerifier;
			this.onSuccessfulTweetRunnable = onSuccessfulPost;
		}

		@Override
		protected String url() {
			return OAUTH_ACCESS_TOKEN_URL;
		}
		
		@Override
		protected void addToAuthHeader(TreeMap<String, String> parameters) throws UnsupportedEncodingException, TokenDoesntMatchException {
			if (!requestToken.equals(pendingRequestToken)) {
				throw new TokenDoesntMatchException();
			}
			
			parameters.put("oauth_token", requestToken);
			parameters.put("oauth_verifier", requestVerifier);
		}
		
		@Override
		protected void addToBody(TreeMap<String, String> parameters) { }

		@Override
		protected void onPostSuccess(Map<String, String> response) {
			pendingRequestToken = null;
			
			String accessToken = response.get("oauth_token");
			String accessTokenSecret = response.get("oauth_token_secret");
			
			// Store info
			Editor editor = AOSUtilsCommon.getDefaultSharedPreferences(getActivity()).edit();
			editor.putString(getActivity().getResources().getString(R.string.aosutils_pref_TwitterApiToken), accessToken);
			editor.putString(getActivity().getResources().getString(R.string.aosutils_pref_TwitterApiTokenSecret), accessTokenSecret);
			editor.commit();
			
			if (pendingTweet != null) {
				tweet(pendingTweet, getActivity(), onSuccessfulTweetRunnable);
			}
		}
	}
	
	public void tweet(String message, Activity activity, Runnable onSuccessfulTweetRunnable) {
		new TweetTask(message, activity, onSuccessfulTweetRunnable).execute();
	}
	private class TweetTask extends AuthenticatedTask<Object, Object, Object> {
		String message;
		Runnable onSuccessfulTweetRunnable;
		
		public TweetTask(String message, Activity activity, Runnable onSuccessfulTweetRunnable) {
			super(apiKey, apiSecret, activity);
			this.message = message;
			this.onSuccessfulTweetRunnable = onSuccessfulTweetRunnable;
		}
		
		@Override
		protected void onPreExecute() {
			pendingTweet = this.message;
			
			String apiToken = AOSUtilsCommon.getDefaultSharedPreferences(getActivity()).getString(getActivity().getString(R.string.aosutils_pref_TwitterApiToken), null);
			String apiTokenSecret = AOSUtilsCommon.getDefaultSharedPreferences(getActivity()).getString(getActivity().getString(R.string.aosutils_pref_TwitterApiTokenSecret), null);
			
			if (apiToken == null || apiTokenSecret == null) {
				this.cancel(false);
				getRequestToken(getActivity());
			}
			else {
				// Show loading dialog
				super.onPreExecute();
			}
		}

		@Override
		protected String url() {
			return OAUTH_TWEET_URL;
		}
		
		@Override
		protected void addToBody(TreeMap<String, String> parameters) { 
			parameters.put("status", message);
		}
		
		@Override
		protected void onPostSuccess(JSONObject jsonObject) {
			pendingTweet = null;
			
			onSuccessfulTweetRunnable.run();
			onSuccessfulTweetRunnable = null;
		}

		@Override
		protected void onPostFail(Exception e) {
			if (e instanceof HTTPUnauthorizedException) {
				// Get a new access token
				getRequestToken(getActivity());
			}
			else {
				// Show alert
				super.onPostFail(e);
			}
		}
	}
	
	private abstract class AuthenticatedTask<A, B, C> extends TwitterTask<Object, Object, Object> {
		public AuthenticatedTask(String apiKey, String secret, Activity activity) {
			super(activity);
		}
		
		@Override
		protected void addToAuthHeader(TreeMap<String, String> parameters) throws UnsupportedEncodingException {
			parameters.put("oauth_token", AOSUtilsCommon.getDefaultSharedPreferences(getActivity()).getString(getActivity().getString(R.string.aosutils_pref_TwitterApiToken), null));
		}
		
		@Override
		protected String getSigningKey() {
			String oauthTokenSecret = AOSUtilsCommon.getDefaultSharedPreferences(getActivity()).getString(getActivity().getString(R.string.aosutils_pref_TwitterApiTokenSecret), null); 
			return super.getSigningKey() + oauthTokenSecret;
		}
		
		protected void onPostSuccess(String responseBody) {
			try {
				onPostSuccess(new JSONObject(responseBody));
			} catch (JSONException e) {
				AOSUtilsCommon.alert(null, getActivity().getResources().getString(R.string.error_Unknown), getActivity());
				e.printStackTrace();
			}
		}
		
		protected int unknownErrorResourceId() {
			return R.string.error_Posting;
		}

		protected abstract void onPostSuccess(JSONObject jsonObject);
	}
	
	private abstract class AuthTask<A, B, C> extends TwitterTask<Object, Object, Object> {
		public AuthTask(Activity activity) {
			super(activity);
		}
		
		protected void onPostSuccess(String responseBody) {
			TreeMap<String, String> response = new TreeMap<String, String>();
			// Create a URI in order to parse out the response
			String url = url();
			String uri = url.indexOf("?") == -1 ? url : url.substring(0, url.indexOf("?"));
			Uri responseUri = Uri.parse(uri + "?" + responseBody);
			
			for (String pair : responseUri.getQuery().split("&")) {
				response.put(pair.substring(0, pair.indexOf("=")).trim(), pair.substring(pair.indexOf("=")+1).trim());
			}
			
			onPostSuccess(response);
		}
		
		protected int unknownErrorResourceId() {
			return R.string.error_Posting_DateTime;
		}
		
		protected abstract void onPostSuccess(Map<String, String> response);
	}
	
	private abstract class TwitterTask<A, B, C> extends LoadingTask<Object, Object, Object> {
		String response;
		
		public TwitterTask(Activity activity) {
			super(activity, R.string.dialog_Loading);
		}

		@Override
		protected Object doInBackground(Object... params) {
			try {
				TreeMap<String, String> authHeaderParams = new TreeMap<String, String>();
				addToAuthHeader(authHeaderParams);
				
				TreeMap<String, String> bodyParams = new TreeMap<String, String>();
				addToBody(bodyParams);
				
				response = postToApi(url(), authHeaderParams, bodyParams, getSigningKey());
			}
			catch (Exception e) {
				return e;
			}
			
			return null;
		}
		
		protected String getSigningKey() {
			return apiSecret + "&";
		}
		
		@Override
		protected void onPostExecute(Object exception) {
			super.onPostExecute(exception);
			
			if (exception != null && exception instanceof Exception) {
				Exception e = (Exception) exception;
				e.printStackTrace();
				onPostFail(e);
			}
			else {
				onPostSuccess(response);
			}
		}
		
		protected void onPostFail(Exception e) {
			int messageResourceId = unknownErrorResourceId();
			if (e instanceof IOException && !(e instanceof HTTPUnauthorizedException)) {
				messageResourceId = R.string.error_Connection;
			}
			
			AOSUtilsCommon.alert(null, getActivity().getResources().getString(messageResourceId), getActivity());
		}
		
		protected abstract String url();
		protected abstract void addToAuthHeader(TreeMap<String, String> parameters) throws UnsupportedEncodingException, TokenDoesntMatchException;
		protected abstract void addToBody(TreeMap<String, String> parameters);
		protected abstract void onPostSuccess(String response);
		protected abstract int unknownErrorResourceId();
	}
	
	
	private String postToApi(String url, Map<String, String> authHeaderParams, Map<String, String> bodyParams, String signingKey) throws HTTPUnauthorizedException, IOException, Exception {
		String uri = url.contains("?") ? url.substring(0, url.indexOf("?")) : url;
		String oauth_nonce = UUID.randomUUID().toString().replace("-", "");
		String oauth_signature_method = "HMAC-SHA1";
		String oauth_timestamp = Long.valueOf(System.currentTimeMillis() / 1000).toString();
		String oauth_version = "1.0";
		String httpMethod = "POST";
		
		// Must be sorted by key, alphabetically (TreeMap does this)
		TreeMap<String, String> authHeaderParamsToPost = new TreeMap<String, String>();
		authHeaderParamsToPost.putAll(authHeaderParams);
		authHeaderParamsToPost.put("oauth_consumer_key", apiKey);
		authHeaderParamsToPost.put("oauth_nonce", oauth_nonce);
		authHeaderParamsToPost.put("oauth_signature_method", oauth_signature_method);
		authHeaderParamsToPost.put("oauth_timestamp", oauth_timestamp);
		authHeaderParamsToPost.put("oauth_version", oauth_version);
		authHeaderParamsToPost.put("oauth_signature", generateSignature(uri, getParamsToSign(url, authHeaderParamsToPost, bodyParams), httpMethod, signingKey, oauth_signature_method));
		
		ArrayList<String> sets = new ArrayList<String>();
		for (String parameter : authHeaderParamsToPost.keySet()) {
			sets.add(parameter + "=\"" + URLEncoder.encode(authHeaderParamsToPost.get(parameter), CHARACTER_ENCODING) + "\"");
		}
		String authorization_header_string = "OAuth " + TextUtils.join(",", sets);
		
		sets.clear();
		for (String parameter : bodyParams.keySet()) {
			sets.add(parameter + "=" + URLEncoder.encode(bodyParams.get(parameter), CHARACTER_ENCODING));
		}
		String postData = TextUtils.join("&", sets);
		
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put("Authorization", authorization_header_string);
		headers.put("Content-Type", "application/x-www-form-urlencoded; charset=" + CHARACTER_ENCODING);
		headers.put("Content-Length", Integer.toString(postData.length()));
		
		String responseBody = HttpUtils.post(url, headers, postData, HTTP_TIMEOUT);
		
		return responseBody;
	}
	
	private static TreeMap<String, String> getParamsToSign(String url, Map<String, String> authHeaderParams, Map<String, String> bodyParams) {
		// Assemble full list of parameters to be signed
		TreeMap<String, String> paramsToSign = new TreeMap<String, String>();
		paramsToSign.putAll(authHeaderParams);
		paramsToSign.putAll(bodyParams);
		
		if (url.indexOf("?") != -1) {
			String query = url.substring(url.indexOf("?"));
			for (String part : query.split("&")) {
				paramsToSign.put(part.substring(0, part.indexOf("=")), part.substring(part.indexOf("=")));
			}
		}
				
		return paramsToSign;
	}
	
	private static String generateSignature(String uri, TreeMap<String, String> parameters, String httpMethod, String signingKey, String algorithm) throws UnsupportedEncodingException, GeneralSecurityException {
		ArrayList<String> sets = new ArrayList<String>();
		for (String parameter : parameters.keySet()) {
			String encodedKey = percentEncode(parameter);
			String encodedValue = percentEncode(parameters.get(parameter));
			
			sets.add(encodedKey + "=" + encodedValue);
		}
		
		
		String encodedUri = URLEncoder.encode(uri, CHARACTER_ENCODING);
		String encodedQuery = URLEncoder.encode(TextUtils.join("&", sets), CHARACTER_ENCODING);
		String signature_base_string = httpMethod + "&" + encodedUri + "&" + encodedQuery;
		
		String oauth_signature = base64encode(signature_base_string, signingKey, algorithm);
		
		return oauth_signature;
	}
	
	private static String percentEncode(String s) {
        if (s == null) {
        	return "";
        }
        try {
        	return URLEncoder.encode(s, CHARACTER_ENCODING)
    			// OAuth encodes some characters differently:
    			.replace("+", "%20").replace("*", "%2A")
    			.replace("%7E", "~");
            // This could be done faster with more hand-crafted code.
        } catch (UnsupportedEncodingException wow) {
            throw new RuntimeException(wow.getMessage(), wow);
        }
    }
	
	private static String base64encode(String baseString, String keyString, String algorithm) throws GeneralSecurityException, UnsupportedEncodingException {
		SecretKey secretKey = null;
		byte[] keyBytes = keyString.getBytes();
		secretKey = new SecretKeySpec(keyBytes, algorithm);
		Mac mac = Mac.getInstance(algorithm);
		mac.init(secretKey);
		byte[] text = baseString.getBytes();
		return new String(Base64.encode(mac.doFinal(text), Base64.DEFAULT)).trim();
	}
}
