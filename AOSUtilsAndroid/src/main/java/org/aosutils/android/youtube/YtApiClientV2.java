package org.aosutils.android.youtube;

/*
 * YouTube Data API v2
 */

import org.aosutils.net.HttpUtils;
import org.aosutils.net.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

public class YtApiClientV2 {	
	/*
	 * Known formats:
	 * 1: RTSP streaming URL for mobile video playback. H.263 video (up to 176x144) and AMR audio.
	 * 5: HTTP URL to the embeddable player (SWF) for this video. This format is not available for a video that is not embeddable. Developers commonly add &format=5 to their queries to restrict results to videos that can be embedded on their sites.
	 * 6: RTSP streaming URL for mobile video playback. MPEG-4 SP video (up to 176x144) and AAC audio.
	 * 
	 * Formats 6 & 1 are playable on Android's MediaPlayer
	 */
	
	public static ArrayList<YtPlaylist> searchYtPlaylists(String query, int from, int maxResults) throws IOException, SAXException, ParserConfigurationException {
		String url = "http://gdata.youtube.com/feeds/api/playlists/snippets?v=2&max-results=" + maxResults + "&start-index=" + from + "&q=" + URLEncoder.encode(query, _YtApiConstants.CHARACTER_ENCODING);
		return parsePlaylists(getEntries(url));
	}
	public static ArrayList<YtVideo> searchYtVideos(String query, int from, int maxResults) throws IOException, ParserConfigurationException, SAXException  {
		String url = "http://gdata.youtube.com/feeds/api/videos?v=2&orderby=relevance&max-results=" + maxResults + "&start-index=" + from + "&q=" + URLEncoder.encode(query, _YtApiConstants.CHARACTER_ENCODING); // + "&format=6";
		
		NodeList entries = getEntries(url);
		return parseVideos(entries);
	}
	public static ArrayList<YtVideo> getYtPlaylist(String ytPlaylistId) throws IOException, ParserConfigurationException, SAXException  {
		String url = "http://gdata.youtube.com/feeds/api/playlists/" + ytPlaylistId + "?v=2"; // + "&format=6";
		return parseVideos(getEntries(url));
	}
	public static String getYouTubeDescription(String ytVideoId) {
		String youTubeDescription = null;
		try {
			String url = "http://gdata.youtube.com/feeds/api/videos/" + ytVideoId + "?v=2";
			Document doc = XmlUtils.parseXml(HttpUtils.get(url, null, _YtApiConstants.HTTP_TIMEOUT));
			Element entry = (Element) doc.getElementsByTagName("entry").item(0);
			Element mediaGroup = (Element) entry.getElementsByTagName("media:group").item(0);
			Node mediaDescription = mediaGroup.getElementsByTagName("media:description").item(0);
			youTubeDescription = mediaDescription.getFirstChild().getNodeValue();
		} catch (Exception e) { // All kinds of exceptions (Connection, DOM Parsing, NullPointers)
			
		}
		
		return youTubeDescription;
	}
	
	private static NodeList getEntries(String url) throws IOException, SAXException, ParserConfigurationException {
		String page = HttpUtils.get(url, null, _YtApiConstants.HTTP_TIMEOUT);
		Document doc = XmlUtils.parseXml(page);
		
		NodeList feeds = doc.getElementsByTagName("feed");
		if (feeds.getLength() == 0) {
			// "Feed" doesn't exist, just send back this empty NodeList, it will be treated like there are 0 entries
			return feeds;
		}
		else {
			// Return entries
			Element feed = (Element) feeds.item(0);
			return feed.getElementsByTagName("entry");
		}
	}
	
	private static ArrayList<YtPlaylist> parsePlaylists(NodeList entries) {
		ArrayList<YtPlaylist> ytPlaylists = new ArrayList<>();
		
		for (int i=0; i<entries.getLength(); i++) {
			Element entry = (Element) entries.item(i);
			
			try {
				String playlistId = entry.getElementsByTagName("yt:playlistId").item(0).getFirstChild().getNodeValue();
				String title = entry.getElementsByTagName("title").item(0).getFirstChild().getNodeValue();
				String summary = entry.getElementsByTagName("summary").item(0).getFirstChild().getNodeValue();
				
				ytPlaylists.add(new YtPlaylist(playlistId, title, summary));
			}
			catch (NullPointerException e) {
				
			}
		}
		
		return ytPlaylists;
	}
	
	private static ArrayList<YtVideo> parseVideos(NodeList entries) {
		ArrayList<YtVideo> videos = new ArrayList<>();
		
		for (int i=0; i<entries.getLength(); i++) {
			try {
				Element entry = (Element) entries.item(i);
				
				NodeList mediaGroups = entry.getElementsByTagName("media:group");
				if (mediaGroups.getLength() > 0) {
					Element mediaGroup = (Element) mediaGroups.item(0);
					
					String ytVideoId = mediaGroup.getElementsByTagName("yt:videoid").item(0).getFirstChild().getNodeValue();
					String title = mediaGroup.getElementsByTagName("media:title").item(0).getFirstChild().getNodeValue();
					int duration = Integer.parseInt(mediaGroup.getElementsByTagName("yt:duration").item(0).getAttributes().getNamedItem("seconds").getNodeValue());
					
					String description = "";
					try {
						description = mediaGroup.getElementsByTagName("media:description").item(0).getFirstChild().getNodeValue();
					}
					catch (Exception e) {
						
					}
					
					title = title.replace("&quot;", "\"").replace("&apos;", "'").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">");
					description = description.replace("&quot;", "\"").replace("&apos;", "'").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">");
					
					YtVideo video = new YtVideo(ytVideoId, title, description, duration);
					videos.add(video);
					
					NodeList mediaContents = mediaGroup.getElementsByTagName("media:content");
					for (int j=0; j<mediaContents.getLength(); j++) {
						Node mediaContent = mediaContents.item(j);
						
						String mediaContentUrl = mediaContent.getAttributes().getNamedItem("url").getNodeValue();
						int mediaContentFormat = Integer.parseInt(mediaContent.getAttributes().getNamedItem("yt:format").getNodeValue());
						
						video.addUrl(mediaContentFormat, mediaContentUrl);
					}
				}
				
				/*
				Node rating = entry.getElementsByTagName("yt:rating").item(0);
				int numLikes = Integer.parseInt(rating.getAttributes().getNamedItem("numLikes").getNodeValue());
				int numDislikes = Integer.parseInt(rating.getAttributes().getNamedItem("numDislikes").getNodeValue());
				float ratingValue = (float) numLikes / (numLikes + numDislikes);
				
				Node statistics = entry.getElementsByTagName("yt:statistics").item(0);
				int viewCount = Integer.parseInt(statistics.getAttributes().getNamedItem("viewCount").getNodeValue());
				*/
			}
			catch (Exception e) {
				
			}
		}
		
		return videos;
	}
}
