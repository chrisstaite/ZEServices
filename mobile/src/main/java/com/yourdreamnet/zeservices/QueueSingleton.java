package com.yourdreamnet.zeservices;

import android.content.Context;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;

public class QueueSingleton {

    private static QueueSingleton mSingleton = null;
    private RequestQueue mRequestQueue;

    private QueueSingleton(Context context) {
        Cache cache = new DiskBasedCache(context.getCacheDir(), 1024 * 1024);
        Network network = new BasicNetwork(new HurlStack());
        mRequestQueue = new RequestQueue(cache, network);
        mRequestQueue.start();
    }

    private static synchronized QueueSingleton getSingleton(Context context) {
        if (mSingleton == null) {
            mSingleton = new QueueSingleton(context);
        }
        return mSingleton;
    }

    public static RequestQueue getQueue(Context context) {
        return getSingleton(context).mRequestQueue;
    }

}
