package com.willen.filedownload;

import android.app.DownloadManager;

/**
 * Created by willenwu on 2016/11/2.
 */

public class DownloadInfo {
    public long id;
    public int soFarBytes = 0;
    public int totalBytes = -1;
    public int status = 0;
    public int notificationVisibility = DownloadManager.Request.VISIBILITY_HIDDEN;
    public String url;
    public String path;
    public String reason;
    public boolean forceDownload = false;
}
