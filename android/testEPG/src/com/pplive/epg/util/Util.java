package com.pplive.epg.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class Util {
	
	public static boolean httpDownload(String httpUrl,String saveFile){
        // 下载网络文件
        int bytesum = 0;
        int byteread = 0;

        URL url = null;
		try {
			url = new URL(httpUrl);
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return false;
		}

        try {
            URLConnection conn = url.openConnection();
            conn.setRequestProperty("RANGE", "bytes=1400-"); 
            InputStream inStream = conn.getInputStream();
            FileOutputStream fs = new FileOutputStream(saveFile);

            byte[] buffer = new byte[1024];
            while ((byteread = inStream.read(buffer)) != -1) {
                bytesum += byteread;
                //System.out.println(bytesum);
                fs.write(buffer, 0, byteread);
            }
            
            System.out.println(String.format("file: %s, download done, size %d, save to %s",
            		httpUrl, bytesum, saveFile));
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

	public static String readFileContent(String fileName) {
		BufferedReader bf = null;
		StringBuilder sb = new StringBuilder();
		
		try {
			File file = new File(fileName);
			bf = new BufferedReader(new FileReader(file));
			String content = "";
			while(content != null) {
				content = bf.readLine();
				if (content == null)
					break;
				
				sb.append(content.trim());
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			if (bf != null) {
				try {
					bf.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		return sb.toString(); 
	}
}
