package com.pplive.db; 

public class MediaInfoEntry {  
    public int _id;  
    public String path;
    public String title;  
    public int duration; // msec  
    public int size;
    public int mime_type;
    public int last_play_pos; // msec
      
    public MediaInfoEntry() {
    	
    }
    
    public MediaInfoEntry(int id, String path, String title) {
    	this(id, path, title, 0, 0, 0, 0);
    }  
      
    public MediaInfoEntry(int id, String path, String title, 
    		int duration, int size, int mime_type, int play_pos) {  
        this._id			= id;  
        this.path 			= path;
        this.title 			= title;
        this.duration 		= duration;
        this.size 			= size;
        this.mime_type 		= mime_type;
        this.last_play_pos 	= play_pos;
    }  
}  