package org.aosutils.net;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Map;

import org.aosutils.IoUtils;

import org.json.JSONException;
import org.json.JSONObject;

public class JsonUtils {
	public static String httpPost(String uri, Map<String, String> headers, JSONObject jsonObject, Integer httpTimeout) throws FileNotFoundException, MalformedURLException, IOException {
		return HttpUtils.post(uri, headers, jsonObject.toString(), httpTimeout);
	}
	
	public static JSONObject parseJson(InputStream inputStream) throws IOException, JSONException {
		return parseJson(IoUtils.getString(inputStream));
	}
	public static JSONObject parseJson(String input) throws JSONException {
		return new JSONObject(input);
	}
	
	public static String toString(JSONObject jsonObject) {
		return jsonObject.toString();
	}
}
