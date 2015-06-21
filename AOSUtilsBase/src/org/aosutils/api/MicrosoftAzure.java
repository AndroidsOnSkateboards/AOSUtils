package org.aosutils.api;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.aosutils.StringUtils;
import org.aosutils.net.HttpUtils;
import org.json.JSONException;
import org.json.JSONObject;

/*
 * Thread safe singleton wrapper for Bing oauth token
 * Originally from Phyous: https://gist.github.com/phyous/5079099 , with modifications made
*/

public class MicrosoftAzure {
	private static final int httpTimeout = 10000;
	
	private static final String oauthServerName = "https://datamarket.accesscontrol.windows.net";
	private static final String oauthEndpoint = "/v2/OAuth2-13/";
	private static final String oauthGrantType = "client_credentials";
	
	private static final String translateScope = "api.microsofttranslator.com";
	private static final String translateEndpoint = "/V2/Ajax.svc/Translate";
	
	private static HashMap<String, String> _oauthTokens = new HashMap<String, String>();
	
	public static String translate(String text, String from, String to, String clientId, String clientSecret) throws IOException, JSONException {
		String oAuthToken = getOauthToken(clientId, clientSecret, translateScope);
		
		if (oAuthToken == null) {
			return null;
		}
		else {
			HashMap<String, String> args = new LinkedHashMap<String, String>();
			args.put("text", text);
			args.put("from", (from == null ? "" : from));
			args.put("to", (to == null ? "en" : to));
			args.put("appId", "Bearer " + oAuthToken);
			
			String argsStr = StringUtils.joinUrlArgs(args);
			String url = "https://" + translateScope + translateEndpoint + "?" + argsStr;
			
			String translated = HttpUtils.get(url, null, httpTimeout).trim();
			translated = StringUtils.trim(translated, "\"");
			return translated;
		}
	}
	
	/**
	* Thread safe lazy initializer for oauth token
	 * @throws JSONException 
	 * @throws IOException 
	*/
	private static String getOauthToken(String clientId, String clientSecret, String scope) throws IOException, JSONException {
		String oauthToken = _oauthTokens.get(clientId + scope);
		
		if (oauthToken == null) {
			synchronized (MicrosoftAzure.class) {
				if (oauthToken == null) {
					// Create request
					HashMap<String, String> args = new LinkedHashMap<String, String>();
					args.put("client_id", clientId);
					args.put("client_secret", clientSecret);
					args.put("scope", "http://" + scope);
					args.put("grant_type", oauthGrantType);
					
					String url = oauthServerName + oauthEndpoint;
					String postData = StringUtils.joinUrlArgs(args);
					
					String response = HttpUtils.post(url, null, postData, httpTimeout);
					
					// Parse response
					oauthToken = (new JSONObject(response)).getString("access_token");
					_oauthTokens.put(clientId + scope, oauthToken);
				}
			}
		}
		
		return oauthToken;
	}
}