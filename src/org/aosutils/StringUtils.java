package org.aosutils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
	
	public static String padLeft(String s, int spaces) {
		return String.format("%1$" + spaces + "s", s);
	}
	
	public static String padRight(String s, int spaces) {
		return String.format("%1$-" + spaces + "s", s);
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
