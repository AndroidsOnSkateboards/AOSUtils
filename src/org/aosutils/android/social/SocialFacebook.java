package org.aosutils.android.social;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;

import org.aosutils.android.AOSUtilsCommon;
import org.aosutils.android.R;
import org.aosutils.android.loadingtask.LoadingTask;
import org.aosutils.net.HttpStatusCodeException;
import org.aosutils.net.HttpUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.net.Uri.Builder;

public class SocialFacebook {
	private static final int HTTP_TIMEOUT = 5000;
	private static final String OAUTH_REDIRECT_URI = "https://www.facebook.com/connect/login_success.html";
	private static final String OAUTH_RETURN_CODE = "code";
	
	
	private String appId;
	private String appSecret;
	private int activityRequestCodeForLogin;
	private PendingPost pendingPost;
	
	public SocialFacebook(String appId, String appSecret, int activityRequestCodeForLogin) {
		this.appId = appId;
		this.appSecret = appSecret;
		this.activityRequestCodeForLogin = activityRequestCodeForLogin;
	}
	
	private class PendingPost {
		private String action;
		private Map<String, String> properties;
		
		private PendingPost(String action, Map<String, String> properties) {
			this.action = action;
			this.properties = properties;
		}
	}
	
	public void post(String action, Map<String, String> properties, Activity loadingTaskActivity, Runnable onSuccessfulPost) {
		new PostTask(action, properties, loadingTaskActivity, onSuccessfulPost).execute();
	}
	
	private class PostTask extends LoadingTask<Object, Object, Object> {
		Runnable onSuccessfulPost;
		
		public PostTask(String action, Map<String, String> properties, Activity loadingTaskActivity, Runnable onSuccessfulPost) {
			super(loadingTaskActivity, R.string.dialog_Posting);
			pendingPost = new PendingPost(action, properties);
			this.onSuccessfulPost = onSuccessfulPost;
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}
		
		@Override
		protected Object doInBackground(Object... params) {
			try {
				String facebookApiToken = AOSUtilsCommon.getDefaultSharedPreferences(getActivity()).getString(getActivity().getString(R.string.aosutils_pref_FacebookApiToken), null);
				if (facebookApiToken == null) {
					login(getActivity());
				}
				else {
					post(pendingPost, getActivity());
					pendingPost = null;
					
					if (onSuccessfulPost != null) {
						onSuccessfulPost.run();
						onSuccessfulPost = null;
					}
				}
				
			} catch (IOException e) {
				if (HttpStatusCodeException.isUnauthorized(e)) {
					login(getActivity());
				}
				else {
					alertOnUi(getActivity().getString(R.string.error_Connection));
				}
				
				e.printStackTrace();
			}
			return null;
		}
	}
	
	private void login(Activity activity) {
		Uri uri = new Uri.Builder().scheme("https").authority("www.facebook.com").path("/dialog/oauth")
			.appendQueryParameter("client_id", appId)
			.appendQueryParameter("redirect_uri", OAUTH_REDIRECT_URI)
			.appendQueryParameter("scope", "publish_actions")
			.build();
		
		SocialCommon.oauthLogin(activityRequestCodeForLogin, uri.toString(), new String[] { OAUTH_RETURN_CODE }, activity);
	}
	
	public void onLoginSuccessful(Intent data, Activity activity, Runnable onSuccessfulPostRunnable) {
		String oAuthCode = data.getExtras().getString(SocialFacebook.OAUTH_RETURN_CODE);
		new GetAndStoreAccessTokenTask(oAuthCode, activity, onSuccessfulPostRunnable).execute();
	}
	
	private class GetAndStoreAccessTokenTask extends LoadingTask<Object, Object, Object> {
		String code;
		Runnable onSuccessfulPost;
		
		public GetAndStoreAccessTokenTask(String code, Activity activity, Runnable onSuccessfulPost) {
			super(activity, R.string.dialog_Posting);
			this.code = code;
			this.onSuccessfulPost = onSuccessfulPost;
		}

		@Override
		protected Object doInBackground(Object... params) {
			String result = null;
			
			// Get token
			Uri uri = new Uri.Builder().scheme("https").authority("graph.facebook.com").path("/oauth/access_token")
				.appendQueryParameter("client_id", appId)
				.appendQueryParameter("redirect_uri", OAUTH_REDIRECT_URI)
				.appendQueryParameter("client_secret", appSecret)
				.appendQueryParameter("code", code)
				.build();
			
			try {
				result = HttpUtils.post(uri.toString(), null, null, HTTP_TIMEOUT);
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			return result;
		}
		
		@Override
		protected void onPostExecute(Object result) {
			super.onPostExecute(result);
			
			if (result == null) {
				alertOnUi(getActivity().getString(R.string.error_Connection));
			}
			else {
				String resultStr = (String) result;
				if (resultStr.indexOf("access_token") == -1) {
					alertOnUi(getActivity().getString(R.string.error_Unknown));
				}
				else {
					String accessToken = resultStr.substring(resultStr.indexOf("access_token=") + "access_token=".length());
					if (accessToken.indexOf("&") != -1) {
						accessToken = accessToken.substring(0, accessToken.indexOf("&"));
					}
					
					// Store info
					Editor editor = AOSUtilsCommon.getDefaultSharedPreferences(getActivity()).edit();
					editor.putString(getActivity().getResources().getString(R.string.aosutils_pref_FacebookApiToken), accessToken);
					editor.commit();
					
					if (pendingPost != null) {
						post(pendingPost.action, pendingPost.properties, getActivity(), onSuccessfulPost);
					}
				}
			}
		}
	}
	
	private Long post(PendingPost pendingPost, Context context) throws MalformedURLException, IOException {
		String accessToken = AOSUtilsCommon.getDefaultSharedPreferences(context).getString(context.getString(R.string.aosutils_pref_FacebookApiToken), null);
		
		Builder uriBuilder = new Uri.Builder().scheme("https").authority("graph.facebook.com").path("/me/" + pendingPost.action)
				.appendQueryParameter("method", "POST")
				.appendQueryParameter("access_token", accessToken);
		
		for (String key : pendingPost.properties.keySet()) {
			String value = pendingPost.properties.get(key);
			uriBuilder.appendQueryParameter(key, value);
		}
		
		Uri uri = uriBuilder.build();
		
		String result = HttpUtils.post(uri.toString(), null, null, HTTP_TIMEOUT);
		Long id = null;
		try {
			id = new JSONObject(result).getLong("id");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return id;
	}
}
