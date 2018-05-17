package com.hct.monitorcamera.File;

import java.util.concurrent.LinkedBlockingQueue;

public class FileSave {
    private static FileSave mInstance;
    private boolean mStop = false;
    private boolean mStart = false;
    protected LinkedBlockingQueue<FileInfo> mSaveFileQueue =
            new LinkedBlockingQueue<FileInfo>();
    private FileRunnable mFileRunnable;
    private Thread mFileThread;

    public synchronized static FileSave getInstance() {
        if (mInstance == null) {
            mInstance = new FileSave();
        }
        return mInstance;
    }

    public synchronized void startThread() {
        if (mStart)
            return;
        mStart = true;
        mStop = false;
        mFileRunnable = new FileRunnable();
        mFileThread = new Thread(mFileRunnable, "FILE_SAVE");
        synchronized (mFileThread) {
            mFileThread.start();
        }
    }

    public synchronized void stopThread() {
        mStop = true;
        mStart = false;
        if (mFileThread != null)
            mFileThread.interrupt();
        mFileThread = null;
    }

    public void addFileToQueue(FileInfo fileInfo) {
        if (fileInfo != null)
            mSaveFileQueue.add(fileInfo);
    }

    private FileInfo takeFileFromQueue() {
        try {
            return mSaveFileQueue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    class FileRunnable implements Runnable {

        @Override
        public void run() {
            while (!mStop) {
                FileInfo fileInfo = takeFileFromQueue();
                if (fileInfo != null)
                    FileUtil.getInstance().savePicture(fileInfo.getmData(), fileInfo.getmFileName());
            }
        }
    }
}
