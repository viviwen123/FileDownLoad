package com.willen.filedownload;

import android.app.DownloadManager;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by willenwu on 2016/11/2.
 */

public class FileDownloadManager {
    private static final String TAG = "FileDownloadManager";
    private static final Uri CONTENT_URI = Uri.parse("content://downloads/my_downloads");

    private DownloadChangeObserver downloadObserver;
    private DownloadManager downloadManager;

    private Map<Long, List<DownloadListener>> mListeners = new ConcurrentHashMap<>();
    private Map<String, Long> mUrl2Id = new ConcurrentHashMap<>();
    private Map<Long, DownloadInfo> mId2DownloadInfo = new ConcurrentHashMap<>();

    private int soFarIdx = -1, totalIdx, statusIdx, urlIdx, pathIdx, reasonIdx;

    public long startDownload(DownloadInfo downloadInfo, DownloadListener listener) {
        if (downloadManager == null) {
            Log.w(TAG, "downloadManager is null, you should init first!");
            return -1;
        }
        if (downloadInfo == null || TextUtils.isEmpty(downloadInfo.url) || TextUtils.isEmpty(downloadInfo.path)){
            Log.w(TAG, "downloadInfo invalid!");
            return -1;
        }
        if (isDownLoading(downloadInfo.url) && !downloadInfo.forceDownload) {
            Log.w(TAG, "The given url is downloading!");
            return mUrl2Id.get(downloadInfo.url);
        }
        FileUtil.mkParentDir(downloadInfo.path);
        if (FileUtil.fileExists(downloadInfo.path)){
            FileUtil.deleteFile(downloadInfo.path);
        }
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadInfo.url));
            request.setDestinationUri(Uri.fromFile(new File(downloadInfo.path)));
            request.setNotificationVisibility(downloadInfo.notificationVisibility);
            long downloadId = downloadManager.enqueue(request);
            addUrl2IdMap(downloadInfo.url, downloadId);
            addId2DownloadInfoMap(downloadId,downloadInfo);
            addCallbackListener(downloadId, listener);
            return downloadId;
        }catch (IllegalArgumentException e){
            Log.e(TAG,e.getMessage());
        }catch (SecurityException e){
            Log.e(TAG,e.getMessage());
        }catch (Exception e){
            Log.e(TAG,e.getMessage());
        }
        return -1;
    }

    public int removeDownload(long id) {
        if (downloadManager == null) {
            Log.i(TAG, "downloadManager is null, you should init first!");
            return -1;
        }

        return downloadManager.remove(id);
    }

    public void init(Context context) {
        downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        downloadObserver = new DownloadChangeObserver(null);
        context.getContentResolver().registerContentObserver(CONTENT_URI, true,
                downloadObserver);
    }

    public void release(Context context) {
        context.getContentResolver().unregisterContentObserver(downloadObserver);
    }

    public DownloadInfo getDownloadInfo(long downloadId) {
        if (downloadManager == null) {
            Log.i(TAG, "downloadManager is null, you should init first!");
            return null;
        }
        DownloadInfo downloadInfo = mId2DownloadInfo.containsKey(downloadId) ? mId2DownloadInfo.get(downloadId) :  new DownloadInfo();
        downloadInfo.id = downloadId;
        DownloadManager.Query query = new DownloadManager.Query().setFilterById(downloadId);
        Cursor c = null;
        try {
            c = downloadManager.query(query);
            iniCursorIdx(c);
            if (c != null && c.moveToFirst()) {
                downloadInfo.soFarBytes = c.getInt(soFarIdx);
                downloadInfo.totalBytes = c.getInt(totalIdx);
                downloadInfo.status = c.getInt(statusIdx);
                downloadInfo.path = c.getString(pathIdx);//use Uri.parse(path).getPath(); can get the real path
                downloadInfo.url = c.getString(urlIdx);
                if (downloadInfo.status == DownloadManager.STATUS_PAUSED || downloadInfo.status == DownloadManager.STATUS_FAILED) {
                    downloadInfo.reason = c.getString(reasonIdx);
                }else {
                    downloadInfo.reason = "";
                }
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return downloadInfo;
    }

    public void addCallbackListener(Long id, DownloadListener listener) {
        if (!mListeners.containsKey(id)) {
            mListeners.put(id, new LinkedList<DownloadListener>());
        }
        mListeners.get(id).add(listener);
    }

    public void removeCallbackListener(Long id, DownloadListener listener) {
        if (!mListeners.containsKey(id)) {
            return;
        }
        mListeners.get(id).remove(listener);
    }

    public void removeCallbackListener(Long id) {
        if (!mListeners.containsKey(id)) {
            return;
        }
        mListeners.remove(id);
    }

    public void addUrl2IdMap(String url, Long id) {
        mUrl2Id.put(url, id);
    }

    public void removeUrl(String url) {
        if (mUrl2Id.containsKey(url)) {
            mUrl2Id.remove(url);
        }
    }

    public void addId2DownloadInfoMap(Long id, DownloadInfo info) {
        mId2DownloadInfo.put(id, info);
    }

    public void removeId(long id) {
        if (mId2DownloadInfo.containsKey(id)) {
            mId2DownloadInfo.remove(id);
        }
    }

    public boolean isDownLoading(String url) {
        if (!mUrl2Id.containsKey(url)) {
            return false;
        }
        Long id = mUrl2Id.get(url);
        if (id == null) {
            return false;
        }
        DownloadInfo info = getDownloadInfo(id);
        if (info == null) {
            return false;
        }
        if (info.status == DownloadManager.STATUS_FAILED || info.status == DownloadManager.STATUS_SUCCESSFUL) {
            removeCallbackListener(id);
            removeUrl(url);
            removeId(id);
            return false;
        }
        if (info.status == DownloadManager.STATUS_RUNNING) {
            return true;
        }
        return false;
    }

    class DownloadChangeObserver extends ContentObserver {

        public DownloadChangeObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            String path = uri.toString();
            if (path == null || path.length() <= CONTENT_URI.toString().length()) {
                return;
            }
            String id = path.substring(path.lastIndexOf(File.separator) + 1);
            if (TextUtils.isEmpty(id)) {
                return;
            }
            try {
                doCallBack(Long.parseLong(id));
            }catch (NumberFormatException e){
                Log.e(TAG, e.getMessage());
            }
        }

    }

    private void iniCursorIdx(Cursor c) {
        if (soFarIdx == -1) {
            soFarIdx = c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
            totalIdx = c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
            statusIdx = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
            pathIdx = c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
            urlIdx = c.getColumnIndex(DownloadManager.COLUMN_URI);
            reasonIdx = c.getColumnIndex(DownloadManager.COLUMN_REASON);
        }
    }

    private void doCallBack(long id) {
        DownloadInfo info = getDownloadInfo(id);
        if (info == null) {
            return;
        }
        if (info.totalBytes <= 0) {
            return;
        }
        boolean shouldRemove = false;
        if (!mListeners.containsKey(info.id)) {
            return;
        }
        List<DownloadListener> listeners = mListeners.get(info.id);
        if (listeners == null) {
            return;
        }
        for (DownloadListener listener : listeners) {
            if (listener == null){
                continue;
            }
            switch (info.status) {
                case DownloadManager.STATUS_FAILED:
                    listener.error(info);
                    shouldRemove = true;
                    break;
                case DownloadManager.STATUS_PAUSED:
                    listener.pause(info);
                    break;
                case DownloadManager.STATUS_PENDING:
                    listener.pending(info);
                    break;
                case DownloadManager.STATUS_RUNNING:
                    listener.progress(info);
                    break;
                case DownloadManager.STATUS_SUCCESSFUL:
                    listener.completed(info);
                    shouldRemove = true;
                    break;
            }
        }
        if (shouldRemove) {
            removeCallbackListener(info.id);
            removeUrl(info.url);
            removeId(id);
        }
    }

}
