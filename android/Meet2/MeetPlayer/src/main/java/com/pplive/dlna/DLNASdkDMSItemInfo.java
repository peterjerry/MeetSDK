package com.pplive.dlna;

public class DLNASdkDMSItemInfo
{
	public String objectId;
    public String parentObjectId;
    public String titleName;
    public boolean isDirectory;
    public long fileSize;
    public String downloadUrl;
	public long fileType; // 0 folder, 1 video, 2 audio, 3 image
    public DLNASdkDMSItemInfo()
    {
    }
    public String toString()
    {
        return "{"+
                "\"objectId\": \"" + objectId + "\"" +
                ",\"parentObjectId\": \"" + parentObjectId + "\"" +
                ",\"titleName\": \"" + titleName + "\"" +
                ",\"isDirectory\": \"" + (isDirectory?"true":"false") + "\"" +
                ",\"fileSize\": \"" + fileSize + "\"" +
                ",\"downloadUrl\": \"" + downloadUrl + "\"" +
				",\"fileType\": \"" + fileType + "\"" +
                "}";
    }
}
