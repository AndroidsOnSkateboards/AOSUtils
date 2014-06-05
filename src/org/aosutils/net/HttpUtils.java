package org.aosutils.net;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class HttpUtils {
	public static class HTTPUnauthorizedException extends IOException {
		public HTTPUnauthorizedException(String message) {
			super(message);
		}

		private static final long serialVersionUID = -58562254193206846L;
	}
	
	public static String get(String uri, Map<String, String> headers, Integer httpTimeout) throws FileNotFoundException, MalformedURLException, IOException {
		return request(uri, headers, false, null, httpTimeout, null);
	}
	public static String get(String uri, Map<String, String> headers, Integer httpTimeout, Proxy proxy) throws FileNotFoundException, MalformedURLException, IOException {
		return request(uri, headers, false, null, httpTimeout, proxy);
	}
	
	public static String post(String uri, Map<String, String> headers, String postData, Integer httpTimeout) throws FileNotFoundException, MalformedURLException, IOException {
		return request(uri, headers, true, postData, httpTimeout, null);
	}
	public static String post(String uri, Map<String, String> headers, String postData, Integer httpTimeout, Proxy proxy) throws FileNotFoundException, MalformedURLException, IOException {
		return request(uri, headers, true, postData, httpTimeout, proxy);
	}
	
	private static String request(String uri, Map<String, String> headers, boolean post, String postData, Integer httpTimeout, Proxy proxy) throws FileNotFoundException, MalformedURLException, IOException {
		URL url = new URL(uri);
		URLConnection urlConnection = proxy == null ? url.openConnection() : url.openConnection(proxy);
		
		if (httpTimeout != null) {
			urlConnection.setConnectTimeout(httpTimeout);
			urlConnection.setReadTimeout(httpTimeout);
		}
		
		if (headers == null) {
			headers = new HashMap<String, String>();
		}
		
		for (String param : headers.keySet()) {
			urlConnection.setRequestProperty(param, headers.get(param));
		}
		
		if (post) {
			urlConnection.setDoOutput(true);
			
			if (postData != null) {
				if ("gzip".equals(headers.get("Content-Encoding"))) {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					GZIPOutputStream gzos = new GZIPOutputStream(baos);
				    gzos.write(postData.getBytes("UTF-8"));
				    postData = new String(baos.toByteArray());
				}
			    
				sendToOutputStream(urlConnection.getOutputStream(), postData);
			}
		}
		
		try {
			InputStream resultStream = urlConnection.getInputStream();
			if ("gzip".equals(urlConnection.getContentEncoding())) {
				resultStream = new GZIPInputStream(resultStream);
			}
			return getString(resultStream);
		}
		catch (IOException e) {
			if (urlConnection instanceof HttpURLConnection) {
				HttpURLConnection httpConnection = (HttpURLConnection) urlConnection;
				if (httpConnection.getResponseCode() == 401) { // Unauthorized
					throw new HTTPUnauthorizedException(e.getMessage());
				}
				else {
					System.err.println("HTTP ResponseCode: " + httpConnection.getResponseCode());
				}
			}
			
			throw e;
		}
	}
	
	public static String getString(InputStream inputStream) throws IOException {
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
		
		String output = "";
		String line = bufferedReader.readLine();
		while (line != null) {
			output += line + "\n";
			line = bufferedReader.readLine();
		}
		
		bufferedReader.close();
		
		return output;
	}
	
	public static void sendToOutputStream(OutputStream outputStream, String output) throws IOException {
		OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
		writer.write(output);
		writer.flush();
	}
}
