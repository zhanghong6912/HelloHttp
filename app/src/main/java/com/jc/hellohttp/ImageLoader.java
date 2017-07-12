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
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.util.LruCache;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Zhang on 2017/7/4.<br/>
 * Description: 实现后台加载图片，并将图片缓存至文件<br/>
 * 使用方法：1. 实例化ImageLoader。 2. 调用into()方法，传递必要参数。 3. 在不需要使用ImageLoader时调用stopImageLoadTask()方法停止任务队列。<br/>
 * 已实现：内存缓存、文件缓存、下载队列<br/>
 * 待优化：与HelloHttp整合。
 * 整合过程中发现ListView中的View重用时会出现显示错乱的问题，解决方法：要求调用into()之前为ImageView设置Tag，并在实例化ImageLoader时传递Adapter（避免图片已经加载完毕但是图片不更新）
 * 解决上述问题后发现另一个问题：如果ListView页面第一次打开，即所有图片都需要从网络加载，则会一次性在开启多个线程，且这些线程都在后台加载图片，会造成ListView滑动时明显卡顿
 */
public class ImageLoader {
    private static final String TAG = "ImageLoader";
    /**
     * 内存缓存
     */
//    private Map<String, SoftReference<Bitmap>> mCache = new HashMap<>();
    private LruCache<String, Bitmap> mCache; // 使用LruCache取代软引用
    @SuppressWarnings("FieldCanBeLocal")
    private int maxMemoCacheSize = 10 * 1024 * 1024; // 最大内存缓存：10MB
    /**
     * Context
     */
    private Context mContext;
    /**
     * 下载任务集合
     */
    private List<ImageLoadTask> mTasks = new ArrayList<>();

    private boolean shouldLoop = true;
    private final Thread imageLoadThread;
    private ShowImageHandler mHandler;

    public ImageLoader(Context context) {
        super();
        this.mContext = context;
        this.mCache = new LruCache<>(maxMemoCacheSize);
        this.mHandler = new ShowImageHandler();

        // 构造方法只执行一次，因此该线程只会有一个
        // 缺陷：只有一个下载线程，下载量大时会导致图片显示过慢
        imageLoadThread = new Thread() {
            @Override
            public void run() {
                while (shouldLoop) {
                    if (!mTasks.isEmpty()) {
                        // 任务队列中有任务
                        ImageLoadTask currTask = mTasks.remove(0);
                        try {
                            HttpURLConnection connection = (HttpURLConnection) new URL(currTask.url).openConnection();
                            connection.setRequestMethod("GET");
                            InputStream inputStream = connection.getInputStream();
                            // 压缩图片
                            int wid = currTask.width == 0 ? currTask.intoView.getWidth() : currTask.width;
                            int hei = currTask.height == 0 ? currTask.intoView.getHeight() : currTask.height;
                            currTask.bitmap = getCompressedBitmap(inputStream, wid, hei);
                            if (currTask.bitmap != null) {
                                // 将压缩后的图片放入内存缓存
                                mCache.put(currTask.url, currTask.bitmap);
                                // 将压缩后的图片放入文件缓存
                                saveBitmap(currTask.bitmap,
                                        new File(mContext.getApplicationContext().getCacheDir() + "/image" + currTask.url.substring(currTask.url.lastIndexOf("/"))));

                                Message msg = Message.obtain();
                                msg.what = HANDLE_MSG_SHOW_BMP;
                                msg.obj = currTask;
                                mHandler.sendMessage(msg);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        // 任务队列中暂时没有任务，则线程阻塞等待
                        synchronized (imageLoadThread) {
                            try {
                                imageLoadThread.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        };
        imageLoadThread.start();
    }

    /**
     * 封装图片下载任务
     */
    private class ImageLoadTask {
        String url;
        Bitmap bitmap;
        int width;
        int height;
        ImageView intoView;
        int defaultBmpResId;
    }

    /**
     * 在指定的控件中显示图片
     *
     * @param view            指定的控件
     * @param url             图片url
     * @param width           指定的图片宽度
     * @param height          指定的高度
     * @param defaultBmpResId 默认图片资源id
     */
    public void into(ImageView view, String url, int width, int height, int defaultBmpResId) {
        // 先设置默认图片
        view.setImageResource(defaultBmpResId);

        Bitmap bitmap = null;
        // 先到内存缓存中查询
//        SoftReference<Bitmap> softReference = mCache.get(url);
//        if (softReference != null) {
//            // 以前保存过
//            bitmap = softReference.get();
//            if (bitmap != null) {
//                // 保存的Bitmap还存在
//                return;
//            }
//        }
        // 先到内存缓存中查询
        bitmap = mCache.get(url);
        if (bitmap != null) {
            // 保存的Bitmap还存在
//            Log.i(TAG, "Bitmap loaded from memory");
            view.setImageBitmap(bitmap);
            return;
        }

        // 执行到此处说明内存缓存中不存在指定的Bitmap
        // 到文件缓存中查找
        bitmap = getCompressedBitmap(new File(mContext.getApplicationContext().getCacheDir() + "/image" + url.substring(url.lastIndexOf("/"))));
        if (bitmap != null) {
            // 文件缓存中存在指定的Bitmap
            // 将Bitmap保存到内存缓存
            mCache.put(url, bitmap);
            view.setImageBitmap(bitmap);
//            Log.i(TAG, "Bitmap loaded from file");
            return;
        }

        // 执行到此处说明内存缓存和文件缓存中均不存在指定的Bitmap
        // 从网络请求图片
        ImageLoadTask newTask = new ImageLoadTask();
        newTask.url = url;
        newTask.width = width;
        newTask.height = height;
        newTask.intoView = view;
        newTask.defaultBmpResId = defaultBmpResId;
        mTasks.add(newTask);
//        Log.i(TAG, "Bitmap loaded from network");
        synchronized (imageLoadThread) {
            imageLoadThread.notify();
        }
    }

    private static final int HANDLE_MSG_SHOW_BMP = 8327;

    private static class ShowImageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HANDLE_MSG_SHOW_BMP:
                    ImageLoadTask task = (ImageLoadTask) msg.obj;
                    if (task != null) {
                        if (task.intoView != null) {
                            if (task.bitmap != null) {
                                task.intoView.setImageBitmap(task.bitmap);
                            } else {
                                if (task.defaultBmpResId != 0) {
                                    task.intoView.setImageResource(task.defaultBmpResId);
                                }
                            }
                        }
                    }
                    break;
            }
        }
    }

    /**
     * 停止任务队列，由于此ImageLoader自身没有控制停止任务队列的机制，所以在不需要使用ImageLoader时，如Activity被销毁时停止任务队列，
     * 否则ImageLoader会一直等待队列中的新任务而不会停止从而造成内存泄漏
     */
    public void stopImageLoadTask() {
        shouldLoop = false;
        synchronized (imageLoadThread) {
            imageLoadThread.notify();
        }
    }

    private void saveBitmap(Bitmap bitmap, File file) {
        try {
            // 如果父目录不存在，则创建父目录
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
     * 根据给定的图片宽高压缩图片，如果给定的宽或高为0则不压缩图片
     *
     * @param in     图片的输入流
     * @param width  目标宽度
     * @param height 目标高度
     * @return 压缩后的图片
     * @throws IOException --
     */
    private Bitmap getCompressedBitmap(InputStream in, int width, int height) throws IOException {
        // 将输入流中的数据读入数组中
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024 * 8];
        int length = 0;
        while ((length = in.read(buffer)) != -1) {
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
        options.inSampleSize = scale;

        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
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
