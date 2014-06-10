package org.aosutils.android.youtube;

public class YtPlaylist {
	private String ytPlaylistId;
	private String title;
	private String summary;
	
	public YtPlaylist(String ytPlaylistId, String title, String summary) {
		this.ytPlaylistId = ytPlaylistId;
		this.title = title;
		this.summary = summary;
	}
	
	public String getYtPlaylistId() {
		return this.ytPlaylistId;
	}
	
	public String getTitle() {
		return this.title;
	}
	
	public String getSummary() {
		return this.summary;
	}
}
