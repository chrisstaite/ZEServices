package com.yourdreamnet.zeservices.api;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import rx.Observable;
import rx.schedulers.Schedulers;
import rx.subjects.AsyncSubject;

public class AuthenticatedApi implements Parcelable {

    private String mAuthenticationToken;
    private String mRefreshToken;
    private String mXsrfToken;
    private String mCurrentVin;
    private String[] mAvailableVins;

    AuthenticatedApi(String authenticationToken, String refreshToken, String xsrfToken, String currentVin, String[] availableVins) {
        mAuthenticationToken = authenticationToken;
        mRefreshToken = refreshToken;
        mXsrfToken = xsrfToken;
        mCurrentVin = currentVin;
        mAvailableVins = availableVins;
    }

    private AuthenticatedApi(Parcel in) {
        mAuthenticationToken = in.readString();
        mRefreshToken = in.readString();
        mXsrfToken = in.readString();
        mCurrentVin = in.readString();
        mAvailableVins = in.createStringArray();
    }

    public static final Creator<AuthenticatedApi> CREATOR = new Creator<AuthenticatedApi>() {
        @Override
        public AuthenticatedApi createFromParcel(Parcel in) {
            return new AuthenticatedApi(in);
        }

        @Override
        public AuthenticatedApi[] newArray(int size) {
            return new AuthenticatedApi[size];
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
            Log.e("ZEServicesApi", "Unable to parse token as UTF-8", e);
        } catch (JSONException e) {
            Log.e("ZEServicesApi", "Unable to decode token to verify", e);
        }
        return true;
    }

    private Observable<AuthenticatedApi> refresh(RequestQueue queue) {
        final String URL = ZEServicesApi.HOST + "/api/user/token/refresh";
        final Map<String, String> request = new HashMap<>();
        final RequestFuture<AuthenticatedApi> result = RequestFuture.newFuture();
        final Observable<AuthenticatedApi> o = Observable.from(result, Schedulers.io());
        request.put("token", mAuthenticationToken);
        final JsonObjectRequest refreshRequest = new JsonObjectRequest(
                Request.Method.POST,
                URL,
                new JSONObject(request),
                response -> {
                    try {
                        mAuthenticationToken = response.getString("token");
                        // TODO: Is the cookie updated here too?
                        result.onResponse(AuthenticatedApi.this);
                    } catch (JSONException e) {
                        Log.e("ZEServicesApi", "Unable to parse JSON response", e);
                        result.onErrorResponse(new VolleyError("Invalid response", e));
                    }
                },
                error -> {
                    Log.e("ZEServicesApi", "Bad response to refresh", error);
                    result.onErrorResponse(error);
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String>  params = new HashMap<>();
                params.put("X-XSRF-TOKEN", mXsrfToken);
                params.put("Cookie", mRefreshToken);
                return params;
            }
        };
        refreshRequest.setRetryPolicy(new DefaultRetryPolicy(
                ZEServicesApi.REQUEST_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));
        result.setRequest(refreshRequest);
        queue.add(refreshRequest);
        return o;
    }

    private <T> Observable<T> doRequest(final RequestQueue queue, RequestCreator<T> request) {
        if (hasExpired()) {
            final AsyncSubject<T> o = AsyncSubject.create();
            refresh(queue).subscribe(
                    api -> api.actualDoRequest(queue, request).subscribe(o),
                    o::onError
            );
            return o.asObservable();
        }
        return actualDoRequest(queue, request);
    }

    private <T> Observable<T> actualDoRequest(RequestQueue queue, RequestCreator<T> creator) {
        final RequestFuture<T> result = RequestFuture.newFuture();
        final Observable<T> o = Observable.from(result, Schedulers.io());
        Request<T> request = creator.get(result, mAuthenticationToken);
        request.setRetryPolicy(new DefaultRetryPolicy(
                ZEServicesApi.REQUEST_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));
        result.setRequest(request);
        queue.add(request);
        return o;
    }

    private <T> Observable<T> doVinRequest(final RequestQueue queue, final String vin, RequestCreator<T> request) {
        if (vin.equals(mCurrentVin)) {
            return doRequest(queue, request);
        } else {
            // Change to the new VIN and perform the request in a separate thread
            final AsyncSubject<T> o = AsyncSubject.create();
            setActive(queue, vin).subscribe(set -> {
                mCurrentVin = vin;
                if (hasExpired()) {
                    refresh(queue).subscribe(
                            api -> doRequest(queue, request).subscribe(o),
                            o::onError
                    );
                } else {
                    doRequest(queue, request).subscribe(o);
                }
            }, o::onError);
            return o.asObservable();
        }
    }

    public Observable<JSONObject> getBattery(RequestQueue queue, String vin) {
        final String URL = "/api/vehicle/" + vin + "/battery";
        return doVinRequest(queue, vin, new RequestCreator.JsonObjectRequestCreator(URL, false, null));
    }

    public Observable<JSONObject> setActive(RequestQueue queue, String vin) {
        final String URL = "/api/vehicle";
        final Map<String, String> request = new HashMap<>();
        request.put("active_vehicle", vin);
        return doRequest(queue, new RequestCreator.JsonObjectRequestCreator(URL, true, new JSONObject(request)));
    }

    public Observable<JSONArray> getChargeHistory(RequestQueue queue, String vin, int month, int year) {
        final String URL = String.format(Locale.ENGLISH, "/api/vehicle/%s/charge/history?begin=%02d%02d&end=%02d%02d", vin, month, year % 100, month, year % 100);
        return doVinRequest(queue, vin, new RequestCreator.JsonArrayRequestCreator(URL, false, null));
    }

    // Example response: [{charge_level: 0, date: 1544741109000, remaining_autonomy: 0, remaining_time: 0, result: "ERROR", type: "START_REQUEST"}]
    public Observable<JSONArray> getBatteryHistory(RequestQueue queue, String vin, int month, int year) {
        final String URL = String.format(Locale.ENGLISH, "/api/vehicle/%s/battery/history?begin=%02d%02d&end=%02d%02d", vin, month, year % 100, month, year % 100);
        return doVinRequest(queue, vin, new RequestCreator.JsonArrayRequestCreator(URL, false, null));
    }

    public Observable<JSONObject> getChargeSiblings(RequestQueue queue, String vin) {
        final String URL = "/api/vehicle/" + vin + "/charge/siblings";
        return doVinRequest(queue, vin, new RequestCreator.JsonObjectRequestCreator(URL,false, null));
    }

    public Observable<String> startPrecondition(RequestQueue queue, String vin) {
        final String URL = "/api/vehicle/" + vin + "/air-conditioning";
        return doVinRequest(queue, vin, new RequestCreator.StringRequestCreator(URL, true, null));
    }

    public Observable<JSONObject> schedulePrecondition(RequestQueue queue, String vin, String startTime) {
        final String URL = "/api/vehicle/" + vin + "/air-conditioning/scheduler";
        final Map<String, String> request = new HashMap<>();
        request.put("start", startTime);
        return doVinRequest(queue, vin, new RequestCreator.JsonObjectRequestCreator(URL, true, new JSONObject(request)));
    }

    public Observable<JSONObject> preconditionStatus(RequestQueue queue, String vin) {
        final String URL = "/api/vehicle/" + vin + "/air-conditioning/last";
        return doVinRequest(queue, vin, new RequestCreator.JsonObjectRequestCreator(URL, false, null));
    }

    public Observable<String> startCharge(RequestQueue queue, String vin) {
        final String URL = "/api/vehicle/" + vin + "/charge";
        return doVinRequest(queue, vin, new RequestCreator.StringRequestCreator(URL, true, null));
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
