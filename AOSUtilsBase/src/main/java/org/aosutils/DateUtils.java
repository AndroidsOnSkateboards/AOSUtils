package org.aosutils;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class DateUtils {
	/**
	 * @param UTC: true for UTC, false for current TimeZone
	 * @return ISO 8601 SimpleDateFormat
	 */
	public static SimpleDateFormat getIso8601SimpleDateFormat(boolean UTC) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		if (UTC) {
			sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		}
		return sdf;
	}
}
