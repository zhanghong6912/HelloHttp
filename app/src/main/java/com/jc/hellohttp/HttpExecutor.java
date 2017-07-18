/*
 * Copyright 2017 zhanghong6912@outlook.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jc.hellohttp;

import android.graphics.Bitmap;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

/**
 * Created by Zhang on 2017/7/10.<br/>
 * Description: http请求的执行者，默认从http请求等候区中按照FIFO的规则取出http请求并执行，得到响应后通过回调的方式将结果投递到主线程
 */
class HttpExecutor extends Thread {

    private static final String TAG = HttpExecutor.class.getSimpleName();

//    private boolean shouldLoop = true;

//    private static final Object mDecodeLock = new Object();

    private boolean interrupted = false;

    private Handler mHandler;
    /**
     * HttpURLConnection
     */
    private HttpURLConnection mConnection;
    /**
     * 请求等候区，内部封装了无边界的阻塞队列
     */
    private RequestWaitingArea mWaitingArea;

    public HttpExecutor(RequestWaitingArea area) {
        mHandler = new Handler(Looper.getMainLooper());
        mWaitingArea = area;
    }

    @Override
    public void run() {
        while (!interrupted) {
            Request request;
            try {
                request = mWaitingArea.getRequestsFromWaitingArea().take();
            } catch (InterruptedException e) {
//                e.printStackTrace();
                if (interrupted) {
                    Log.i(TAG, TAG + ": thread id = " + Thread.currentThread().getId() + " has been interrupted...");
                    return;
                }
                continue;
            }
            switch (request.getRequestMethod()) {
                case GET:
                    performGetRequest(request);
                    break;
                case POST:
                    performPostRequest(request);
                    break;
            }
        }
//        while (shouldLoop) {
//            if (!mWaitingArea.getRequestsFromWaitingArea().isEmpty()) {
//                Request request = mWaitingArea.getRequestsFromWaitingArea().remove(0);
//                switch (request.getRequestMethod()) {
//                    case GET:
//                        performGetRequest(request);
//                        break;
//                    case POST:
//                        performPostRequest(request);
//                        break;
//                }
//            } else {
//                synchronized (mSynLock) {
//                    try {
//                        Log.i("HttpExecutor", "Waiting area: Thread id = " + Thread.currentThread().getId() + " has no request, thread is waiting...");
//                        mSynLock.wait();
//                    } catch (InterruptedException e) {
//                        if (!shouldLoop || Config.quit) {
//                            Log.i("HttpExecutor", "Waiting area: Thread id = " + this.getId() + ", exiting loop...");
//                            return;
//                        }
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }
    }

    private void performGetRequest(Request request) {
        try {
            mConnection = (HttpURLConnection) new URL(request.getUrl()).openConnection();
            mConnection.setRequestMethod("GET");
            mConnection.setConnectTimeout(Config.CONNECT_TIMEOUT);
            mConnection.setReadTimeout(Config.READ_TIMEOUT);
            if (mConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                if (!interrupted) {
                    switch (request.getRequestType()) {
                        case STRING:
                        case JSON:
                            String response = responseToString(mConnection.getInputStream());
                            postResponse(response, request.getCallback());
                            break;
                        case IMAGE:
                            try {
                                Bitmap bitmap = HelloHttp.getCompressedBitmap(mConnection.getInputStream(),
                                        request.getBmpWidth(), request.getBmpHeight(), request.getBitmapConfig());
                                postResponse(bitmap, request.getCallback());
                            } catch (OutOfMemoryError error) {
                                throw new RuntimeException("OutOfMemoryError caught! request url: " + request.getUrl());
                            }
                            break;
                        case DOWNLOAD:
                            String fileName = request.getUrl().substring(request.getUrl().lastIndexOf("/") + 1);
                            File downloadFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
                            if (downloadFile.exists()) {
                                if (downloadFile.isFile() && downloadFile.delete()) {
                                    Log.i(TAG, "File:" + fileName + "already exists, delete and downloadReq again...");
                                }
                            }
                            FileOutputStream outputStream = new FileOutputStream(downloadFile);
                            InputStream fileInput = mConnection.getInputStream();
                            byte[] buffer = new byte[1024 * 8];
                            int length = 0;
                            while ((length = fileInput.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, length);
                                outputStream.flush();
                            }
                            outputStream.close();
                            postResponse("downloadReq success", request.getCallback());
                            break;
                        case UPLOAD:
                            throw new IllegalStateException("Cannot upload file through \'GET\' request");
//                            break;
                    }
                } else {
                    Log.e(TAG, "Request: \'url = " + request.getUrl() + "\' has been interrupted...");
                    //noinspection UnnecessaryReturnStatement
                    return;
                }
            } else {
                String errorCode = String.valueOf(mConnection.getResponseCode());
                handleError(errorCode, request.getCallback());
            }
        } catch (IOException e) {
            e.printStackTrace();
            handleError(e.getMessage(), request.getCallback());
        }
    }

    private void performPostRequest(Request request) {
        try {
            mConnection = (HttpURLConnection) new URL(request.getUrl()).openConnection();
            mConnection.setRequestMethod("POST");
            mConnection.setConnectTimeout(Config.CONNECT_TIMEOUT);
            mConnection.setReadTimeout(Config.READ_TIMEOUT);
            mConnection.setDoOutput(true);
            mConnection.setDoInput(true);
            switch (request.getRequestType()) {
                case STRING:
                    mConnection.setRequestProperty("Content-Type", Config.STRING_REQ_PROP);
                    break;
                case JSON:
                    mConnection.setRequestProperty("Content-Type", Config.JSON_REQ_PROP);
                    break;
                case IMAGE:
//                    mConnection.setRequestProperty("Content-Type", Config.STRING_REQ_PROP);
                    break;
                case DOWNLOAD:
                    break;
                case UPLOAD:
                    // TODO: 2017/7/14
                    break;
            }
            if (request.getRequestParams() != null && !request.getRequestParams().isEmpty()) {
                String paramsStr = encodeParams(request.getRequestParams(), Config.PARAMS_ENCODING);
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(mConnection.getOutputStream()));
                writer.write(paramsStr);

            }
            if (mConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                if (!interrupted) {
                    switch (request.getRequestType()) {
                        case STRING:
                        case JSON:
                            String response = responseToString(mConnection.getInputStream());
                            postResponse(response, request.getCallback());
                            break;
                        case IMAGE:
                            try {
                                Bitmap bitmap = HelloHttp.getCompressedBitmap(mConnection.getInputStream(),
                                        request.getBmpWidth(), request.getBmpHeight(), request.getBitmapConfig());
                                postResponse(bitmap, request.getCallback());
                            } catch (OutOfMemoryError error) {
                                throw new RuntimeException("OutOfMemoryError caught! request url: " + request.getUrl());
                            }
                            break;
                        case DOWNLOAD:
                            // 强制设置为低优先级
                            request.setPriority(Request.Priority.LOW);
                            String fileName = request.getUrl().substring(request.getUrl().lastIndexOf("/") + 1);
                            File downloadFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
                            if (downloadFile.exists()) {
                                if (downloadFile.isFile() && downloadFile.delete()) {
                                    Log.i(TAG, "File:" + fileName + "already exists, delete and downloadReq again...");
                                }
                            }
                            FileOutputStream outputStream = new FileOutputStream(downloadFile);
                            InputStream fileInput = mConnection.getInputStream();
                            byte[] buffer = new byte[1024 * 8];
                            int length = 0;
                            while ((length = fileInput.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, length);
                                outputStream.flush();
                            }
                            outputStream.close();
                            postResponse("Download success. Download request is deprecated, if you want to download file(s), please use \'android.app.DownloadManager\' instead."
                                    , request.getCallback());
                            break;
                        case UPLOAD:
                            // TODO: 2017/7/14
                            break;
                    }
                } else {
                    Log.e(TAG, "Request: \'url = " + request.getUrl() + "\' has been interrupted...");
                    //noinspection UnnecessaryReturnStatement
                    return;
                }
            } else {
                String errorCode = String.valueOf(mConnection.getResponseCode());
                handleError(errorCode, request.getCallback());
            }
        } catch (IOException e) {
            e.printStackTrace();
            handleError(e.getMessage(), request.getCallback());
        }
    }

    private void postResponse(final Object response, final RequestCallback callback) {
        if (callback != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onSuccess(response);
                }
            });
        }
    }

    private void handleError(final String error, final RequestCallback callback) {
        if (callback != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (error == null) {
                        callback.onError("Http request error");
                    } else {
                        if (TextUtils.isDigitsOnly(error)) {
                            callback.onError("Http request error, errorCode: " + error);
                        } else {
                            callback.onError("Http request error, error message: " + error);
                        }
                    }
                }
            });
        }
    }

    private String responseToString(InputStream inputStream) throws IOException {
        if (inputStream != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line = "";
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            inputStream.close();
            return stringBuilder.toString();
        } else {
            return null;
        }
    }

    /**
     * 将请求参数转换为String
     */
    private String encodeParams(Map<String, String> params, String paramsEncoding) {
        StringBuilder encodedParams = new StringBuilder();
        try {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                encodedParams.append(URLEncoder.encode(entry.getKey(), paramsEncoding));
                encodedParams.append('=');
                encodedParams.append(URLEncoder.encode(entry.getValue(), paramsEncoding));
                encodedParams.append('&');
            }
            return encodedParams.toString();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Encoding not supported: " + paramsEncoding, e);
        }
    }

