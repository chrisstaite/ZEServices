package com.yourdreamnet.zeservices.api;

import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import rx.Observable;
import rx.schedulers.Schedulers;
import rx.subjects.AsyncSubject;

public class ZEServicesApi {

    // Requests can take some time, so let them take up to 30 seconds
    static final int REQUEST_TIMEOUT_MS = 30000;
    // The base URL for the ZE Services API
    static final String HOST = "https://www.services.renault-ze.com";

    private String mUsername;
    private String mPassword;
    private AsyncSubject<AuthenticatedApi> mAuthenticated;

    public ZEServicesApi(String username, String password)
    {
        mUsername = username;
        mPassword = password;
        mAuthenticated = null;
    }

    public synchronized Observable<AuthenticatedApi> getAuthenticated(RequestQueue queue)
    {
        if (mAuthenticated == null) {
            mAuthenticated = AsyncSubject.create();
            getAccessToken(queue).subscribe(mAuthenticated);
        }
        return mAuthenticated.asObservable();
    }

    private Observable<AuthenticatedApi> getAccessToken(RequestQueue queue)
    {
        final String URL = HOST + "/api/user/login";
        final RequestFuture<AuthenticatedApi> result = RequestFuture.newFuture();
        final Observable<AuthenticatedApi> o = Observable.from(result, Schedulers.io());

        final Map<String, String> request = new HashMap<>();
        request.put("username", mUsername.toLowerCase());
        request.put("password", mPassword);

        final JsonObjectRequest authenticationRequest = new JsonObjectRequest(
            Request.Method.POST,
            URL,
            new JSONObject(request),
            response -> {
                try {
                    String authenticationToken = response.getString("token");
                    String xsrfToken = response.getString("xsrfToken");
                    JSONObject user = response.getJSONObject("user");
                    JSONObject currentVehicle = user.getJSONObject("vehicle_details");
                    JSONArray availableVehicles = user.getJSONArray("associated_vehicles");
                    String currentVIN = currentVehicle.getString("VIN");
                    String refreshToken = response.getString("refreshToken");
                    String[] availableVINs = new String[availableVehicles.length()];
                    for (int i = 0; i < availableVehicles.length(); i++) {
                        availableVINs[i] = availableVehicles.getJSONObject(i).getString("VIN");
                    }
                    result.onResponse(new AuthenticatedApi(authenticationToken, refreshToken, xsrfToken, currentVIN, availableVINs));
                } catch (JSONException e) {
                    Log.e("ZEServicesApi", "Unable to parse JSON response", e);
                    result.onErrorResponse(new VolleyError("Unable to parse response", e));
                }
            }, error -> {
                result.onErrorResponse(error);
                Log.e("ZEServicesApi", "Bad response to login", error);
            }
        ) {
            @Override
            protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                Map headers = response.headers;
                String cookie = (String) headers.get("Set-Cookie");
                Log.d("ZEServicesApi", "Got cookie " + cookie);
                Response<JSONObject> result = super.parseNetworkResponse(response);
                if (result.isSuccess()) {
                    try {
                        // TODO: Check the expiry of the cookie
                        result.result.put("refreshToken", cookie.split(";")[0]);
                    } catch (JSONException e) {
                        Log.e("ZEServicesApi", "Unable to add token to response", e);
                    }
                }
                return result;
            }
        };

        authenticationRequest.setRetryPolicy(new DefaultRetryPolicy(
            REQUEST_TIMEOUT_MS,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        result.setRequest(authenticationRequest);
        queue.add(authenticationRequest);

        return o;
    }

}
