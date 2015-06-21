package org.aosutils.net;

import java.io.IOException;

public class HttpStatusCodeException extends IOException {
	private static final long serialVersionUID = -4486771807423795451L;
	
	private int statusCode;
	private String statusMessage;
	private String response;
	
	public HttpStatusCodeException(int statusCode, String statusMessage, String response) {
		super("HTTP " + statusCode + appendToStatusCode(getCombinedMessage(statusMessage, response)));
		this.statusCode = statusCode;
		this.statusMessage = statusMessage;
		this.response = response;
	}
	
	public static boolean isUnauthorized(Exception e) {
		return e instanceof HttpStatusCodeException && ((HttpStatusCodeException) e).statusCode == 401;
	}
	
	public int getStatusCode() {
		return this.statusCode;
	}
	
	public String getStatusMessage() {
		return this.statusMessage;
	}
	
	public String getResponse() {
		return this.response;
	}
	
	private static String getCombinedMessage(String statusMessage, String response) {
		if (statusMessage == null && response == null) {
			return null;
		}
		else if (statusMessage == null || (response != null && response.startsWith(statusMessage))) {
			return response;
		}
		else if (response == null) {
			return statusMessage;
		}
		else {
			return statusMessage + ", " + response;
		}
	}
	
	private static String appendToStatusCode(String combinedMessage) {
		return combinedMessage == null ? "" : ": " + combinedMessage;
	}
}
