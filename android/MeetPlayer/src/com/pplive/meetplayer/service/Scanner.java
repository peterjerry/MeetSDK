package com.pplive.meetplayer.service;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Stack;

import com.pplive.meetplayer.util.LocalIoUtil;

import android.util.Log;

public class Scanner implements OnScannedListener<File> {

	private static final String TAG = "ppmedia/Scanner";

	private static Scanner sScanner = null;

	// Singleton
	static synchronized Scanner getInstance() {

		if (sScanner == null) {
			sScanner = new Scanner();
		}
		return sScanner;
	}

	private Scanner() { }

	private OnScannedListener<File> mOnScannedListener;

	public void setOnScannedListener(OnScannedListener<File> listener) {
		mOnScannedListener = listener;
	}

	public void scan(final File fileToScan) {
		scan(fileToScan, null);
	}
	
	public void scan(final File fileToScan, FileFilter filter) {
	    scan(fileToScan, filter, true);
	}

	public void scan(final File fileToScan, FileFilter filter, boolean recursively) {
	    if (LocalIoUtil.isAccessible(fileToScan)){
	        if (fileToScan.isDirectory()){
	            scan(fileToScan.listFiles(filter), filter, recursively);
	        }
	        else {
	            scan(new File[] { fileToScan }, filter, recursively);
	        }
	    }
	}

	public void scan(final File[] filesToScan, FileFilter filter) {
		scan(filesToScan, null, true);
	}

	public void scan(final File[] files, FileFilter filter, boolean recursively) {
		if (files != null && files.length > 0) {
			Stack<File> stack = new Stack<File>();
			stack.addAll(Arrays.asList(files));
			while (!stack.isEmpty()) {
				File file = stack.pop();
				if (!LocalIoUtil.isAccessible(file)) {
					continue;
				}
				
				if (file.isDirectory()) {
				    if (recursively){
	                    File[] subFiles = file.listFiles(filter);
	                    if (subFiles != null) {
	                        stack.addAll(Arrays.asList(subFiles));
	                    }
				    }
				} else if (file.isFile()) {
					onScanned(file);
				}
			}
		}
	}
	
	public void scan(final File[] files, FileFilter filter, int max_depth) {
        scan(files, filter, 0 /* depth */, max_depth);
    }

    public void scan(final File[] files, FileFilter filter, int depth, int max_depth) {
        for (File file : files) {
            scan(file, filter, depth /* depth */, max_depth);
        }
    }

    public void scan(final File file, FileFilter filter, int max_depth) {
        scan(file, filter, 0, max_depth);
    }

    private void scan(final File file, FileFilter filter, int depth, int max_depth) {
        if (!LocalIoUtil.isAccessible(file)) {
            return;
        }
        
        if (file.isDirectory()) {
            if (depth <= max_depth) {
                File[] fileList = file.listFiles(filter);
                if (null != fileList) {
                    scan(fileList, filter, ++depth, max_depth);
                }
            }
        } else if (file.isFile()) {
            Log.d(TAG, "depth: " + depth + " file: " + file.getAbsolutePath());
            onScanned(file);
        }
    }
	
	@Override
	public void onScanned(File f) {
		if (mOnScannedListener != null) {
			mOnScannedListener.onScanned(f);
		}
	}
}
