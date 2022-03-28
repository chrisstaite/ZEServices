package com.yourdreamnet.zeservices.ui.chargedata;

import android.util.Log;

import com.yourdreamnet.zeservices.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.lifecycle.ViewModel;

public class ChargeDataViewModel extends ViewModel {

    private List<ChargeNotification> mNotifications;

    static class ChargeNotification {

        private Date mDate;
        private Date mEndDate;
        private int mDuration;
        private int mLevelPercentage;
        private String mEndStatus;

        private ChargeNotification(JSONObject notification) throws JSONException {
            SimpleDateFormat dateParser = new SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()
            );
            try {
                mDate = dateParser.parse(notification.getString("chargeStartDate"));
            } catch (ParseException e) {
                mDate = null;
            }
            try {
                mEndDate = dateParser.parse(notification.getString("chargeEndDate"));
            } catch (ParseException e) {
                mEndDate = null;
            }
            mDuration = notification.getInt("chargeDuration");
            mLevelPercentage = notification.getInt("chargeEndBatteryLevel");
            mEndStatus = notification.getString("chargeEndStatus");
        }

        public Date getDate() {
            return mDate;
        }

        public int getDuration() {
            return mDuration;
        }

        public String getEndStatus() { return mEndStatus; }

        public int getLevelPercentage() {
            return mLevelPercentage;
        }

        public Date getEndDate() {
            return mEndDate;
        }

    }

    void setChargeData(JSONArray notifications) {
        mNotifications = new ArrayList<>(notifications.length());
        for (int i = 0; i < notifications.length(); i++) {
            try {
                mNotifications.add(new ChargeNotification(notifications.getJSONObject(i)));
            } catch (JSONException e) {
                Log.e("ChargeData", "Error parsing charge data for index " + i, e);
            }
        }
    }

    void clearChargeData() {
        mNotifications.clear();
    }

    List<ChargeNotification> getNotifications() {
        return mNotifications;
    }

}
