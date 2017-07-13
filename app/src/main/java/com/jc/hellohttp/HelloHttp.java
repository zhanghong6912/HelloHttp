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

/**
 * Created by Zhang on 2017/7/10.<br/>
 * Description: 一个简单的http请求框架，支持StringRequest，JsonRequest，ImageRequest<br/>
 * 使用方法：1.创建RequestQueue实例：HelloHttp.createRequestQueue()。2.创建Request对象，传递相关参数。3.在RequestCallback中得到请求响应。4.在无需使用时停止服务：HelloHttp.stop()<br/>
 * 待优化：使用BlockingQueue取代手动管理请求队列；实现请求优先级；实现FileRequest以支持普通文件的上传和下载；集成ImageLoader
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

}
