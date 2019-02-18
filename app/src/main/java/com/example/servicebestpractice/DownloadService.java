package com.example.servicebestpractice;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import java.io.File;

/**
 * 下载的服务
 */
public class DownloadService extends Service {
    private DownloadTask downloadTask;
    private String downloadUrl;
    /**
     * 监听接口
     */
    private DownloadListener listener = new DownloadListener() {
        @Override
        public void onProgress(int progress) {
            // 构建了一个用于显示下载进度的通知
            // 然后调用NotificationManager的notify() 方法去触发这个通知，
            // 这样就可以在下拉状态栏中实时看到当前下载的进度了
            getNotificationManager().notify(1, getNotification("Downloading...",
                    progress));
        }
        @Override
        public void onSuccess() {
            downloadTask = null;
            // 下载成功时将前台服务通知关闭， 并创建一个下载成功的通知
            stopForeground(true);
            getNotificationManager().notify(1, getNotification("Download Success",
                    -1));
            Toast.makeText(DownloadService.this, "Download Success",
                    Toast.LENGTH_SHORT).show();
        }
        @Override
        public void onFailed() {
            downloadTask = null;
            // 下载失败时将前台服务通知关闭， 并创建一个下载失败的通知
            stopForeground(true);
            getNotificationManager().notify(1, getNotification("Download Failed",-1));
            Toast.makeText(DownloadService.this, "Download Failed",
                    Toast.LENGTH_SHORT).show();
        }
        @Override
        public void onPaused() {
            downloadTask = null;
            Toast.makeText(DownloadService.this, "Paused", Toast.LENGTH_SHORT).
                    show();
        }
        @Override
        public void onCanceled() {
            downloadTask = null;
            stopForeground(true);
            Toast.makeText(DownloadService.this, "Canceled", Toast.LENGTH_SHORT).
                    show();
        }
    };

    private DownloadBinder mBinder = new DownloadBinder();
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    class DownloadBinder extends Binder {
        /**
         * 开启downloadTask线程，显示开始下载通知等
         * @param url
         */
        public void startDownload(String url) {
            if (downloadTask == null) {
                downloadUrl = url;
                downloadTask = new DownloadTask(listener);
                downloadTask.execute(downloadUrl);
                startForeground(1, getNotification("Downloading...", 0));
                Toast.makeText(DownloadService.this, "Downloading...", Toast.
                        LENGTH_SHORT).show();
            }
        }
        public void pauseDownload() {
            if (downloadTask != null) {
                downloadTask.pauseDownload();
            }
        }
        public void cancelDownload() {
            if (downloadTask != null) {
                downloadTask.cancelDownload();
            } else {
                if (downloadUrl != null) {
                    // 取消下载时需将文件删除， 并将通知关闭
                    String fileName = downloadUrl.substring(downloadUrl.
                            lastIndexOf("/"));
                    String directory = Environment.getExternalStoragePublicDirectory
                            (Environment.DIRECTORY_DOWNLOADS).getPath();
                    File file = new File(directory + fileName);
                    if (file.exists()) {
                        file.delete();
                    }
                    getNotificationManager().cancel(1);
                    stopForeground(true);Toast.makeText(DownloadService.this, "Canceled",
                            Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    private NotificationManager getNotificationManager() {
        return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }
    private Notification getNotification(String title, int progress) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            /*create notification channel*/
            String channelId = "1";
            String channelName = "1";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            createNotificationChannel(channelId, channelName, importance);

            /*show the notification*/
            Notification notification = new NotificationCompat.Builder(this, channelId)
                    .setContentTitle("This is content title")
                    .setContentText("This is content text")
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.mipmap.ic_launcher))
                    .setContentIntent(pi)
                    .build();
            startForeground(1, notification);//调用startForeground() 方法后就会让MyService变成一个前台服务， 并在系统状态栏显示出来。
        }
        /**
         * 7.0及以下：前台服务
         */
        else{
            Notification notification = new NotificationCompat.Builder(this)
                    .setContentTitle("This is content title")
                    .setContentText("This is content text")
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.mipmap.ic_launcher))
                    .setContentIntent(pi)
                    .build();
            startForeground(1, notification);
        }



        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(),
                R.mipmap.ic_launcher));
        builder.setContentIntent(pi);
        builder.setContentTitle(title);
        if (progress >= 0) {
            // 当progress大于或等于0时才需显示下载进度
            builder.setContentText(progress + "%");
            //setProgress() 方法接收3个参数，
            // 第一个参数传入通知的最大进度，
            // 第二个参数传入通知的当前进度，
            // 第三个参数表示是否使用模糊进度条， 这里传入false 。
            // 设置完setProgress() 方法， 通知上就会有进度条显示出来了。
            builder.setProgress(100, progress, false);
        }
        return builder.build();
    }
    @TargetApi(Build.VERSION_CODES.O)
    private void createNotificationChannel(String channelId, String channelName, int importance) {
        NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
        NotificationManager notificationManager = (NotificationManager) getSystemService(
                NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(channel);
    }
}