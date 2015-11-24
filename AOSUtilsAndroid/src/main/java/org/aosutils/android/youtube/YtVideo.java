package org.aosutils.android.youtube;

import android.util.SparseArray;

public class YtVideo {
	private String ytVideoId;
	private String name;
	private String description;
	private Integer duration;
	private Boolean licensedContent;
	
	private SparseArray<String> urls = new SparseArray<String>();
	
	public YtVideo(String ytVideoId, String name, String description, Integer duration) {
		this.ytVideoId = ytVideoId;
		this.name = name;
		this.description = description;
		this.duration = duration;
	}
	public String toString() {
		return(this.name);
	}	
	
	public String getYtVideoId() {
		if (ytVideoId != null) {
			return ytVideoId;
		}
		else if (urls.get(5) != null) {
			String url = urls.get(5);
			int begin = url.lastIndexOf("/");
			int end = url.indexOf("?");
			if (begin != -1 && end != -1 && end > begin) {
				begin += 1;
				String parsedYtVideoId = url.substring(begin, end);
				return parsedYtVideoId;
			}
		}
		
		return null;
	}
	public String getName() {
		return this.name;
	}
	public String getDescription() {
		return this.description;
	}
	public int getDuration() {
		return this.duration;
	}
	public void setIsLicensedContent(Boolean licensedContent) {
		this.licensedContent = licensedContent;
	}
	public Boolean isLicensedContent() {
		return this.licensedContent;
	}
	
	public SparseArray<String> getUrls() {
		return(urls);
	}
	public String getUrl(int type) {
		return urls.get(type);
	}
	public void setUrls(SparseArray<String> urls) {
		this.urls = urls;
	}
	public void addUrl(int format, String url) {
		urls.put(format, url);
	}
}
