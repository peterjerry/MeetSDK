package com.gotye.meetplayer.util;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.LinkedList;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

public class LogcatHelper {

    public static final int ACTION_LOGCAT = 1;

    public static final int ACTION_NET_INFO = 2;

    public static final String TAG_LOGCAT = "[LOGCAT_ERROR]";

    private static final String LAUNCHER_LOG_FILE_NAME = "launcher";
    private static final String DEFAULT_ZIP_FILE_NAME = "log";
    private static final String ZIP_LOG_EXTENSION = ".zip";
    private static final String LOG_EXTENSION = ".log";
	private static final String MINI_DUMP_EXTENSION = ".dmp";

    private static LogcatHelper INSTANCE;

    private Context mContext;

    private File savedLog;

    private boolean inited;

    private LinkedList<String> logToWrite;

    private WriteLogThread thread;

    private LogcatHelper() {
    }

    public static LogcatHelper getInstance() {
        if (INSTANCE == null) {
            synchronized (LogcatHelper.class) {
                if (INSTANCE == null) {
                    INSTANCE = new LogcatHelper();
                }
            }
        }
        return INSTANCE;
    }

    public void init(Context context) {
        try {
            this.mContext = context;
            this.logToWrite = new LinkedList<String>();
            this.savedLog = new File(context.getCacheDir() + File.separator
                    + LAUNCHER_LOG_FILE_NAME + LOG_EXTENSION);
            if (!savedLog.exists()) {
                savedLog.createNewFile();
            }
        } catch (IOException e) {
            Log.e(LogcatHelper.TAG_LOGCAT, e.getMessage());
        }
        this.inited = true;
    }

    public void invokeMethod(int action, String value) {
        if (!inited) {
            return;
        }

        switch (action) {
        case ACTION_LOGCAT:
            writeSingleLog(value);
            break;
        case ACTION_NET_INFO:
            asyncWriteSingleLog(value);
            break;
        default:
            break;
        }
    }

    private void writeSingleLog(String value) {

        try {
            synchronized (logToWrite) {
                if (logToWrite.size() > 100) {
                    logToWrite.clear();
                }

                logToWrite.add(value);
                logToWrite.notifyAll();
            }

            if (thread == null) {
                synchronized (WriteLogThread.class) {
                    if (thread == null) {
                        thread = new WriteLogThread();
                        thread.start();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("resource")
    private synchronized void asyncWriteSingleLog(String value) {
        while (true) {
            synchronized (WriteLogThread.class) {
                // if (thread != null) {
                // try {
                // thread.wait(1000);
                // } catch (InterruptedException e) {
                // e.printStackTrace();
                // break;
                // }
                // } else {
                try {
                    FileOutputStream fos = null;
                    BufferedWriter writer = null;
                    fos = new FileOutputStream(savedLog, true);
                    writer = new BufferedWriter(new OutputStreamWriter(fos));
                    writer.write(value);
                    writer.flush();
                    fos.close();
                } catch (IOException e) {
                    break;
                }
                break;
                // }
            }
        }
    }

    private class WriteLogThread extends Thread {

        private FileOutputStream fos = null;
        private BufferedWriter writer = null;

        public WriteLogThread() {
            setName("log_thread");
        }

        @Override
        public void run() {
            while (true) {
                String firstLine = "";
                synchronized (logToWrite) {
                    if (logToWrite.isEmpty()) {
                        try {
                            logToWrite.wait(5000);
                            if (logToWrite.isEmpty()) {
                                break;
                            }
                        } catch (InterruptedException e) {
                            break;
                        }
                    } else {
                        try {
                            firstLine = logToWrite.remove();
                        } catch (Exception e) {
                            break;
                        }
                    }
                }

                try {
                    if (fos == null) {
                        fos = new FileOutputStream(savedLog, true);
                        writer = new BufferedWriter(new OutputStreamWriter(fos));
                    }

                    if (TextUtils.isEmpty(firstLine)) {
                        continue;
                    } else {
                        writer.write(firstLine);
                        writer.newLine();
                        writer.flush();
                    }
                } catch (IOException e) {
                    break;
                }
            }

            synchronized (WriteLogThread.class) {
                closeStream(fos);
                fos = null;
                thread = null;
            }
        }

        private void closeStream(Closeable stream) {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public File zipLogFiles(String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            fileName = DEFAULT_ZIP_FILE_NAME + ZIP_LOG_EXTENSION;
        }
        String zipPath = mContext.getCacheDir() + File.separator + fileName;
        File zipFile = new File(zipPath);
        File[] files = LogcatHelper.getInstance().getLogcatFiles();
        try {
            ZipUtils.zipFiles(files, zipPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return zipFile;
    }

    public void clearLogFiles() throws IOException {
        File[] files = getLogcatFiles();
        for (File f : files) {
            f.delete();
            f.createNewFile();
        }
    }

    public File[] getLogcatFiles() {
        File fileDir = new File(mContext.getCacheDir().getAbsolutePath());
        FilenameFilter filter = new FilenameFilter() {

            @Override
            public boolean accept(File dir, String filename) {
                return (new File(mContext.getCacheDir().getAbsolutePath(),
                        filename).isFile())
                        && (filename.endsWith(LOG_EXTENSION) || 
							filename.endsWith(MINI_DUMP_EXTENSION) || 
							filename.contains("PeerLog") || filename.contains("deviceinfo"));
            }
        };

        return fileDir.listFiles(filter);
    }

}
