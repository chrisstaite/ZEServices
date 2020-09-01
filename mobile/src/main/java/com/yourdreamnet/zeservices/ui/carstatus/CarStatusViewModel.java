package com.yourdreamnet.zeservices.ui.carstatus;

import android.annotation.SuppressLint;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import androidx.lifecycle.ViewModel;

public class CarStatusViewModel extends ViewModel {

    private static final double KM_IN_MILE = 0.621371;

    private boolean mCharging;
    private boolean mPluggedIn;
    private int mChargeLevel;
    private int mRangeKm;
    private Date mLastUpdated;
    private boolean mRangeMiles = true;

    @SuppressLint("SimpleDateFormat")
    boolean setBatteryData(JSONObject batteryData) {
        try {
            mCharging = batteryData.getInt("chargingStatus") == 1;
            mPluggedIn = batteryData.getInt("plugStatus") == 1;
            mChargeLevel = batteryData.getInt("batteryLevel");
            mRangeKm = batteryData.getInt("batteryAutonomy");
            try {
                mLastUpdated = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").parse(
                        batteryData.getString("timestamp")
                );
            } catch (ParseException e) {
                mLastUpdated = new Date();
            }
            return true;
        } catch (JSONException e) {
            Log.e("CarStatus", "Unable to parse battery data", e);
            return false;
        }
    }

    void setCharging(boolean charging) {
        mCharging = charging;
    }

    void setRangeMiles(boolean miles) {
        mRangeMiles = miles;
    }

    boolean isRangeMiles() {
        return mRangeMiles;
    }

    boolean isCharging() {
        return mCharging;
    }

    boolean isPluggedIn() {
        return mPluggedIn;
    }

    int getChargeLevel() {
        return mChargeLevel;
    }

    int getRange() {
        if (mRangeMiles) {
            return (int) Math.round(mRangeKm * KM_IN_MILE);
        }
        return mRangeKm;
    }

    Date getLastUpdate() {
        return mLastUpdated;
    }

}
