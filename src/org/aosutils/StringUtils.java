package org.aosutils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StringUtils {
	public static String join(Collection<String> parts, String delimiter) {
		return join(parts, delimiter, "");
	}
	
	public static String join(Collection<String> parts, String delimiter, String encapsulation) {
		String joined = null;
		
		if (parts != null) {
			joined = "";
			for (String part : parts) {
				joined += encapsulation + part + encapsulation + delimiter;
			}
			
			if (joined.length() > 0) {
				joined = joined.substring(0, joined.length()-delimiter.length());
			}
		}
		
		return joined;
	}
	
	public static String joinUrlArgs(Map<String, String> urlArgs) {
		Map<String, String> urlEncodedArgs = new LinkedHashMap<String, String>(); 
		for (String key : urlArgs.keySet()) {
			String value = urlArgs.get(key);
			if (value == null) {
				value = "";
			}
			
			try {
				key = URLEncoder.encode(key, AOSConstants.CHARACTER_ENCODING);
				value = URLEncoder.encode(value, AOSConstants.CHARACTER_ENCODING);
			}
			catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			
			urlEncodedArgs.put(key, value);
		}
		
		return joinAndUnmap(urlEncodedArgs, "=", "&");
	}
	
	public static String joinAndUnmap(Map<String, String> map, String keyValueDelimiter, String itemDelimiter) {
		ArrayList<String> items = new ArrayList<String>();
		for (String key : map.keySet()) {
			String value = map.get(key);
			items.add(key + keyValueDelimiter + value);
		}
		return join(items, itemDelimiter);
	}
	
	public static List<String> split(String inputString, String delimiter) {
		ArrayList<String> parts = new ArrayList<String>();
		
		while (inputString.indexOf(delimiter) != -1) {
			parts.add(inputString.substring(0, inputString.indexOf(delimiter)));
			inputString = inputString.substring(inputString.indexOf(delimiter)+1);
		}
		if (inputString.length() > 0) {
			parts.add(inputString);
		}
		
		return parts;
	}
	
	public static LinkedHashMap<String, String> splitAndMap(String inputString, String splitDelimiter, String mapDelimiter) {
		LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
		
		for (String part : split(inputString, splitDelimiter)) {
			String key = part.substring(0, part.indexOf(mapDelimiter));
			String value = part.substring(part.indexOf(mapDelimiter)+1);
			map.put(key, value);
		}
		
		return map;
	}
	
	public static LinkedHashMap<String, String> parseUrlArgs(String url) {
		LinkedHashMap<String, String> urlArgs = splitAndMap(url.substring(url.indexOf("?")+1), "&", "=");
		LinkedHashMap<String, String> urlDecodedArgs = new LinkedHashMap<String, String>();
		
		for (String key : urlArgs.keySet()) {
			String value = urlArgs.get(key);
			
			try {
				key = URLDecoder.decode(key, AOSConstants.CHARACTER_ENCODING);
				value = URLDecoder.decode(value, AOSConstants.CHARACTER_ENCODING);
			}
			catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			
			urlDecodedArgs.put(key, value);
		}
		
		return urlDecodedArgs;
	}
	
	public static String padLeft(String s, int spaces) {
		return String.format("%1$" + spaces + "s", s);
	}
	
	public static String padRight(String s, int spaces) {
		return String.format("%1$-" + spaces + "s", s);
	}
	
	public static String trim(String string, String trim) {
		while (string.startsWith(trim)) {
			string = string.substring(trim.length());
		}
		while (string.endsWith(trim)) {
			string = string.substring(0, string.length() - trim.length());
		}
		return string;
	}
	
	public static String toTitleCase(String str) {
		StringBuilder result = new StringBuilder();
		
		boolean newWord = true;
		for (int i=0; i<str.length(); i++) {
			char c = str.charAt(i);
			if (newWord == true) {
				c = Character.toUpperCase(c);
			}
			result.append(c);
			
			newWord = (c == ' ' || c == '\n' || c == '\r');
		}
		
		return result.toString();
	}
	
	public static String format(String formattedString, Collection<? extends Object> values) {
		for (Object value : values) {
			int firstFormatterIndex = formattedString.indexOf("%");
			if (firstFormatterIndex != -1) {
				int secondFormatterIndex = formattedString.indexOf("%", firstFormatterIndex+1);
				
				int replaceBeginIndex = 0;
				int replaceEndIndex = formattedString.length();
				
				String append = "";
				if (secondFormatterIndex != -1) {
					replaceEndIndex = secondFormatterIndex;
					append = formattedString.substring(replaceEndIndex);
				}
				
				formattedString = String.format(formattedString.substring(replaceBeginIndex, replaceEndIndex), value) + append;
			}
		}
		
		return formattedString;
	}
	
	public static Integer parseAsIntegerNullSafe(String s) {
		if (s == null) {
			return null;
		}
		else {
			return Integer.parseInt(s);
		}
	}
	public static Double parseAsDoubleNullSafe(String s) {
		if (s == null) {
			return null;
		}
		else {
			return Double.parseDouble(s);
		}
	}
}
