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
 * Created by Zhang on 2017/7/7.<br/>
 * Description: HelloHttp的相关配置
 */
class Config {

    static String STRING_REQ_PROP = "application/x-www-form-urlencoded";
    static String JSON_REQ_PROP = "application/json";
    static final String PARAMS_ENCODING = "UTF-8";

    /**
     * 默认核心线程池大小
     */
    static final int CORE_SIZE = getDefCoreSize();
    /**
     * 连接超时时间
     */
    static final int CONNECT_TIMEOUT = 5000;
    /**
     * 读取超时时间
     */
    static final int READ_TIMEOUT = 5000;

    private static int getDefCoreSize() {
        // 根据CPU核心数（包括超线程）决定默认线程池的大小。
        int processors = Runtime.getRuntime().availableProcessors();
        if (processors < 4) return 4;
        if (processors > 8) return 8;
        return processors;
    }

}
