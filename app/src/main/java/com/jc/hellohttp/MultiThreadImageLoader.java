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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.Nullable;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * Created by Zhang on 2017/7/12.<br/>
 * Description: 多线程的实现后台加载图片，并将图片缓存至文件<br/>
 * 使用方法：1.实例化ImageLoader。2.为控件设置Tag，Tag的值为图片的url地址（必要）。3.调用into()方法，传递必要参数。<br/>
 * 已实现：内存缓存、文件缓存、与HelloHttp整合的下载队列<br/>
 * 待优化：避免同时解码多张图片造成卡顿；避免ListView中不为控件设置Tag就可能会出现的图片显示错乱的问题
 */
public class MultiThreadImageLoader {

    private static final String TAG = MultiThreadImageLoader.class.getSimpleName();
    /**
     * 内存缓存
     */
    private LruCache<String, Bitmap> mCache; // 使用LruCache取代软引用
    @SuppressWarnings("FieldCanBeLocal")
    private int maxMemoCacheSize = 10 * 1024 * 1024; // 最大内存缓存：10MB
    /**
     * Context
     */
    private Context mContext;

    /**
     * 请求队列
     */
    private RequestQueue mRequestQueue;

    private BaseAdapter mAdapter;

    public MultiThreadImageLoader(Context context, RequestQueue requestQueue, @Nullable BaseAdapter adapter) {
        this.mContext = context;
        this.mCache = new LruCache<>(maxMemoCacheSize);
        this.mRequestQueue = requestQueue;
        this.mAdapter = adapter;
    }

    /**
     * 在指定的控件中显示图片
     *
     * @param intoView        指定的控件
     * @param url             图片url
     * @param width           指定的图片宽度
     * @param height          指定的高度
     * @param defaultBmpResId 默认图片资源id
     */
    // 缺陷：如果同时加入的下载任务量过多，比如ListView中，会因同时解码多张图片而出现滑动过程中明显的掉帧甚至卡顿
    public void into(final ImageView intoView, final String url, int width, int height, final int defaultBmpResId) {
        // 先设置默认图片
        if (url.equals(intoView.getTag())) {
            intoView.setImageResource(defaultBmpResId);
        }

        Bitmap bitmap = null;
        // 先到内存缓存中查询
        bitmap = mCache.get(url);
        if (bitmap != null) {
            // 保存的Bitmap还存在
            Log.i(TAG, "Bitmap loaded from memory...");
            if (url.equals(intoView.getTag())) {
                intoView.setImageBitmap(bitmap);
            }
            return;
        }

        // 执行到此处说明内存缓存中不存在指定的Bitmap
        // 到文件缓存中查找
        bitmap = getCompressedBitmap(new File(mContext.getApplicationContext().getCacheDir() + "/image" + url.substring(url.lastIndexOf("/"))));
        if (bitmap != null) {
            // 文件缓存中存在指定的Bitmap
            // 将Bitmap保存到内存缓存
            mCache.put(url, bitmap);
            if (url.equals(intoView.getTag())) {
                intoView.setImageBitmap(bitmap);
            }
            Log.i(TAG, "Bitmap loaded from file...");
            return;
        }

        // 执行到此处说明内存缓存和文件缓存中均不存在指定的Bitmap
        // 从网络请求图片
        int wid = width == 0 ? intoView.getWidth() : width;
        int hei = height == 0 ? intoView.getHeight() : height;
        Request request = new Request(url, Request.RequestType.IMAGE, Request.RequestMethod.GET, null, new RequestCallback() {
            @Override
            public void onSuccess(Object response) {
                if (response != null && response instanceof Bitmap) {
                    if (url.equals(intoView.getTag())) {
                        intoView.setImageBitmap((Bitmap) response);
                        if (mAdapter != null) {
                            // 避免图片已经下载，但是列表不自动更新
                            mAdapter.notifyDataSetChanged();
                        }
                    }
                    // 将压缩后的图片放入内存缓存
                    mCache.put(url, (Bitmap) response);
                    // 将压缩后的图片放入文件缓存
                    saveBitmap((Bitmap) response,
                            new File(mContext.getApplicationContext().getCacheDir() + "/image" + url.substring(url.lastIndexOf("/"))));
                    Log.i(TAG, "Bitmap loaded from network...");
                }
            }

            @Override
            public void onError(String errorMsg) {
                intoView.setImageResource(defaultBmpResId);
                if (mAdapter != null) {
                    mAdapter.notifyDataSetChanged();
                }
                Log.e(TAG, errorMsg);
            }
        }, wid, hei, Bitmap.Config.ARGB_8888);
        request.setPriority(Request.Priority.LOW); // 设为低优先级
        if (!mRequestQueue.add(request)) {
            Log.e(TAG, "An error occurred while the image load task joined the request queue...");
        }
    }

    private void saveBitmap(Bitmap bitmap, File file) {
        try {
            if (!file.getParentFile().exists()) {
                //noinspection ResultOfMethodCallIgnored
                file.getParentFile().mkdirs();
            }
            FileOutputStream outputStream = new FileOutputStream(file);
            // 保存到文件
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 从文件缓存目录中加载出Bitmap
     *
     * @param file 给定的文件对象
     * @return Bitmap
     */
    @Nullable
    private Bitmap getCompressedBitmap(File file) {
        if (!file.exists()) {
            return null;
        }
        return BitmapFactory.decodeFile(file.getAbsolutePath());
    }

}
