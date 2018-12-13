package com.yourdreamnet.zeservices;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;
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

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import rx.Observable;
import rx.schedulers.Schedulers;
import rx.subjects.AsyncSubject;

public class ZEServicesAPI {

    // Requests can take some time, so let them take up to 30 seconds
    private static final int REQUEST_TIMEOUT_MS = 30000;
    // The base URL for the ZE Services API
    private static final String HOST = "https://www.services.renault-ze.com";

    private String mUsername;
    private String mPassword;
    private AsyncSubject<AuthenticatedAPI> mAuthenticated;

    ZEServicesAPI(String username, String password)
    {
        mUsername = username;
        mPassword = password;
        mAuthenticated = null;
    }

    public static class AuthenticatedAPI implements Parcelable {

        private String mAuthenticationToken;
        private String mRefreshToken;
        private String mXsrfToken;
        private String mCurrentVin;
        private String[] mAvailableVins;

        AuthenticatedAPI(String authenticationToken, String refreshToken, String xsrfToken, String currentVin, String[] availableVins) {
            mAuthenticationToken = authenticationToken;
            mRefreshToken = refreshToken;
            mXsrfToken = xsrfToken;
            mCurrentVin = currentVin;
            mAvailableVins = availableVins;
        }

        private AuthenticatedAPI(Parcel in) {
            mAuthenticationToken = in.readString();
            mRefreshToken = in.readString();
            mXsrfToken = in.readString();
            mCurrentVin = in.readString();
            mAvailableVins = in.createStringArray();
        }

        public static final Creator<AuthenticatedAPI> CREATOR = new Creator<AuthenticatedAPI>() {
            @Override
            public AuthenticatedAPI createFromParcel(Parcel in) {
                return new AuthenticatedAPI(in);
            }

            @Override
            public AuthenticatedAPI[] newArray(int size) {
                return new AuthenticatedAPI[size];
            }
        };

        boolean hasExpired() {
            // The token should be a valid JWT token of three parts
            final String[] parts = mAuthenticationToken.split("\\.");
            if (parts.length != 3) {
                return true;
            }
            byte[] decoded = Base64.decode(parts[1], Base64.DEFAULT);
            try {
                JSONObject parsed = new JSONObject(new String(decoded, "UTF-8"));
                return new Date().after(new Date(parsed.getLong("exp") * 1000));
            } catch (UnsupportedEncodingException e) {
                Log.e("ZEServicesAPI", "Unable to parse token as UTF-8", e);
            } catch (JSONException e) {
                Log.e("ZEServicesAPI", "Unable to decode token to verify", e);
            }
            return true;
        }

        private Observable<AuthenticatedAPI> refresh(RequestQueue queue) {
            final String URL = HOST + "/api/user/token/refresh";
            final Map<String, String> request = new HashMap<>();
            final RequestFuture<AuthenticatedAPI> result = RequestFuture.newFuture();
            final Observable<AuthenticatedAPI> o = Observable.from(result, Schedulers.io());
            request.put("token", mAuthenticationToken);
            final JsonObjectRequest refreshRequest = new JsonObjectRequest(
                Request.Method.POST,
                URL,
                new JSONObject(request),
                response -> {
                    try {
                        mAuthenticationToken = response.getString("token");
                        // TODO: Is the cookie updated here too?
                        result.onResponse(AuthenticatedAPI.this);
                    } catch (JSONException e) {
                        Log.e("ZEServicesAPI", "Unable to parse JSON response", e);
                        result.onErrorResponse(new VolleyError("Invalid response", e));
                    }
                },
                error -> {
                    Log.e("ZEServicesAPI", "Bad response to refresh", error);
                    result.onErrorResponse(error);
                }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String>  params = new HashMap<>();
                    params.put("X-XSRF-TOKEN", mXsrfToken);
                    params.put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.80 Safari/537.36");
                    params.put("Referer", "https://www.services.renault-ze.com/user/login");
                    params.put("Origin", "https://www.services.renault-ze.com");
                    params.put("Cookie", mRefreshToken);
                    return params;
                }
            };
            refreshRequest.setRetryPolicy(new DefaultRetryPolicy(
                REQUEST_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));
            result.setRequest(refreshRequest);
            queue.add(refreshRequest);
            return o;
        }

        private Observable<JSONObject> doRequest(final RequestQueue queue, final String url, final Map<String, String> query) {
            if (hasExpired()) {
                final AsyncSubject<JSONObject> o = AsyncSubject.create();
                refresh(queue).subscribe(
                    api -> api.actualDoRequest(queue, url, query).subscribe(o),
                    o::onError
                );
                return o.asObservable();
            }
            return actualDoRequest(queue, url, query);
        }

