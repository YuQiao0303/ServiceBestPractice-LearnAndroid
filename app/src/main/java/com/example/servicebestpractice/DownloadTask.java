package com.example.servicebestpractice;

import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.Request;


/**
 * DownloadTast：下载任务类， 继承AsyncTask，运用异步消息处理机制来处理多线程任务
 * 下载的具体逻辑就在里面
 * 第一个泛型参数指定为String ， 表示在执行AsyncTask的时候需要传入一个字符串参数给后台任务；
 * 第二个泛型参数指定为Integer ， 表示使用整型数据来作为进度显示单位；
 * 第三个泛型参数指定为Integer ， 则表示使用整型数据来反馈执行结果。
 */
public class DownloadTask extends AsyncTask<String, Integer, Integer> {
    //定义四个常量表示下载的状态
    public static final int TYPE_SUCCESS = 0;
    public static final int TYPE_FAILED = 1;
    public static final int TYPE_PAUSED = 2;
    public static final int TYPE_CANCELED = 3;

    private DownloadListener listener;
    private boolean isCanceled = false;
    private boolean isPaused = false;
    private int lastProgress;

    public DownloadTask(DownloadListener listener) {
        this.listener = listener;
    }

    /**
     * 下载逻辑
     * @param params 下载的url地址
     * @return
     */

    @Override
    protected Integer doInBackground(String... params) {
        InputStream is = null;
        RandomAccessFile savedFile = null;
        File file = null;
        try {
            long downloadedLength = 0; // 记录已下载的文件长度
            String downloadUrl = params[0];
            String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/")); //解析下载的文件名
            String directory = Environment.getExternalStoragePublicDirectory
                    (Environment.DIRECTORY_DOWNLOADS).getPath();//下载到SD卡的Download目录下
            file = new File(directory + fileName);
            if (file.exists()) {
                downloadedLength = file.length();
            }
            long contentLength = getContentLength(downloadUrl); //下载文件的总长度
            if (contentLength == 0) {
                return TYPE_FAILED;
            } else if (contentLength == downloadedLength) {
            // 已下载字节和文件总字节相等， 说明已经下载完成了
                return TYPE_SUCCESS;
            }
            //紧接着使用OkHttp来发送一条网络请求， 需要注意的是， 这里在请求中
            //添加了一个header， 用于告诉服务器我们想要从哪个字节开始下载， 因为已下载过的部分就不需
            //要再重新下载了。
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    // 断点下载， 指定从哪个字节开始下载
                    .addHeader("RANGE", "bytes=" + downloadedLength + "-")
                    .url(downloadUrl)
                    .build();
            Response response = client.newCall(request).execute();
            if (response != null) {
                is = response.body().byteStream();
                savedFile = new RandomAccessFile(file, "rw");
                savedFile.seek(downloadedLength); // 跳过已下载的字节
                byte[] b = new byte[1024];int total = 0;
                int len;
                while ((len = is.read(b)) != -1) {
                    if (isCanceled) {
                        return TYPE_CANCELED;
                    } else if(isPaused) {
                        return TYPE_PAUSED;
                    } else {
                        total += len;
                        savedFile.write(b, 0, len);
                        // 计算已下载的百分比
                        int progress = (int) ((total + downloadedLength) * 100 /
                                contentLength);
                        publishProgress(progress);
                    }
                }
                response.body().close();
                return TYPE_SUCCESS;
            }




        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (savedFile != null) {
                    savedFile.close();
                }
                if (isCanceled && file != null) {
                    file.delete();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }return TYPE_FAILED;
    }

    /**
     * 在界面上更新下载进度
     * @param values
     */
    @Override
    protected void onProgressUpdate(Integer... values) {
        int progress = values[0];
        //    private DownloadListener listener;
        //    private int lastProgress;         两个是类的属性
        if (progress > lastProgress) {
            listener.onProgress(progress);
            lastProgress = progress;
        }
    }

    /**
     * 通知最终的下载结果
     * @param status
     */
    @Override
    protected void onPostExecute(Integer status) {
        switch (status) {
            case TYPE_SUCCESS:
                listener.onSuccess();
                break;
            case TYPE_FAILED:
                listener.onFailed();
                break;
            case TYPE_PAUSED:
                listener.onPaused();
                break;
            case TYPE_CANCELED:
                listener.onCanceled();
                break;
            default:break;
        }
    }
    public void pauseDownload() {
        isPaused = true;
    }
    public void cancelDownload() {
        isCanceled = true;
    }
    private long getContentLength(String downloadUrl) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(downloadUrl)
                .build();
        Response response = client.newCall(request).execute();
        if (response != null && response.isSuccessful()) {
            long contentLength = response.body().contentLength();
            response.body().close();
            return contentLength;
        }
        return 0;
    }
}