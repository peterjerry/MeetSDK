/**
 * Copyright (C) 2013 PPTV
 *
 */
package com.gotye.meetsdk.util;

import java.io.File;

import android.text.TextUtils;

/**
 *
 * @author leoxie
 * @version 2013-3-20
 */
public class IoUtil {

	public static boolean delete(String filePath) {
		File f = new File(filePath);
		
		return f.exists() ? f.delete() : true;
	}
	
	public static boolean isAccessible(File file) {
		return file != null && file.exists() && file.canRead();
	}
	
    static boolean makePath(String path) {
        if (TextUtils.isEmpty(path)) {
            return false;
        }
        File dir = new File(path);
        if (!dir.exists()) {
            return dir.mkdirs();
        } else
            return true;
    }

    static boolean makeParentPath(String filename) {
        if (TextUtils.isEmpty(filename)) {
            return false;
        }
        File file = new File(filename);
        return makePath(file.getParentFile().getAbsolutePath());
    }

}
