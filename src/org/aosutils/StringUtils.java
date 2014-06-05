package org.aosutils;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
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
	
	public static String padRight(String s, int n) {
		return String.format("%1$-" + n + "s", s);  
	}

	public static String padLeft(String s, int n) {
		return String.format("%1$" + n + "s", s);  
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
	
	public String readFromFile(String fullPath) throws IOException {
		Path pathObj = FileSystems.getDefault().getPath(fullPath);
		String response = new String(Files.readAllBytes(pathObj));
		
		return response;
	}
}
