package com.yourdreamnet.zecommon.api;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.NoCache;

public class QueueSingleton {

    private static QueueSingleton mSingleton = null;
    private RequestQueue mRequestQueue;

    private QueueSingleton() {
        Cache cache = new NoCache();
        Network network = new BasicNetwork(new HurlStack());
        mRequestQueue = new RequestQueue(cache, network);
        mRequestQueue.start();
    }

    private static synchronized QueueSingleton getSingleton() {
        if (mSingleton == null) {
            mSingleton = new QueueSingleton();
        }
        return mSingleton;
    }

    public static RequestQueue getQueue() {
        return getSingleton().mRequestQueue;
    }

}
