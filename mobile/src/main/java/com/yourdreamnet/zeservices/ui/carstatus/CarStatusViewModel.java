package com.yourdreamnet.zeservices.ui.carstatus;

import android.arch.lifecycle.ViewModel;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

class CarStatusViewModel extends ViewModel {

    private static final double KM_IN_MILE = 0.621371;

    private boolean mCharging;
    private boolean mPluggedIn;
    private int mChargeLevel;
    private int mRangeKm;
    private Date mLastUpdated;
    private boolean mRangeMiles = true;

    boolean setBatteryData(JSONObject batteryData) {
        try {
            mCharging = batteryData.getBoolean("charging");
            mPluggedIn = batteryData.getBoolean("plugged");
            mChargeLevel = batteryData.getInt("charge_level");
            mRangeKm = batteryData.getInt("remaining_range");
            mLastUpdated = new Date(batteryData.getLong("last_update"));
            return true;
        } catch (JSONException e) {
            Log.e("CarStatus", "Unable to parse battery data", e);
            return false;
        }
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
