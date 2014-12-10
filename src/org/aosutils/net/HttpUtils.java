package org.aosutils.net;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HttpsURLConnection;

import org.aosutils.AOSConstants;
import org.aosutils.IoUtils;
import org.aosutils.IoUtils.WriteFileMonitor;

public class HttpUtils {	
	public static String get(String url, Map<String, String> headers, Integer httpTimeout) throws FileNotFoundException, MalformedURLException, IOException {
		return request(url, headers, null, httpTimeout, null, false);
	}
	public static String post(String url, Map<String, String> headers, String postData, Integer httpTimeout) throws FileNotFoundException, MalformedURLException, IOException {
		postData = (postData == null ? "" : postData);
		return request(url, headers, postData, httpTimeout, null, false);
	}
	
	public static String request(String url, Map<String, String> headers, String postData, Integer httpTimeout, Proxy proxy, boolean forceTrustSSLCert) throws FileNotFoundException, MalformedURLException, IOException {
		InputStream inputStream = getStream(requestConnection(url, headers, postData, httpTimeout, proxy, forceTrustSSLCert));
		return IoUtils.getString(inputStream);
	}
	
	public static URLConnection requestConnection(String url, Map<String, String> headers, String postData, Integer httpTimeout, Proxy proxy, boolean forceTrustSSLCert) throws FileNotFoundException, MalformedURLException, IOException {
		URL urlObj = new URL(url);
		URLConnection urlConnection = proxy == null ? urlObj.openConnection() : urlObj.openConnection(proxy);
		
		if (httpTimeout != null) {
			urlConnection.setConnectTimeout(httpTimeout);
			urlConnection.setReadTimeout(httpTimeout);
		}
		
		if (forceTrustSSLCert == true && urlConnection instanceof HttpsURLConnection) {
			try {
				HttpsForceTrustCertManager.getInstance().forceTrustSSLCert( (HttpsURLConnection) urlConnection);
			} catch (KeyManagementException | NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
		}
		
		if (headers == null) {
			headers = new HashMap<String, String>();
		}
		
		for (String param : headers.keySet()) {
			urlConnection.setRequestProperty(param, headers.get(param));
		}
		
		if (postData != null) {
			urlConnection.setDoOutput(true);
			
			if (!postData.equals("")) {
				if ("gzip".equals(headers.get("Content-Encoding"))) {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					GZIPOutputStream gzos = new GZIPOutputStream(baos);
				    gzos.write(postData.getBytes(AOSConstants.CHARACTER_ENCODING));
				    postData = new String(baos.toByteArray());
				}
			    
				IoUtils.sendToOutputStream(urlConnection.getOutputStream(), postData);
			}
		}
		
		return urlConnection;
	}
	
	public static class DownloadMonitor extends WriteFileMonitor {
		private int bytesTotal;
		private Thread thread;
		private IOException ioException;
		
		public DownloadMonitor(int bytesTotal) {
			this.bytesTotal = bytesTotal;
		}
		
		public int getBytesTotal() {
			return this.bytesTotal;
		}
		
		public Thread getThread() {
			return this.thread;
		}
		public IOException getException() {
			return this.ioException;
		}
	}
	
	/* 
	 * If running in a separate thread, you must manually check for an IOException
	 */
	public static DownloadMonitor download(URLConnection urlConnection, final String localFilename, boolean runInSeparateThread) throws IOException {
		final DownloadMonitor downloadMonitor = new DownloadMonitor(urlConnection.getContentLength());
		final InputStream inputStream = getStream(urlConnection);
		
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				try {
					IoUtils.writeFile(localFilename, inputStream, downloadMonitor);
				} catch (IOException e) {
					downloadMonitor.ioException = e;
				}
			}
		};
		
		if (runInSeparateThread) {
			Thread thread = new Thread(runnable);
			downloadMonitor.thread = thread;
			thread.start();
		}
		else {
			runnable.run();
			
			if (downloadMonitor.ioException != null) {
				throw downloadMonitor.ioException;
			}
		}
		
		return downloadMonitor;
	}
	
	public static InputStream getStream(URLConnection urlConnection) throws HttpStatusCodeException, IOException {
		if (urlConnection instanceof HttpURLConnection) {
			HttpURLConnection httpConnection = (HttpURLConnection) urlConnection;
			
			int statusCode = httpConnection.getResponseCode();
			
			// Some old Android devices throw statusCode -1 when there's a problem
			if (statusCode == -1 || statusCode >= 300) { // Bad HTTP status code
				String response = null;
				try {
					InputStream errorStream = gUnzip(httpConnection.getErrorStream(), urlConnection);
					response = IoUtils.getString(errorStream);
				}
				catch (Exception e) {
				}
				
				throw new HttpStatusCodeException(statusCode, httpConnection.getResponseMessage(), response);
			}
		}
		
		// This may throw a new IOException
		return gUnzip(urlConnection.getInputStream(), urlConnection);
	}
	
	private static InputStream gUnzip(InputStream inputStream, URLConnection urlConnection) throws IOException {
		if ("gzip".equals(urlConnection.getContentEncoding())) {
			inputStream = new GZIPInputStream(inputStream);
		}
		return inputStream;
	}
	
	public static String getPublicIpAddress(final Integer httpTimeout) {
		final StringBuilder ipInfo = new StringBuilder();
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					//String url = "http://checkip.dyndns.org/";
					String url = "http://api.ipify.org/";
					ipInfo.append(HttpUtils.get(url, null, httpTimeout));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		thread.start();
		String ip = null;
		try {
			thread.join();
			if (!("".equals(ipInfo.toString().trim()))) {
				ip = ipInfo.toString().trim();
			}
		} catch (InterruptedException e) {
		}
		
		return ip;
	}
}