        private Observable<JSONObject> actualDoRequest(RequestQueue queue, String url, Map<String, String> query) {
            final RequestFuture<JSONObject> result = RequestFuture.newFuture();
            final Observable<JSONObject> o = Observable.from(result, Schedulers.io());
            final JsonObjectRequest request = new JsonObjectRequest(
                query == null ? Request.Method.GET : Request.Method.POST,
                HOST + url,
                query == null ? null : new JSONObject(query),
                result,
                result
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String>  params = new HashMap<>();
                    params.put("Authorization", "Bearer " + mAuthenticationToken);
                    params.put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.80 Safari/537.36");
                    params.put("Referer", "https://www.services.renault-ze.com/user/login");
                    params.put("Origin", "https://www.services.renault-ze.com");
                    return params;
                }
            };
            request.setRetryPolicy(new DefaultRetryPolicy(
                REQUEST_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));
            result.setRequest(request);
            queue.add(request);
            return o;
        }

        private Observable<JSONObject> doVinRequest(final RequestQueue queue, final String vin, final String url, final Map<String, String> request) {
            if (vin.equals(mCurrentVin)) {
                return doRequest(queue, url, request);
            } else {
                // Change to the new VIN and perform the request in a separate thread
                final AsyncSubject<JSONObject> o = AsyncSubject.create();
                Observable<JSONObject> active = setActive(queue, vin);
                active.subscribe(set -> {
                    mCurrentVin = vin;
                    if (hasExpired()) {
                        refresh(queue).subscribe(
                            api -> doRequest(queue, url, request).subscribe(o),
                            o::onError
                        );
                    } else {
                        doRequest(queue, url, request).subscribe(o);
                    }
                }, o::onError);
                return o.asObservable();
            }
        }

        public Observable<JSONObject> getBattery(RequestQueue queue, String vin) {
            final String URL = "/api/vehicle/" + vin + "/battery";
            return doVinRequest(queue, vin, URL, null);
        }

        public Observable<JSONObject> setActive(RequestQueue queue, String vin) {
            final String URL = "/api/vehicle";
            final Map<String, String> request = new HashMap<>();
            request.put("active_vehicle", vin);
            return doRequest(queue, URL, request);
        }

        public Observable<JSONObject> getChargeSiblings(RequestQueue queue, String vin) {
            final String URL = "/api/vehicle/" + vin + "/charge/siblings";
            return doVinRequest(queue, vin, URL, null);
        }

        public Observable<JSONObject> startPrecondition(RequestQueue queue, String vin) {
            final String URL = "/api/vehicle/" + vin + "/air-conditioning";
            return doVinRequest(queue, vin, URL, null);
        }

        public Observable<JSONObject> schedulePrecondition(RequestQueue queue, String vin, String startTime) {
            final String URL = "/api/vehicle/" + vin + "/air-conditioning/scheduler";
            final Map<String, String> request = new HashMap<>();
            request.put("start", startTime);
            return doVinRequest(queue, vin, URL, request);
        }

        public Observable<JSONObject> preconditionStatus(RequestQueue queue, String vin) {
            final String URL = "/api/vehicle/" + vin + "/air-conditioning/last";
            return doVinRequest(queue, vin, URL, null);
        }

        public Observable<JSONObject> startCharge(RequestQueue queue, String vin) {
            final String URL = "/api/vehicle/" + vin + "/charge";
            return doVinRequest(queue, vin, URL, null);
        }

        public String getCurrentVin() {
            return mCurrentVin;
        }

        public String[] getAvailableVins() {
            return mAvailableVins;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int flags) {
            parcel.writeString(mAuthenticationToken);
            parcel.writeString(mRefreshToken);
            parcel.writeString(mXsrfToken);
            parcel.writeString(mCurrentVin);
            parcel.writeStringArray(mAvailableVins);
        }
    }

    synchronized Observable<AuthenticatedAPI> getAuthenticated(RequestQueue queue)
    {
        if (mAuthenticated == null) {
            mAuthenticated = AsyncSubject.create();
            getAccessToken(queue).subscribe(mAuthenticated);
        }
        return mAuthenticated.asObservable();
    }

    private Observable<AuthenticatedAPI> getAccessToken(RequestQueue queue)
    {
        final String URL = HOST + "/api/user/login";
        final RequestFuture<AuthenticatedAPI> result = RequestFuture.newFuture();
        final Observable<AuthenticatedAPI> o = Observable.from(result, Schedulers.io());

        final Map<String, String> request = new HashMap<>();
        request.put("username", mUsername);
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
                    result.onResponse(new AuthenticatedAPI(authenticationToken, refreshToken, xsrfToken, currentVIN, availableVINs));
                } catch (JSONException e) {
                    Log.e("ZEServicesAPI", "Unable to parse JSON response", e);
                    result.onErrorResponse(new VolleyError("Unable to parse response", e));
                }
            }, error -> {
                result.onErrorResponse(error);
                Log.e("ZEServicesAPI", "Bad response to login", error);
            }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String>  params = new HashMap<>();
                params.put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.80 Safari/537.36");
                params.put("Referer", "https://www.services.renault-ze.com/user/login");
                params.put("Origin", "https://www.services.renault-ze.com");
                return params;
            }

            @Override
            protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                Map headers = response.headers;
                String cookie = (String) headers.get("Set-Cookie");
                Log.d("ZEServicesAPI", "Got cookie " + cookie);
                Response<JSONObject> result = super.parseNetworkResponse(response);
                if (result.isSuccess()) {
                    try {
                        // TODO: Check the expiry of the cookie
                        result.result.put("refreshToken", cookie.split(";")[0]);
                    } catch (JSONException e) {
                        Log.e("ZEServicesAPI", "Unable to add token to response", e);
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
