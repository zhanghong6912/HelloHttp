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
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Zhang on 2017/7/10.<br/>
 * Description: 一个简单的http请求框架，支持StringRequest，JsonRequest，ImageRequest<br/>
 * 使用方法：1.创建RequestQueue实例：HelloHttp.createRequestQueue()。2.创建Request对象，传递相关参数。3.在RequestCallback中得到请求响应。4.在无需使用时停止服务：HelloHttp.stop()<br/>
 */
public class HelloHttp {

    public static RequestQueue createRequestQueue() {
//        return RequestQueue.getInstance();
        return new RequestQueue();
    }

    public static void stop(RequestQueue queue) {
//        RequestQueue.getInstance().stopRequestQueue();
        Config.quit = true;
        queue.stopRequestQueue();
    }

    /**
     * 根据给定的图片宽高压缩图片，如果给定的宽或高为0则不压缩图片
     *
     * @param inputStream 图片的输入流
     * @param width       目标宽度
     * @param height      目标高度
     * @return 压缩后的图片
     */
    // 避免同时解码多张图片，可明显降低处理过程中CPU占用，改善流畅度
    static synchronized Bitmap getCompressedBitmap(InputStream inputStream, int width, int height, Bitmap.Config config) {
        try {
            // 将输入流中的数据读入数组中
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024 * 8];
            int length = 0;
            while ((length = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, length);
                byteArrayOutputStream.flush();
            }
            byte[] bytes = byteArrayOutputStream.toByteArray();
            byteArrayOutputStream.close();
            // 解析byte[]数组，获取图片原始尺寸
            BitmapFactory.Options options = new BitmapFactory.Options();
            // 仅加载边界属性
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
            // 根据原始尺寸计算压缩比例
            int scale = 1;
            if (width != 0 && height != 0) {
                int scaleW = options.outWidth / width;
                int scaleH = options.outHeight / height;
                scale = scaleW > scaleH ? scaleW : scaleH;
            }
            // 再次解析byte[]数组，获取Bitmap
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = config;
            options.inSampleSize = scale;
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // android.app.DownloadManager sample
//    String downloadUrl = "http://dl.hdslb.com/mobile/latest/iBiliPlayer-bili.apk";
//    DownloadManager.Request downloadReq = new DownloadManager.Request(Uri.parse(downloadUrl));
//    downloadReq.setTitle("Download File");
//    downloadReq.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
//    downloadReq.setAllowedOverRoaming(false);
//    downloadReq.setDescription("Downloading...");
//    downloadReq.setDestinationInExternalFilesDir(TestActivity.this, Environment.DIRECTORY_DOWNLOADS, downloadUrl.substring(downloadUrl.lastIndexOf("/") + 1));
//    DownloadManager manager = (DownloadManager) TestActivity.this.getSystemService(DOWNLOAD_SERVICE);
//    manager.enqueue(downloadReq);

}
