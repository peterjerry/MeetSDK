package android.pplive.media.player;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class MediaInfo {

	static final String TAG = "player/MediaInfo";
	
	private String mTitle;
	private String mPath;
	private long mDurationMS;
	private long mSizeByte;
	private File mFile;
	
	private String mFormatName;
	private HashMap<Integer, String> mChannels;
	
	private String mVideoCodecName;
	private int mWidth;
	private int mHeight;
	
	// video
	private int mVideoChannels; /// should always be 1?
	private int mThumbnailWidth;
	private int mThumbnailHeight;
	private int mThumbnail[]; // picture data
	
	// audio
	private int mAudioChannels;
	private ArrayList<TrackInfo> audioTrackInfos;
	
	// subtitle
	private int mSubTitleChannels;
	private ArrayList<TrackInfo> subtitleTrackInfos;

	MediaInfo() {
		this("");
	}

	MediaInfo(String s) {
		this(s, 0L, 0L);
	}

	public MediaInfo(String path, long durationMS, long sizeByte) {
		mTitle = getTitleImpl(path);
		mPath = path;
		mDurationMS = durationMS;
		mSizeByte = sizeByte;
		mFile = null;
		mWidth = 0;
		mHeight = 0;
		mFormatName = null;
		mVideoCodecName = null;
		mThumbnailWidth = 0;
		mThumbnailHeight = 0;
		mThumbnail = null;
		mAudioChannels = 0;
		mVideoChannels = 0;
		mSubTitleChannels = 0;
		mChannels = new HashMap<Integer, String>();
		audioTrackInfos = new ArrayList<TrackInfo>();
		subtitleTrackInfos = new ArrayList<TrackInfo>();
	}
	
	// common
	@Deprecated
	public void setChannels(String channelName, int index) {
		mChannels.put(Integer.valueOf(index), channelName);
	}

	@Deprecated
	public HashMap<Integer, String> getChannels() {
		return mChannels;
	}
	
	public String getTitle() {
		return mTitle;
	}
	
	private String getTitleImpl(String path) {
		if (path.startsWith("/")) {
			// local file
			int indexStart, indexEnd;
			indexEnd = path.lastIndexOf('.');
			if (indexEnd != -1) {
				indexStart = path.lastIndexOf('/');
				if (indexStart != -1) // /mnt/sdcard/1/test.mp4
					return path.substring(indexStart + 1, indexEnd);
			}
		}
		
		return "N/A";
	}

	public String getPath() {
		if (mPath == null) {
			mPath = "";
		}
		
		return mPath;
	}

	public long getDuration() {
		return mDurationMS;
	}

	public long getSize() {
		return mSizeByte;
	}

	public File getFile() {
		if (null == mFile) {
			mFile = new File(getPath());
		}
		return mFile;
	}

	public long lastModified() {
		return getFile().lastModified();
	}

	public String getFormatName() {
		return mFormatName;
	}

	// video
	public int getVideoChannels() {
		return mVideoChannels;
	}
	
	public String getVideoCodecName() {
		return mVideoCodecName;
	}
	
	public int getWidth() {
		return mWidth;
	}

	public int getHeight() {
		return mHeight;
	}

	public int getThumbnailWidth() {
		return mThumbnailWidth;
	}

	public int getThumbnailHeight() {
		return mThumbnailHeight;
	}

	public int[] getThumbnail() {
		return mThumbnail;
	}

	public int getAudioChannels() {
		return mAudioChannels;
	}
	
	// audio
	public void setAudioChannelsInfo(int id, int streamIndex, String codecName, String lang, String title) {
		TrackInfo audiotrackinfo = new TrackInfo();
		audiotrackinfo.setId(id);
		audiotrackinfo.setStreamIndex(streamIndex);
		audiotrackinfo.setCodecName(codecName);
		audiotrackinfo.setLanguage(lang);
		audiotrackinfo.setTitle(title);
		audioTrackInfos.add(audiotrackinfo);
	}

	public ArrayList<TrackInfo> getAudioChannelsInfo() {
		return audioTrackInfos;
	}

	// subtitle
	public void setExtenalSubtitleChannelsInfo(String s) {
		if (s == null || !s.endsWith(".srt")) {
			return;
		}
		TrackInfo subtitletrackinfo = new TrackInfo();
		subtitletrackinfo.setId(mSubTitleChannels);
		mSubTitleChannels++;
		subtitletrackinfo.setStreamIndex(-1);
		if (s.endsWith(".srt")) {
			subtitletrackinfo.setCodecName("SubRip");
		}
		subtitleTrackInfos.add(subtitletrackinfo);
	}
	
	public void setSubtitleChannelsInfo(int i, int j, String s, String s1, String s2) {
		TrackInfo subtitletrackinfo = new TrackInfo();
		subtitletrackinfo.setId(i);
		subtitletrackinfo.setStreamIndex(j);
		subtitletrackinfo.setCodecName(s);
		subtitletrackinfo.setLanguage(s1);
		subtitletrackinfo.setTitle(s2);
		subtitleTrackInfos.add(subtitletrackinfo);
	}

	public ArrayList<TrackInfo> getSubtitleChannelsInfo() {
		return subtitleTrackInfos;
	}
	
	public int getSubtitleChannels() {
		return mSubTitleChannels;
	}
	
	@Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        
        sb.append(mTitle).append('|').
        	append(mPath).append('|').
            append(mDurationMS).append('|').
            append(mSizeByte).append('|').
            append(mWidth).append('x').append(mHeight).append('|').
            append(mFormatName).append('|').
            append(mVideoCodecName).append('|');
        if (audioTrackInfos.size() > 0) {
        	for(int i=0;i<audioTrackInfos.size();i++) {
        		sb.append(audioTrackInfos.get(i).getCodecName()).append('(');
        		sb.append(audioTrackInfos.get(i).getTitle()).append(",");
        		sb.append(audioTrackInfos.get(i).getLanguage()).append(")|");
        	}
        }
        if (subtitleTrackInfos.size() > 0) {
        	for(int i=0;i<subtitleTrackInfos.size();i++) {
        		sb.append(subtitleTrackInfos.get(i).getCodecName()).append('(');
        		sb.append(subtitleTrackInfos.get(i).getTitle()).append(",");
        		sb.append(subtitleTrackInfos.get(i).getLanguage()).append(")|");
        	}
        }
            
        return sb.toString();
    }
}
