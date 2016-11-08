package com.willen.download;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.willen.filedownload.DownloadInfo;
import com.willen.filedownload.DownloadListener;
import com.willen.filedownload.FileDownloadManager;

import java.io.File;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity {
    FileDownloadManager downloadManager = new FileDownloadManager();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startDownload();
    }

    @Override
    protected void onDestroy() {
        downloadManager.release(this);
        super.onDestroy();
    }

    public void startDownload(){
        downloadManager.init(this);
        final DownloadInfo downloadInfo = new DownloadInfo();
        downloadInfo.url = "https://github.com/Tencent/tinker/archive/master.zip";
        String sdcard = Environment.getExternalStorageDirectory().getAbsolutePath();
        String folder = sdcard + File.separator + "download";
        downloadInfo.path = folder + File.separator + "tinker.zip";
        downloadManager.startDownload(downloadInfo, listener);
    }

    private DownloadListener listener = new DownloadListener() {
        @Override
        public void progress(DownloadInfo downloadInfo) {
            Log.i(TAG, String.format("progress %d/%d %d", downloadInfo.soFarBytes, downloadInfo.totalBytes,downloadInfo.status));
        }

        @Override
        public void completed(DownloadInfo downloadInfo) {
            Log.i(TAG, String.format("completed %d/%d %d\n %s\n %s", downloadInfo.soFarBytes, downloadInfo.totalBytes,downloadInfo.status
                    ,downloadInfo.path,downloadInfo.url));
        }

        @Override
        public void error(DownloadInfo downloadInfo) {
            Log.i(TAG, String.format("error %d/%d %d", downloadInfo.soFarBytes, downloadInfo.totalBytes,downloadInfo.status));
        }

        @Override
        public void pause(DownloadInfo downloadInfo) {
            Log.i(TAG, String.format("pause %d/%d %d", downloadInfo.soFarBytes, downloadInfo.totalBytes,downloadInfo.status));
        }

        @Override
        public void pending(DownloadInfo downloadInfo) {
            Log.i(TAG, String.format("pending %d/%d %d", downloadInfo.soFarBytes, downloadInfo.totalBytes,downloadInfo.status));
        }
    };
}