//    /**
//     * 根据给定的图片宽高压缩图片，如果给定的宽或高为0则不压缩图片
//     *
//     * @param inputStream 图片的输入流
//     * @param width       目标宽度
//     * @param height      目标高度
//     * @return 压缩后的图片
//     */
//    private Bitmap getCompressedBitmap(InputStream inputStream, int width, int height, Bitmap.Config config) {
//        try {
//            // 将输入流中的数据读入数组中
//            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//            byte[] buffer = new byte[1024 * 8];
//            int length = 0;
//            while ((length = inputStream.read(buffer)) != -1) {
//                byteArrayOutputStream.write(buffer, 0, length);
//                byteArrayOutputStream.flush();
//            }
//            byte[] bytes = byteArrayOutputStream.toByteArray();
//            byteArrayOutputStream.close();
//            // 解析byte[]数组，获取图片原始尺寸
//            BitmapFactory.Options options = new BitmapFactory.Options();
//            // 仅加载边界属性
//            options.inJustDecodeBounds = true;
//            BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
//            // 根据原始尺寸计算压缩比例
//            int scale = 1;
//            if (width != 0 && height != 0) {
//                int scaleW = options.outWidth / width;
//                int scaleH = options.outHeight / height;
//                scale = scaleW > scaleH ? scaleW : scaleH;
//            }
//            // 再次解析byte[]数组，获取Bitmap
//            options.inJustDecodeBounds = false;
//            options.inPreferredConfig = config;
//            options.inSampleSize = scale;
//            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
//        } catch (IOException e) {
//            e.printStackTrace();
//            return null;
//        }
//    }

    void interruptExecutor() {
//        shouldLoop = false;
        interrupted = true;
        if (mConnection != null) {
            mConnection.disconnect();
        }
        interrupt();
//        Log.i("HttpExecutor", "Waiting area: Thread id = " + this.getId() + ", exiting loop...");
//        synchronized (mSynLock) {
//            mSynLock.notify();
//        }
    }

}

