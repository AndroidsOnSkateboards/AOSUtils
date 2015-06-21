package org.aosutils.android.youtube;

/*
 * YouTube Data API v3
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.aosutils.net.HttpUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;
import android.net.Uri.Builder;
import android.text.TextUtils;


public class YtApiClientV3 {
	private static class YtSearchResult {
		String nextPageToken;
		
		public YtSearchResult(String nextPageToken) {
			this.nextPageToken = nextPageToken;
		}
		
		public String getNextPageToken() {
			return nextPageToken;
		}
	}
	
	private static class SearchResultIds extends YtSearchResult {
		ArrayList<String> ids;
		
		public SearchResultIds(ArrayList<String> ids, String nextPageToken) {
			super(nextPageToken);
			this.ids = ids;
		}
	}
	
	public static class SearchResultVideos extends YtSearchResult {
		ArrayList<YtVideo> videos;
		
		public SearchResultVideos(ArrayList<YtVideo> videos, String nextPageToken) {
			super(nextPageToken);
			this.videos = videos;
		}
		
		public ArrayList<YtVideo> getVideos() {
			return videos;
		}
	}
	
	public static class SearchResultPlaylists extends YtSearchResult {
		ArrayList<YtPlaylist> playlists;
		
		public SearchResultPlaylists(ArrayList<YtPlaylist> playlists, String nextPageToken) {
			super(nextPageToken);
			this.playlists = playlists;
		}
		
		public ArrayList<YtPlaylist> getPlaylists() {
			return playlists;
		}
	}
	
	
	public static SearchResultPlaylists searchYtPlaylists(String query, int maxResults, String pageToken, String apiKey) throws IOException, JSONException {
		SearchResultIds searchResult = searchYtIds(query, "playlist", maxResults, pageToken, apiKey);
	
		ArrayList<YtPlaylist> videos = new ArrayList<YtPlaylist>();
		if (searchResult.ids != null && searchResult.ids.size() > 0) {
			videos.addAll(getPlaylistInfo(searchResult.ids, apiKey));
		}
		
		return new SearchResultPlaylists(videos, searchResult.nextPageToken);
	}
	
	public static SearchResultVideos searchYtVideos(String query, int maxResults, String pageToken, String apiKey) throws IOException, JSONException {
		SearchResultIds searchResult = searchYtIds(query, "video", maxResults, pageToken, apiKey);
	
		ArrayList<YtVideo> videos = new ArrayList<YtVideo>();
		if (searchResult.ids != null && searchResult.ids.size() > 0) {
			videos.addAll(getVideoInfo(searchResult.ids, apiKey));
		}
		
		return new SearchResultVideos(videos, searchResult.nextPageToken);
	}
	
	public static SearchResultVideos getPlaylistItems(String playlistId, int maxResults, String pageToken, String apiKey) throws IOException, JSONException {
		SearchResultIds searchResult = playlistVideoIds(playlistId, maxResults, pageToken, apiKey);
	
		ArrayList<YtVideo> videos = new ArrayList<YtVideo>();
		if (searchResult.ids != null && searchResult.ids.size() > 0) {
			videos.addAll(getVideoInfo(searchResult.ids, apiKey));
		}
		
		return new SearchResultVideos(videos, searchResult.nextPageToken);
	}
	
	private static SearchResultIds searchYtIds(String query, String type, int maxResults, String pageToken, String apiKey) throws IOException, JSONException {
		ArrayList<String> ids = new ArrayList<String>();
		
		Builder uriBuilder = new Uri.Builder().scheme("https").authority("www.googleapis.com").path("/youtube/v3/search")
				.appendQueryParameter("key", apiKey)
				.appendQueryParameter("part", "id")
				.appendQueryParameter("order", "relevance")
				.appendQueryParameter("maxResults", Integer.toString(maxResults))
				.appendQueryParameter("q", query);
		
		if (type != null) {
			uriBuilder.appendQueryParameter("type", type);
		}
		if (pageToken != null) {
			uriBuilder.appendQueryParameter("pageToken", pageToken);
		}
		
		String uri = uriBuilder.build().toString();
		String output = HttpUtils.get(uri, null, _YtApiConstants.HTTP_TIMEOUT);
		
		
		JSONObject jsonObject = new JSONObject(output);
		
		String nextPageToken = jsonObject.has("nextPageToken") ? jsonObject.getString("nextPageToken") : null;
		JSONArray items = jsonObject.getJSONArray("items");
		
		for (int i=0; i<items.length(); i++) {
			JSONObject item = items.getJSONObject(i);
			JSONObject id = item.getJSONObject("id");
			
			ids.add(id.has("videoId") ? id.getString("videoId") : id.getString("playlistId"));
		}
		
		SearchResultIds searchResult = new SearchResultIds(ids, nextPageToken);
		
		return searchResult;
	}
	
	private static SearchResultIds playlistVideoIds(String playlistId, int maxResults, String pageToken, String apiKey) throws IOException, JSONException {
		ArrayList<String> ids = new ArrayList<String>();
		
		Builder uriBuilder = new Uri.Builder().scheme("https").authority("www.googleapis.com").path("/youtube/v3/playlistItems")
				.appendQueryParameter("key", apiKey)
				.appendQueryParameter("part", "id,snippet")
				.appendQueryParameter("maxResults", Integer.toString(maxResults))
				.appendQueryParameter("playlistId", playlistId);
		
		if (pageToken != null) {
			uriBuilder.appendQueryParameter("pageToken", pageToken);
		}
		
		String uri = uriBuilder.build().toString();
		String output = HttpUtils.get(uri, null, _YtApiConstants.HTTP_TIMEOUT);
		
		JSONObject jsonObject = new JSONObject(output);
		
		String nextPageToken = jsonObject.has("nextPageToken") ? jsonObject.getString("nextPageToken") : null;
		JSONArray items = jsonObject.getJSONArray("items");
		
		for (int i=0; i<items.length(); i++) {
			JSONObject item = items.getJSONObject(i);
			JSONObject snippet = item.getJSONObject("snippet");
			
			JSONObject resourceId = snippet.getJSONObject("resourceId");
			ids.add(resourceId.getString("videoId"));
		}
		
		SearchResultIds searchResult = new SearchResultIds(ids, nextPageToken);
		
		return searchResult;
	}
	
	public static YtPlaylist getPlaylistInfo(String playlistId, String apiKey) throws IOException, JSONException {
		ArrayList<String> playlistIds = new ArrayList<String>();
    	playlistIds.add(playlistId);
    	ArrayList<YtPlaylist> playlistInfos = getPlaylistInfo(playlistIds, apiKey);
    	return playlistInfos.size() == 0 ? null : playlistInfos.get(0);
	}
	
	public static ArrayList<YtPlaylist> getPlaylistInfo(Collection<String> playlistIds, String apiKey) throws IOException, JSONException {
		ArrayList<YtPlaylist> playlists = new ArrayList<YtPlaylist>();
		
		String uri = new Uri.Builder().scheme("https").authority("www.googleapis.com").path("/youtube/v3/playlists")
				.appendQueryParameter("key", apiKey)
				.appendQueryParameter("part", "id,snippet")
				.appendQueryParameter("id", TextUtils.join(",", playlistIds)).build().toString();
		
		String output = HttpUtils.get(uri, null, _YtApiConstants.HTTP_TIMEOUT);
		JSONObject jsonObject = new JSONObject(output);
		
		JSONArray items = jsonObject.getJSONArray("items");
		for (int i=0; i<items.length(); i++) {
			JSONObject item = items.getJSONObject(i);
			
			String playlistId = item.getString("id");
			String title = item.getJSONObject("snippet").getString("title");
			String description = item.getJSONObject("snippet").getString("description");
			
			YtPlaylist playlist = new YtPlaylist(playlistId, title, description);
			playlists.add(playlist);
		}
		
		return playlists;
	}
	
	public static YtVideo getVideoInfo(String videoId, String apiKey) throws IOException, JSONException {
		ArrayList<String> videoIds = new ArrayList<String>();
    	videoIds.add(videoId);
    	ArrayList<YtVideo> videoInfos = getVideoInfo(videoIds, apiKey);
    	return videoInfos.size() == 0 ? null : videoInfos.get(0);
	}
	
	public static ArrayList<YtVideo> getVideoInfo(Collection<String> videoIds, String apiKey) throws IOException, JSONException {
		ArrayList<YtVideo> videos = new ArrayList<YtVideo>();
		
		String uri = new Uri.Builder().scheme("https").authority("www.googleapis.com").path("/youtube/v3/videos")
				.appendQueryParameter("key", apiKey)
				.appendQueryParameter("part", "id,snippet,contentDetails")
				.appendQueryParameter("id", TextUtils.join(",", videoIds)).build().toString();
		
		String output = HttpUtils.get(uri, null, _YtApiConstants.HTTP_TIMEOUT);
		JSONObject jsonObject = new JSONObject(output);
		
		JSONArray items = jsonObject.getJSONArray("items");
		for (int i=0; i<items.length(); i++) {
			JSONObject item = items.getJSONObject(i);
			
			String videoId = item.getString("id");
			String title = item.getJSONObject("snippet").getString("title");
			String description = item.getJSONObject("snippet").getString("description");
			
			String durationStr = item.getJSONObject("contentDetails").getString("duration");
			
			int hours = !durationStr.contains("H") ? 0 : Integer.parseInt(durationStr.substring(durationStr.indexOf("PT") + 2, durationStr.indexOf("H")));
			int minutes = !durationStr.contains("M") ? 0 : 
				hours > 0 ? Integer.parseInt(durationStr.substring(durationStr.indexOf("H") + 1, durationStr.indexOf("M"))) :
					Integer.parseInt(durationStr.substring(durationStr.indexOf("PT") + 2, durationStr.indexOf("M")));
			int seconds = !durationStr.contains("S") ? 0 : minutes > 0 ? Integer.parseInt(durationStr.substring(durationStr.indexOf("M") + 1, durationStr.indexOf("S"))) :
				hours > 0 ? Integer.parseInt(durationStr.substring(durationStr.indexOf("H") + 1, durationStr.indexOf("S"))) :
				Integer.parseInt(durationStr.substring(durationStr.indexOf("PT") + 2, durationStr.indexOf("S")));
			int duration = (hours * 60 * 60) + (minutes * 60) + seconds;
			
			boolean licensedContent = item.getJSONObject("contentDetails").getBoolean("licensedContent");
			
			
			YtVideo video = new YtVideo(videoId, title, description, duration);
			video.setIsLicensedContent(licensedContent);
			videos.add(video);
		}
		
		return videos;
	}
}
