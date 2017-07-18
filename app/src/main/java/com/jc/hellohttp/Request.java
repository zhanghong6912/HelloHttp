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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.Map;

/**
 * Created by Zhang on 2017/7/7.<br/>
 * Description: 封装http请求，通过构造方法决定不同的请求类型和请求方式
 */
public class Request implements Comparable<Request> {

    /**
     * http请求方式，暂时仅支持GET和POST
     */
    public enum RequestMethod {
        GET, POST
    }

    /**
     * http请求的类型，类似于Volley中的StringRequest, JsonRequest, ImageRequest
     */
    public enum RequestType {
        STRING, JSON, IMAGE, @Deprecated DOWNLOAD, UPLOAD
    }

    /**
     * http请求优先级，分为四个等级：低；普通；高；立即
     */
    public enum Priority {
        LOW, NORMAL, HIGH, IMMEDIATE
    }

    /**
     * URL
     */
    private String mUrl = "";
    /**
     * 请求完成后的回调
     */
    private RequestCallback mCallback;
    /**
     * 请求方式，默认为GET请求
     */
    private RequestMethod mRequestMethod = RequestMethod.GET;
    /**
     * 请求类型，默认为StringRequest
     */
    private RequestType mRequestType = RequestType.STRING;
    /**
     * 请求参数（针对POST请求）
     */
    private Map<String, String> mRequestParams;
    /**
     * 请求优先级，默认为NORMAL
     */
    private Priority mPriority = Priority.NORMAL;
    /**
     * 序列号，用于当两个请求优先级相等时，比较其排队顺序
     */
    private Integer mSequence;

    // 与ImageRequest相关的参数
    private int mBmpWidth = 0; // 所需图片的宽度，0表示不压缩图片
    private int mBmpHeight = 0; // 所需图片的宽度，0表示不压缩图片
    private Bitmap.Config mBitmapConfig = Bitmap.Config.ARGB_8888; // 所需图片的品质，默认为最高品质

    public Request(String url, RequestType type, RequestCallback callback) {
        if (url == null || TextUtils.isEmpty(url)) {
            throw new IllegalArgumentException("Unsupported url");
        }
        this.mUrl = url;
        this.mRequestType = type;
        this.mCallback = callback;
    }

    public Request(String url, RequestType type, RequestMethod method, RequestCallback callback) {
        if (url == null || TextUtils.isEmpty(url)) {
            throw new IllegalArgumentException("Unsupported url");
        }
        this.mUrl = url;
        this.mRequestType = type;
        this.mRequestMethod = method;
        this.mCallback = callback;
    }

    // 适用于带请求参数的POST请求
    public Request(String url, RequestType type, Map<String, String> params, RequestCallback callback) {
        if (url == null || TextUtils.isEmpty(url)) {
            throw new IllegalArgumentException("Unsupported url");
        }
        this.mUrl = url;
        this.mRequestType = type;
        this.mRequestMethod = RequestMethod.POST;
        this.mRequestParams = params;
        this.mCallback = callback;
    }

    // 适用于ImageRequest，指定的BitmapConfig为null时，将采用默认配置：不压缩图片，且Bitmap.Config为ARGB_8888，即最高品质
    public Request(String url, RequestType type, RequestMethod method, Map<String, String> params, RequestCallback callback,
                   int bmpWidth, int bmpHeight, @Nullable Bitmap.Config bmpConfig) {
        if (url == null || TextUtils.isEmpty(url)) {
            throw new IllegalArgumentException("Unsupported url");
        }
        this.mUrl = url;
        this.mRequestType = type;
        this.mRequestMethod = method;
        this.mRequestParams = params;
        this.mCallback = callback;
        this.mBmpWidth = bmpWidth;
        this.mBmpHeight = bmpHeight;
        if (bmpConfig != null) {
            this.mBitmapConfig = bmpConfig;
        }
    }

    String getUrl() {
        return mUrl;
    }

    RequestCallback getCallback() {
        return mCallback;
    }

    RequestMethod getRequestMethod() {
        return mRequestMethod;
    }

    RequestType getRequestType() {
        return mRequestType;
    }

    Map<String, String> getRequestParams() {
        return mRequestParams;
    }

    int getBmpWidth() {
        return mBmpWidth;
    }

    int getBmpHeight() {
        return mBmpHeight;
    }

    Bitmap.Config getBitmapConfig() {
        return mBitmapConfig;
    }

    Priority getPriority() {
        return mPriority;
    }

    // 设为public，目的是让用户在构造一个请求时也能为其设置优先级
    public void setPriority(Priority priority) {
        if (priority != null)
            this.mPriority = priority;
    }

    Integer getSequence() {
        if (mSequence == null) {
            throw new IllegalStateException("\'getSequence()\' called before \'setSequence()\'");
        }
        return mSequence;
    }

    void setSequence(Integer mSequence) {
        this.mSequence = mSequence;
    }

    @Override
    public int compareTo(@NonNull Request another) {
        Priority left = this.getPriority();
        Priority right = another.getPriority();
        // 比较优先级，如果优先级相等则比较之前设置的序列号
        return left == right ? this.mSequence - another.mSequence : right.ordinal() - left.ordinal();
    }

}
