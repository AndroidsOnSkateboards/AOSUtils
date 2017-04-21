package org.aosutils.android.youtube;

import android.text.TextUtils;

import org.aosutils.AOSConstants;
import org.aosutils.StringUtils;
import org.aosutils.net.HttpUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class YtApiSignature {
	/*
	 *  Thanks to the great documentation at youtubedown, I've been able to replicate this in Java:
	 *  youtubedown: http://www.jwz.org/hacks/youtubedown
	 */
	
	private static class Html5PlayerInfo {
		String version;
		String url;
	}
	
	public static String requestCurrentAlgorithm() throws IOException {
		HashMap<String, String> headers = new HashMap<>();
		headers.put("User-Agent", AOSConstants.USER_AGENT_DESKTOP);
		
		String homepage = HttpUtils.get("http://" + _YtApiConstants.YOUTUBE_DOMAIN, headers, _YtApiConstants.HTTP_TIMEOUT);
		
		return requestCurrentAlgorithm(homepage);
	}
	
	public static String requestCurrentAlgorithm(String youtubePageSource) throws IOException {
		Html5PlayerInfo playerInfo = getHtml5PlayerInfo(youtubePageSource);
		return playerInfo == null ? null : requestCurrentAlgorithmFromHtml5PlayerUrl(playerInfo.url);
	}
	
	public static String getCurrentVersion(String youtubePageSource) {
		Html5PlayerInfo playerInfo = getHtml5PlayerInfo(youtubePageSource);
		return playerInfo == null ? null : playerInfo.version;
	}
	
	/**
	 * Same algorithm format as used in youtubedown:
	 * 
	 * r  = reverse the string;
	 * sN = slice from character N to the end;
	 * wN = swap 0th and Nth character.
	 */
	public static String decode(String signature, String algorithm) {
		for (String procedure : TextUtils.split(algorithm, " ")) {
			String procedureType = procedure.substring(0, 1);
			if (procedureType.equals("r")) { // reverse the string
				signature = new StringBuilder(signature).reverse().toString();
			}
			else if (procedureType.equals("s")) { // slice from character N to the end
				int position = Integer.parseInt(procedure.substring(1));
				signature = signature.substring(position);
			}
			else if (procedureType.equals("w")) { // swap 0th and Nth character
				int position = Integer.parseInt(procedure.substring(1));
				signature = signature.substring(position, position+1) + signature.substring(1, position) + signature.substring(0, 1) + signature.substring(position+1);
			}
		}
		return signature;
	}
	
	private static Html5PlayerInfo getHtml5PlayerInfo(String youtubePageSource) {
		String regex = "player-([^{:]+?)\\.js";

		Matcher matcher = Pattern.compile(regex).matcher(youtubePageSource);
		if (matcher.find()) {
			int pathBegin = youtubePageSource.substring(0, matcher.start()).lastIndexOf("\"") + 1;
			int pathEnd = youtubePageSource.substring(matcher.end()).indexOf("\"") + matcher.end();

			String playerVersion = matcher.group(1).replace("\\/", "/");
			if (playerVersion.contains("/")) {
				playerVersion = playerVersion.substring(0, playerVersion.indexOf("/"));
			}

			String path = youtubePageSource.substring(pathBegin, pathEnd);
			while (path.contains("\\/")) {
				path = path.replace("\\/", "/");
			}
			
			if (path.startsWith("//")) {
				path = "http:" + path;
			}

			Html5PlayerInfo playerInfo = new Html5PlayerInfo();
			playerInfo.version = playerVersion;
			playerInfo.url = path;
			
			return playerInfo;
		}

		return null;
	}
	
	private static String requestCurrentAlgorithmFromHtml5PlayerUrl(String html5PlayerUrl) throws IOException {
		String playerJsSrc = HttpUtils.get(html5PlayerUrl, null, _YtApiConstants.HTTP_TIMEOUT);
		return getAlgorithmFromHtml5PlayerJsSrc(playerJsSrc);
	}
		
	private static String getAlgorithmFromHtml5PlayerJsSrc(String jsSrc) throws IOException {
		// Find "C" in this: var A = B.sig || C (B.s), this will be the name of the signature swapping algorithm function
		String c = null;
		{
			String regex = "var [^;]+=[^;]+.sig\\|\\|(.+?)\\([^;]+\\)";
			Matcher matcher = Pattern.compile(regex).matcher(jsSrc);
			if (matcher.find()) {
				c = matcher.group(1);
			}
		}

		// Find body of function C(D) { ... }, this is the signature swapping algorithm function :)
		String body = null;
		if (c != null) {
			body = getFunctionBody(c, jsSrc);
		}

		if (body != null) {
			// They inline the swapper if it's used only once.
			// Convert "var b=a[0];a[0]=a[63%a.length];a[63]=b;" to "a=swap(a,63);".
			{
				String regex = "var (.+?)=(.+?)\\[(.+?)\\];(.+?)\\[(.+?)\\]=(.+?)\\[(.+?)%(.+?)\\.length\\];(.+?)\\[(.+?)\\]=(.+?);";
				body = replaceAll(body, regex, new int[] {6, 6, 7}, "%s=swap(%s,%s);");
			}
			
			/*
			 * Handle procedures
			 */
			
			// Split
			body = replaceAll(body, "[^;]+=[^;]+\\.split\\(\"\"\\);", new int[] { }, "");

			// Join
			body = replaceAll(body, "[;]*[ ]*return [^;]+\\.join\\(\"\"\\)", new int[] { }, "");

			// Reverse
			body = replaceAll(body, "[^;]+=[^;]+\\.reverse\\(\\)[^;]*", new int[] { }, "r");

			// Slice
			body = replaceAll(body, "[^;]+=[^;]+\\.slice\\((.*?)\\)[^;]*", new int[]{1}, "s%s");

			// Function call to Swap/Reverse/Splice
			//body = replaceAll(body, "[^;]+=[^;]+\\([^;]+,(.+?)\\)[^;]*", new int[] { 1 }, "w%s");
			body = replaceFunctions(body, jsSrc);

			// Clean up
			body = body.replace(";", " ").trim();
		}
		
		return body;
	}
	
	private static String getFunctionBody(String functionName, String document) {

		String regex;
		if (functionName.contains(".")) {
			String functionVar = functionName.substring(0, functionName.indexOf(".")).trim();
			String function = functionName.substring(functionName.indexOf(".")+1).trim();
			
			regex = "var " + Pattern.quote(functionVar) + "=\\{.*?" + function + ":function\\(.*?\\)\\{(.*?)\\}";
		}
		else {
			regex = document.contains("function " + functionName)
					? "function " + Pattern.quote(functionName) + "\\(.+?\\)\\{(.+?)\\}"
					: "[, \\n]" + Pattern.quote(functionName) + "=function\\(.*?\\)\\{(.*?)\\}";
		}

		Matcher matcher = Pattern.compile(regex, Pattern.DOTALL).matcher(document);
		if (matcher.find()) {
			return matcher.group(1);
		}

		return null;
	}
	
	private static String replaceAll(String document, String regexToFind, int[] valueIndicesToExtract, String formattedStringToReplace) {
		Matcher matcher = Pattern.compile(regexToFind).matcher(document);
		while (matcher.find()) {
			String match = document.substring(matcher.start(), matcher.end());
			
			ArrayList<String> extractedValues = new ArrayList<>();
			
			for (int valueIndexToExtract : valueIndicesToExtract) {
				String extractedValue = matcher.group(valueIndexToExtract);
				extractedValues.add(extractedValue);
			}
			
			String replaceString = StringUtils.format(formattedStringToReplace, extractedValues);
			String regexEscapedMatch = Pattern.quote(match);
			
			document = document.replaceFirst(regexEscapedMatch, replaceString);
			
			// Reset matcher for new document (with new replacements)
			matcher = Pattern.compile(regexToFind).matcher(document);
		}
		
		return document;
	}
	
	private static String replaceFunctions(String algorithmFunction, String jsSrc) {
		String regexToFind = "[^;]*?[=]*([^;]*?)\\(([^;]+?),([^;]+?)\\)[^;]*";
		
		Matcher matcher = Pattern.compile(regexToFind).matcher(algorithmFunction);

		while (matcher.find()) {
			//String match = algorithmFunction.substring(matcher.start(), matcher.end());
			String fnName = matcher.group(1);
			String fnValue1 = matcher.group(2);
			String fnValue2 = matcher.group(3);

			String fnValue = fnValue2;
			try {
				Double.parseDouble(fnValue2);
			} catch (NumberFormatException e) {
				fnValue = fnValue1;
			}

			String fnBody = getFunctionBody(fnName, jsSrc);
			String replacement = fnBody.contains("reverse") ? "r" :
					fnBody.contains("slice") || fnBody.contains("splice") ? "s" + fnValue :
							"w" + fnValue;

			algorithmFunction = algorithmFunction.substring(0, matcher.start()) + replacement + algorithmFunction.substring(matcher.end());

			// Reset matcher for new document (with new replacements)
			matcher = Pattern.compile(regexToFind).matcher(algorithmFunction);
		}
		
		return algorithmFunction;
	}
}
