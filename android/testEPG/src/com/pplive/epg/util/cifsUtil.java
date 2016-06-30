package com.pplive.epg.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbFileOutputStream;

public class cifsUtil {
	public static void main(String[] args) { 
		cifsUtil test = new cifsUtil();
		String src_path = "smb://michael:123456@192.168.1.118/html/crash/";
		String filename = "index.php";
		String save_folder = "E://aaa/";
		
		test.smbList(src_path);
        if (test.smbGet(src_path + filename, save_folder))
        	System.out.println("file saved: " + src_path);
        else
        	System.out.println("file failed to saved: " + src_path);
        
        if (test.smbPut(src_path, save_folder + "avcodec-56.dll"))
        	System.out.println("file saved: " + src_path);
        else
        	System.out.println("file failed to saved: " + src_path);
	}
	
	
	public void smbList(String remoteUrl) {
        try  {  
            SmbFile remoteFile = new SmbFile(remoteUrl);
            remoteFile.connect();
            SmbFile[] files = remoteFile.listFiles();
            for (int i=0;i<files.length;i++) {
            	SmbFile f = files[i];
            	System.out.println(String.format("%s %s %d",
                		(f.isDirectory() ? "D" : "F"),
                		f.getName(),
                		f.getContentLength()));
            }
           
        }  
        catch (Exception e) {  
            e.printStackTrace();  
        }  
	}
        
	
	/** 
     * 从共享目录拷贝文件到本地 
     * @param remoteUrl 共享目录上的文件路径 
     * @param localDir 本地目录 
     */  
    public boolean smbGet(String remoteUrl, String localDir)  
    {  
        InputStream in = null;  
        OutputStream out = null;  
        try  
        {  
            SmbFile remoteFile = new SmbFile(remoteUrl);
            remoteFile.connect();
            
            String fileName = remoteFile.getName();  
            File localFile = new File(localDir + File.separator + fileName);  
            in = new BufferedInputStream(new SmbFileInputStream(remoteFile));  
            out = new BufferedOutputStream(new FileOutputStream(localFile));  
            byte[] buffer = new byte[1024];
            int ret;
            long total_size = 0;
            while ((ret = in.read(buffer)) != -1) {
                out.write(buffer, 0, ret);
                total_size += ret;
            }
            
            System.out.println("total file size: " + total_size);
            return true;
        }  
        catch (Exception e) {  
            e.printStackTrace();  
        }  
        finally {  
            try {  
                out.close();  
                in.close();  
            }  
            catch (IOException e) {  
                e.printStackTrace();  
            }  
        }
        
        return false;
    }  
  
    /** 
     * 从本地上传文件到共享目录 
     * @Version1.0 Sep 25, 2009 3:49:00 PM 
     * @param remoteUrl 共享文件目录 
     * @param localFilePath 本地文件绝对路径 
     */  
    public boolean smbPut(String remoteUrl, String localFilePath)  
    {  
        InputStream in = null;  
        OutputStream out = null;  
        try {  
            File localFile = new File(localFilePath);  
  
            String fileName = localFile.getName();  
            SmbFile remoteFile = new SmbFile(remoteUrl + "/" + fileName);  
            in = new BufferedInputStream(new FileInputStream(localFile));  
            out = new BufferedOutputStream(new SmbFileOutputStream(remoteFile));  
            byte[] buffer = new byte[1024];  
            int read;
            long total_write = 0;
            while ((read = in.read(buffer)) != -1) {  
                out.write(buffer, 0, read);  
                total_write += read;
            }
            
            System.out.println("total write size: " + total_write);
            return true;
        }  
        catch (Exception e)  
        {  
            e.printStackTrace();  
        }  
        finally {  
            try  {  
                out.close();  
                in.close();  
            }  
            catch (IOException e) {  
                e.printStackTrace();  
            }  
        }
        
        return false;
    }  
}
