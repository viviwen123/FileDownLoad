package com.willen.filedownload;


/**
 * Created by willenwu on 2016/11/2.
 */

public interface DownloadListener {
    public void progress(DownloadInfo downloadInfo);
    public void completed(DownloadInfo downloadInfo);
    public void error(DownloadInfo downloadInfo);
    public void pause(DownloadInfo downloadInfo);
    public void pending(DownloadInfo downloadInfo);
}
