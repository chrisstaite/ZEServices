package com.yourdreamnet.zeservices.api;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

abstract class RequestCreator<T> {

    String mUrl;
    int mMethod;
    T mQuery;

    RequestCreator(String url, boolean post, T query) {
        mUrl = ZEServicesApi.HOST + url;
        mMethod = post ? Request.Method.POST : Request.Method.GET;
        mQuery = query;
    }

    Map<String, String> headers(String token) {
        Map<String, String> params = new HashMap<>();
        params.put("Authorization", "Bearer " + token);
        return params;
    }

    abstract Request<T> get(RequestFuture<T> result, String token);

    static class JsonObjectRequestCreator extends RequestCreator<JSONObject> {

        JsonObjectRequestCreator(String url, boolean post, JSONObject query) {
            super(url, post, query);
        }

        @Override
        Request<JSONObject> get(RequestFuture<JSONObject> result, String token) {
            return new JsonObjectRequest(mMethod, mUrl, mQuery, result, result) {
                @Override
                public Map<String, String> getHeaders() {
                    return headers(token);
                }
            };
        }

    }

    static class JsonArrayRequestCreator extends RequestCreator<JSONArray> {

        JsonArrayRequestCreator(String url, boolean post, JSONArray query) {
            super(url, post, query);
        }

        @Override
        Request<JSONArray> get(RequestFuture<JSONArray> result, String token) {
            return new JsonArrayRequest(mMethod, mUrl, mQuery, result, result) {
                @Override
                public Map<String, String> getHeaders() {
                    return headers(token);
                }
            };
        }

    }

    static class StringRequestCreator extends RequestCreator<String> {

        StringRequestCreator(String url, boolean post, String query) {
            super(url, post, query);
        }

        @Override
        Request<String> get(RequestFuture<String> result, String token) {
            return new StringRequest(mMethod, mUrl, result, result) {
                @Override
                public Map<String, String> getHeaders() {
                    return headers(token);
                }
            };
        }

    }

}
