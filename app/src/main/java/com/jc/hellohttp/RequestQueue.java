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

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Zhang on 2017/7/7.<br/>
 * Description: http请求队列，封装了http请求等候区
 */
public final class RequestQueue {
    private static final String TAG = RequestQueue.class.getSimpleName();

    // RequestQueue在此处不能是静态的，否则会造成调用stop()之后RequestQueue中的所有资源无法释放。
//    private static RequestQueue mInstance;
//
//    static synchronized RequestQueue getInstance() {
//        if (mInstance == null) {
//            mInstance = new RequestQueue();
//        }
//        return mInstance;
//    }

    private List<RequestWaitingArea> mWaitingAreas;

    public RequestQueue() {
        mWaitingAreas = new ArrayList<>();
    }

    /**
     * 添加一个http请求
     *
     * @param request --
     * @return 请求是否成功加入队列
     */
    public boolean add(Request request) {
        if (request == null) {
            Log.e(TAG, "request = null, add failed...");
            return false;
        }
        // FIXME: 2017/7/13 如何做到调用stop()之后不再加入请求，而又避免调用stop()之后再次启动也无法加入请求?
//        if (Config.quit) {
//            Log.e(TAG, "RequestQueue id already stopped, add failed...");
//            return false;
//        }
        RequestWaitingArea properArea = null;
        if (mWaitingAreas.isEmpty()) {
            // 请求等候区为空
            properArea = new RequestWaitingArea();
            properArea.addRequest(request);
            mWaitingAreas.add(properArea);
            return true;
        }
        // 请求等候区不为空，先看已存在的等候区是否有空队列
        for (int i = 0; i < mWaitingAreas.size(); i++) {
            if (mWaitingAreas.get(i).getRequestCount() == 0) {
                properArea = mWaitingAreas.get(i);
                properArea.addRequest(request);
                return true;
            }
        }
        // 请求等候区不为空，且没有空队列，则看等候区数量是否超过上限
        if (mWaitingAreas.size() < Config.CORE_SIZE) {
            // 未超过上限，则再创建一个新的等候区队列
            properArea = new RequestWaitingArea();
            mWaitingAreas.add(properArea);
            properArea.addRequest(request);
        } else {
            // 已达到上限，且每个等候区都不是空队列，则选择一个请求数量最少的队列
            properArea = chooseProperArea();
            if (properArea == null) {
                // FIXME: 2017/7/11 如果得到的properArea为null，则证明chooseProperArea()的算法有误
                properArea = mWaitingAreas.get(0);
            }
            properArea.addRequest(request);
        }
        return true;
    }

    private RequestWaitingArea chooseProperArea() {
        int requestCount = Integer.MAX_VALUE;
        RequestWaitingArea properArea = null;
        for (int i = 0; i < mWaitingAreas.size(); i++) {
            int temp = mWaitingAreas.get(i).getRequestCount();
            if (temp < requestCount) {
                properArea = mWaitingAreas.get(i);
                requestCount = properArea.getRequestCount();
            }
        }
        return properArea;
    }

    void stopRequestQueue() {
        for (int i = 0; i < mWaitingAreas.size(); i++) {
            mWaitingAreas.get(i).stopHttpExecutor();
        }
    }

}
