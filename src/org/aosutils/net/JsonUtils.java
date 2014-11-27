package org.aosutils.net;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Map;

import org.aosutils.IoUtils;
import org.json.JSONArray;
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
	
	public static JSONArray toJsonArray(Collection<?> object) throws JSONException {
		return (JSONArray) toJsonType(object);
	}
	public static JSONObject toJsonObject(Map<String, ?> object) throws JSONException {
		return (JSONObject) toJsonType(object);
	}
	private static Object toJsonType(Object object) throws JSONException {
		if (object instanceof JSONObject || object instanceof JSONArray) {
			throw new JSONException("Trying to build JSONObject/Array out of existing JSONObject/Array");
		}
		else if (object instanceof Collection) {
			JSONArray jsonArray = new JSONArray();
			for (Object item : (Collection<?>) object) {
				jsonArray.put(toJsonType(item));
			}
			return jsonArray;
		}
		else if (object instanceof Map) {
			JSONObject jsonObject = new JSONObject();
			for (Object key : ((Map<?, ?>) object).keySet()) {
				if (!(key instanceof String)) {
					throw new JSONException("All Maps must have Strings as keys.");
				}
				else {
					String name = (String) key;
					Object value = ((Map<?, ?>) object).get(key);
					jsonObject.put(name, toJsonType(value));
				}
			}
			return jsonObject;
		}
		else return object.toString();
	}
}
