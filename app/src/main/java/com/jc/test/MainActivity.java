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
package com.jc.test;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.jc.hellohttp.HelloHttp;
import com.jc.hellohttp.R;
import com.jc.hellohttp.Request;
import com.jc.hellohttp.RequestCallback;
import com.jc.hellohttp.RequestQueue;

public class MainActivity extends AppCompatActivity {

    private RequestQueue mRequestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // create a request queue
        mRequestQueue = HelloHttp.createRequestQueue();

        String url = "??????????";
        Request request = new Request(url, Request.RequestType.STRING, Request.RequestMethod.GET, new RequestCallback() {
            @Override
            public void onSuccess(Object response) {
                // do something
            }

            @Override
            public void onError(String errorMsg) {
                // do something
            }
        });
        // set priority(optional)
//        request.setPriority(Request.Priority.HIGH);
        // add request into request queue
        mRequestQueue.add(request);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // stop request queue
        HelloHttp.stop(mRequestQueue);
    }
}
