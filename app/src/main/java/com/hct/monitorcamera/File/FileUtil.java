package com.hct.monitorcamera.File;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;

import com.hct.monitorcamera.Util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;

public class FileUtil {
    public static final String MONITOR_CAMERA_ROOT_DIR = "MonitorCamera";
    public static final String INTERNAL_PATH = Environment.getExternalStorageDirectory().getPath();
    public static final long LEFT_SIZE = 1073741824L;//1GB
    private static FileUtil mInstance;
    public static int mCurrentPictureNum = 0;
    private ArrayList<String> mPictureNameList = new ArrayList<>();

    public synchronized static FileUtil getInstance() {
        if (mInstance == null) {
            mInstance = new FileUtil();
        }
        return mInstance;
    }

    public void addPictureNameList(String pictureName) {
        mPictureNameList.add(pictureName);
    }

    public String getPictureNameAtPostion(int position) {
        if (mPictureNameList.size() > 0) {
            return mPictureNameList.get(position);
        }
        return null;
    }

    public ArrayList<String> getPictureNameList() {
        return mPictureNameList;
    }

    public boolean isWritable() {
        if (getAvailableSize() > LEFT_SIZE)
            return true;
        else
            return false;
    }

    public long getTotalSize() {
        if (TextUtils.isEmpty(INTERNAL_PATH))
            return 0;
        try {
            final StatFs statFs = new StatFs(INTERNAL_PATH);
            long blockSize = 0;
            long blockCountLong = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                blockSize = statFs.getBlockSizeLong();
                blockCountLong = statFs.getBlockCountLong();
            } else {
                blockSize = statFs.getBlockSize();
                blockCountLong = statFs.getBlockCount();
            }
            return blockSize * blockCountLong;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public long getAvailableSize() {
        if (TextUtils.isEmpty(INTERNAL_PATH))
            return 0;
        try {
            final StatFs statFs = new StatFs(INTERNAL_PATH);
            long blockSize = 0;
            long availableBlocks = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                blockSize = statFs.getBlockSizeLong();
                availableBlocks = statFs.getAvailableBlocksLong();
            } else {
                blockSize = statFs.getBlockSize();
                availableBlocks = statFs.getAvailableBlocks();
            }
            return availableBlocks * blockSize;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public void savePicture(byte[] data, String pictureName) {
        Util.e("savePicture---------> pictureName = " + pictureName);
        if (TextUtils.isEmpty(pictureName) || data == null) {
            return;
        }
        File root = new File(INTERNAL_PATH + File.separator + MONITOR_CAMERA_ROOT_DIR);
        if (!root.exists())
            root.mkdirs();
        String mImageFilePath = new File(INTERNAL_PATH + File.separator + MONITOR_CAMERA_ROOT_DIR,
                pictureName).getAbsolutePath();
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(mImageFilePath);
            Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
            bmp.compress(Bitmap.CompressFormat.JPEG, 80, out);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public  void writeCaching(String data) {

        Writer writer = null;
        FileOutputStream fos = null;
        File file = new File(INTERNAL_PATH + File.separator + MONITOR_CAMERA_ROOT_DIR);
        if (!file.exists()) {
            file.mkdirs();
        }
        File inputFile = new File(INTERNAL_PATH + File.separator + MONITOR_CAMERA_ROOT_DIR,
                "test.txt");
        if (!inputFile.exists()) {
            try {
                inputFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
        inputFile.setWritable(true);
        inputFile.setReadable(true);

        try {
            fos = new FileOutputStream(inputFile, true);
            writer = new OutputStreamWriter(fos);
            writer.write(data + "\n");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null)
                    writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                if (fos != null)
                    fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
