package org.aosutils.net;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class HttpsForceTrustCertManager {
	private static HttpsForceTrustCertManager _HttpsForceTrustCertManager;
	
	private SSLSocketFactory sslSocketFactory;
	private HostnameVerifier hostnameVerifier;
	
	// Make constructor private, so it can only be called from getInstance();
	private HttpsForceTrustCertManager() {
	}
		
	public static HttpsForceTrustCertManager getInstance() throws NoSuchAlgorithmException, KeyManagementException {
		if (_HttpsForceTrustCertManager == null) {
			_HttpsForceTrustCertManager = new HttpsForceTrustCertManager();
			
			// Create a trust manager that does not validate certificate chains
			TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return null;
				}
			    public void checkClientTrusted(X509Certificate[] certs, String authType) {
			    }
			    public void checkServerTrusted(X509Certificate[] certs, String authType) {
			    }
			} };
			
			// Initialize SSLSocketFactory
			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			_HttpsForceTrustCertManager.sslSocketFactory = sc.getSocketFactory();
			
			// Create all-trusting host name verifier
			_HttpsForceTrustCertManager.hostnameVerifier = new HostnameVerifier() {
				public boolean verify(String hostname, SSLSession session) {
					return true;
				}
			};
		}
		
		return _HttpsForceTrustCertManager;
	}
	
	public void forceTrustSSLCert(HttpsURLConnection urlConnection) {
		// Set the all-trusting trust manager
		urlConnection.setSSLSocketFactory(sslSocketFactory);
		
		// Set the all-trusting host verifier
		urlConnection.setHostnameVerifier(hostnameVerifier);
	}
}
