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

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Zhang on 2017/7/11.<br/>
 * Description: http请求的等候区，一个等候区对应一个HttpExecutor（线程）， http请求进入等候区后以FIFO的规则等待执行
 */
class RequestWaitingArea {

    /**
     * 等候区的队列
     */
    private List<Request> mWaitingArea;
    /**
     * http请求执行者
     */
    private final HttpExecutor mHttpExecutor;

    public RequestWaitingArea() {
        mWaitingArea = new ArrayList<>();
        mHttpExecutor = new HttpExecutor(this);
        mHttpExecutor.start();
    }

    /**
     * 将http请求加入等候区队列
     *
     * @param request http请求
     */
    void addRequest(Request request) {
        mWaitingArea.add(request);
        synchronized (HttpExecutor.mSynLock) {
            HttpExecutor.mSynLock.notify();
        }
    }

    /**
     * 获取这一等候区队列中的所有请求
     *
     * @return --
     */
    List<Request> getRequestsFromWaitingArea() {
        return mWaitingArea;
    }

    int getRequestCount() {
        return mWaitingArea.size();
    }

    void stopHttpExecutor() {
        mHttpExecutor.interruptExecutor();
    }

}
