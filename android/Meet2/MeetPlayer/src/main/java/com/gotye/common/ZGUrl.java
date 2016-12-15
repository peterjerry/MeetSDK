package com.gotye.common;

/**
 * Created by Michael.Ma on 2016/6/20.
 */
public class ZGUrl {
    public String title;
    public String vid;
    public String file_type;
    public String urls;
    public String durations;

    public ZGUrl(String vid, String type, String urls, String durations) {
        this("", vid, type, urls, durations);
    }

    public ZGUrl(String title, String vid, String type, String urls, String durations) {
        this.title      = title;
        this.vid        = vid;
        this.file_type  = type;
        this.urls       = urls;
        this.durations  = durations;
    }
}
