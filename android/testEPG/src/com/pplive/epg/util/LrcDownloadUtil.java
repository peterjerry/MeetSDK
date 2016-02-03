package com.pplive.epg.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

public class LrcDownloadUtil {
	private static String API_GECIME_LYRIC_URL = "http://geci.me/api/lyric/";
	private static String API_GECIME_LYRIC_ATRIST = "http://geci.me/api/artist/";
	private static String API_BAIDU_LYRIC_URL = "http://box.zhangmen.baidu.com" +
			"/x?op=12" +
			"&count=1" +
			"&title="; //%B2%BB%B5%C3%B2%BB%B0%AE$$%C5%CB%E7%E2%B0%D8$$$$";
	private static String BAIDU_LRC_URL_FMT = "http://box.zhangmen.baidu.com/bdlrc/%d/%d.lrc";
	private static String BAIDU_LRC2_URL_FMT = "http://music.baidu.com/data/music/links?songIds=%d";
	private static String BAIDU_SONG_SEARCH_URL_FMT = "http://musicmini.baidu.com/app/search/searchList.php?qword=";
    private static String BAIDU_LRC_HOST = "http://qukufile2.qianqian.com";
    
	public static class SongInfo {
        private String mSongName;
        private String mArtist;
        private String mLrcPath;
        private String mSongPicPath;

        public SongInfo(String songName, String artist, String lrcpath, String songPic) {
            this.mSongName = songName;
            this.mArtist = artist;
            this.mLrcPath = lrcpath;
            this.mSongPicPath = songPic;
        }

        public String getLrcPath() {
            return mLrcPath;
        }

        public String getSongPicPath() {
            return mSongPicPath;
        }
    };
    
	public static List<LrcData> getLyc(String song_name) {
		return getLyc(song_name, null);
	}
	
	public static List<LrcData> getLyc(String song_name, String artist) {
		System.out.println(String.format("Java: getLyc(): song %s, artist %s", song_name, artist));
		
		String encoded_str;
		try {
			encoded_str = URLEncoder.encode(song_name, "UTF-8");
			String url = API_GECIME_LYRIC_URL + encoded_str;
			
			if (artist != null) {
				encoded_str = URLEncoder.encode(artist, "UTF-8");
				url += "/";
				url += encoded_str;
			}
			
			System.out.println("Java: getLyc(): " + url);
			
			HttpGet request = new HttpGet(url);
			HttpResponse response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				System.out.println("Java: return status is not 200 " + response.getStatusLine().getStatusCode());
				return null;
			}
			
			String result = EntityUtils.toString(response.getEntity());
			System.out.println("Java: result: " + result);
			
			JSONTokener jsonParser = new JSONTokener(result);
			JSONObject root = (JSONObject) jsonParser.nextValue();
			int count = root.getInt("count");
			JSONArray lyrics = root.getJSONArray("result");
			int num = lyrics.length();
			
			List<LrcData> lrcList = new ArrayList<LrcData>();
			
			for (int i=0;i<num;i++) {
				JSONObject lyc = lyrics.getJSONObject(i);
				String lrc_path = lyc.getString("lrc");
				String lrc_song = lyc.getString("song");
				int artist_id = lyc.getInt("artist_id");
				int sid = lyc.getInt("sid");
				
				LrcData lrc = new LrcData(artist_id, "", sid, 0, lrc_song, lrc_path);
				lrcList.add(lrc);
				
				System.out.println(String.format("Java: get lrc_path: %s, lrc_song: %s, artist_id: %d, sid %d",
						lrc_path, lrc_song, artist_id, sid));
			}
			
			return lrcList;
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
		
	}
	
