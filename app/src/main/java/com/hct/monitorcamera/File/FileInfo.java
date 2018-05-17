package com.hct.monitorcamera.File;

public class FileInfo {
    private byte[] mData;
    private String mFileName;

    public FileInfo(byte[] data, String fileName) {
        mData = data;
        mFileName = fileName;
    }

    public byte[] getmData() {
        return mData;
    }

    public void setmData(byte[] mData) {
        this.mData = mData;
    }

    public String getmFileName() {
        return mFileName;
    }

    public void setmFileName(String mFileName) {
        this.mFileName = mFileName;
    }
}
