package com.willen.filedownload;

import android.util.Log;

import java.io.File;

public class FileUtil {
    private static final String TAG = "FileOperation";

    public static void mkParentDir(String path) {
        if (path == null) {
            return;
        }
        File file = new File(path);
        File pFile = file.getParentFile();
        if (!pFile.exists()) {
            if (!pFile.mkdirs() || !pFile.isDirectory()) {
                Log.e(TAG, "mkParentDir mkdir error:" + path);
            }
        }
    }

    public static final boolean fileExists(String filePath) {
        if (filePath == null) {
            return false;
        }

        File file = new File(filePath);
        if (file.exists())
            return true;
        return false;
    }

    public static boolean deleteFile(String filePath) {
        if (filePath == null) {
            return true;
        }

        File file = new File(filePath);
        if (file.exists()) {
            return file.delete();
        }
        return true;
    }

}