	public static String getBaiduLyc(String song_name, String artist) {
		System.out.println(String.format("Java: getBaiduLyc(): song %s, artist %s", song_name, artist));
		
		String encoded_str;
		try {
			encoded_str = URLEncoder.encode(song_name, "GB2312");
			String url = API_BAIDU_LYRIC_URL + encoded_str;
			
			if (artist != null) {
				encoded_str = URLEncoder.encode(artist, "GB2312");
				url += "$$";
				url += encoded_str;
				url += "$$$$";
			}
			
			System.out.println("Java: getLyc(): " + url);
			
			HttpGet request = new HttpGet(url);
			HttpResponse response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200){
				System.out.println("Java: return status is not 200 " + response.getStatusLine().getStatusCode());
				return null;
			}
			
			String result = new String(EntityUtils.toString(
					response.getEntity()).getBytes("gb2312"), "utf-8");
			System.out.println("Java: result: " + result);
			
			SAXBuilder builder = new SAXBuilder();
			Reader returnQuote = new StringReader(result);
	        Document doc = builder.build(returnQuote);
			Element root = doc.getRootElement();
			
			String count = root.getChildText("count");
			int c = Integer.valueOf(count);
			if (c == 0) {
				System.out.println("Java: lrc count is zero");
				return null;
			}
			
			List<Element> xml_url_list = root.getChildren("url");
			if (xml_url_list == null || xml_url_list.size() == 0) {
				System.out.println("Java: failed to get xml_url_list");
				return null;
			}
			
			Element first_item = xml_url_list.get(0);
			String lrcid = first_item.getChildText("lrcid");
			int id = Integer.valueOf(lrcid);
			if (id ==0) {
				System.out.println("Java: error lrcid is 0");
				return null;
			}
			
			String lrc_url = String.format(BAIDU_LRC_URL_FMT, id / 100, id);
			System.out.println("Java: get lrc_url " + lrc_url);
			return lrc_url;
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JDOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static SongInfo getBaiduLrc2(int songIds) {
        String path = String.format(BAIDU_LRC2_URL_FMT, songIds);
        System.out.println("Java: getBaiduLrc2() url: " + path);
        try {
            URL url = new URL(path);
            URLConnection conn = url.openConnection();
            conn.connect();
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            StringBuffer sb = new StringBuffer();
            while((line = in.readLine()) != null){
                sb.append(line);
            }

            String result = sb.toString();
            System.out.println("Java: result: " + result);

            JSONTokener jsonParser = new JSONTokener(result);
            JSONObject root = (JSONObject) jsonParser.nextValue();
            JSONObject data = root.getJSONObject("data");
            JSONArray songlist = data.getJSONArray("songList");
            int count = songlist.length();
            for (int i=0;i<count;i++) {
                /*queryId: "1262598",
                songId: 1262598,
                songName: "千千阙歌",
                artistId: "11699",
                artistName: "陈慧娴",
                albumId: 197096,
                albumName: "千千阙歌",
                songPicSmall: "http://musicdata.baidu.com/data2/pic/88579378/88579378.jpg",
                songPicBig: "http://musicdata.baidu.com/data2/pic/88579352/88579352.jpg",
                songPicRadio: "http://musicdata.baidu.com/data2/pic/88579342/88579342.jpg",
                lrcLink: "/data2/lrc/240890428/240890428.lrc",
                version: "",
                copyType: 1,
                time: 298,
                linkCode: 22000,
                songLink: "http://file.qianqian.com//data2/music/134380688/134380688.mp3?xcode=1e33e62cd60a750b6a37d80a2c02fc5b&src="http%3A%2F%2Fpan.baidu.com%2Fshare%2Flink%3Fshareid%3D1368254163%26uk%3D2605942610"",
                showLink: "http://pan.baidu.com/share/link?shareid=1368254163&uk=2605942610",
                format: "mp3",
                rate: 128,
                size: 4779264,
                relateStatus: "0",
                resourceType: "2",
                source: "web"*/

                JSONObject song = songlist.getJSONObject(i);
                String queryId = song.getString("queryId");
                int songId = song.getInt("songId");
                String songName = song.getString("songName");
                String artistName = song.getString("artistName");
                String artistId = song.getString("artistId");
                int albumId = song.getInt("albumId");
                String albumName = song.getString("albumName");
                String songPic = song.getString("songPicBig");
                String lrcLink = song.getString("lrcLink");
                String songLink = song.getString("songLink");
                String showLink = song.getString("showLink");
                String format = song.getString("format");
                int rate = song.getInt("rate");
                int size = song.getInt("size");

                //歌曲地址里如果有
                // http://qukufile2.qianqian.com/data2/pic/和
                // http://c.hiphotos.baidu.com/ting/pic/item/
                // 那就需要将http://c.hiphotos.baidu.com/ting/pic/item/给去掉

                //歌词地址：http://qukufile2.qianqian.com+获取到的url
                String lrcPath = BAIDU_LRC_HOST + lrcLink;
                System.out.println(String.format("Java: queryId %s, songId %d, songName %s, " +
                                "artistName %s, artistid %s, albumId %d, albumName %s, " +
                                "lrcPath %s, songLink %s, " +
                                "songPic %s, " +
                                "format %s, bitrate %d, filesize %d",
                        queryId, songId, songName,
                        artistName, artistId, albumId, albumName,
                        lrcPath, songLink,
                        songPic,
                        format, rate, size));
                return new SongInfo(songName, artistName, lrcPath, songPic);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }
	
	public static int getBaiduSongId(String keyword) {
		System.out.println(String.format("Java: getBaiduSongId(): keyword %s", keyword));

		String encoded_str;
		try {
			encoded_str = URLEncoder.encode(keyword, "UTF-8");
			String path = BAIDU_SONG_SEARCH_URL_FMT + encoded_str;

			System.out.println("Java: getBaiduSongId() url: " + path);
            //String html = getHtmlString(path);
            //if (html != null) {
            //}
            org.jsoup.nodes.Document content = Jsoup.parse(new URL(path), 5000);
            Elements divs = content.select("#sc-table");
            org.jsoup.nodes.Document divcontions = Jsoup.parse(divs.toString());
            Elements element = divcontions.getElementsByTag("th");
            for (org.jsoup.nodes.Element song : element) {
                Elements ids = song.getElementsByAttribute("id");
                String str = ids.toString();
                if (!str.isEmpty()) {
                    int pos = str.indexOf("id=");
                    if (pos > 0) {
                        int pos2 = str.indexOf("\"", pos + 4);
                        String id = str.substring(pos + 4, pos2);
                        if (isNumeric(id)) {
                            int songId = Integer.valueOf(id);
                            System.out.println("Java: get lrc id: " + songId);
                            return songId;
                        }
                    }
                }
            }
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
            e.printStackTrace();
        }

        return -1;
	}
	
	public static String getArtist(int artist_id) {
		try {
			String url = API_GECIME_LYRIC_ATRIST + String.valueOf(artist_id);
			System.out.println("Java: getArtist() " + url);
			
			HttpGet request = new HttpGet(url);
			HttpResponse response = HttpClients.createDefault().execute(request);
			if (response.getStatusLine().getStatusCode() != 200) {
				return null;
			}
			
			String result = EntityUtils.toString(response.getEntity());
			System.out.println("Java: result: " + result);
			
			JSONTokener jsonParser = new JSONTokener(result);
			JSONObject root = (JSONObject) jsonParser.nextValue();
			int count = root.getInt("count");
			int code = root.getInt("code");
			if (code != 0) {
				System.out.println("Java: code is wrong " + code);
				return null;
			}
			
			JSONObject artist = root.getJSONObject("result");
			return artist.getString("name");
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	private static boolean isNumeric(String str){
        Pattern pattern = Pattern.compile("[0-9]*");
        Matcher isNum = pattern.matcher(str);
        if( !isNum.matches() ){
            return false;
        }
        return true;
    }
}
