package org.aosutils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
		return joinAndUnmap(urlArgs, "=", "&");
	}
	
	public static String joinAndUnmap(Map<String, String> map, String keyValueDelimiter, String itemDelimiter) {
		ArrayList<String> items = new ArrayList<String>();
		for (String key : map.keySet()) {
			items.add(key + keyValueDelimiter + map.get(key));
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
	
	public static HashMap<String, String> splitAndMap(String inputString, String splitDelimiter, String mapDelimiter) {
		HashMap<String, String> map = new HashMap<String, String>();
		
		for (String part : split(inputString, splitDelimiter)) {
			String key = part.substring(0, part.indexOf(mapDelimiter));
			String value = part.substring(part.indexOf(mapDelimiter)+1);
			map.put(key, value);
		}
		
		return map;
	}
	
	public static HashMap<String, String> parseUrlArgs(String url) {
		return splitAndMap(url.substring(url.indexOf("?")+1), "&", "=");
	}
	
	public static String padLeft(String s, int spaces) {
		return String.format("%1$" + spaces + "s", s);
	}
	
	public static String padRight(String s, int spaces) {
		return String.format("%1$-" + spaces + "s", s);
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
