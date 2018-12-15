package com.yourdreamnet.zeservices.ui.chargedata;

import android.util.Log;

import com.yourdreamnet.zeservices.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import androidx.lifecycle.ViewModel;

public class ChargeDataViewModel extends ViewModel {

    private List<ChargeNotification> mNotifications;

    public enum ChargeType {
        Start("START_NOTIFICATION", R.string.started_charge),
        End("END_NOTIFICATION", R.string.ended_charge),
        Unknown("", R.string.unknown);

        private String mName;
        private int mResource;

        ChargeType(String name, int resource) {
            mName = name;
            mResource = resource;
        }

        int getResource() {
            return mResource;
        }

        static ChargeType getType(String name) {
            for (ChargeType type : ChargeType.values()) {
                if (type.mName.equals(name)) {
                    return type;
                }
            }
            return Unknown;
        }
    }

    public enum PointType {
        Accelerated("ACCELERATED", R.string.accelerated),
        Invalid("INVALID", R.string.invalid),
        Slow("SLOW", R.string.slow),
        Unknown("", R.string.unknown);

        private String mName;
        private int mResource;

        PointType(String name, int resource) {
            mName = name;
            mResource = resource;
        }

        int getResource() {
            return mResource;
        }

        static PointType getType(String name) {
            for (PointType type : PointType.values()) {
                if (type.mName.equals(name)) {
                    return type;
                }
            }
            return Unknown;
        }
    }

    static class ChargeNotification {

        private Date mDate;
        private ChargeType mType;
        private PointType mPoint;
        private int mLevelPercentage;
        private int mRangeKm;
        private Date mEstimatedFinish;

        private ChargeNotification(JSONObject notification) throws JSONException {
            mDate = new Date(notification.getLong("date"));
            mType = ChargeType.getType(notification.getString("type"));
            mPoint = PointType.getType(notification.getString("charging_point"));
            mLevelPercentage = notification.getInt("charge_level");
            mRangeKm = notification.getInt("remaining_autonomy");
            if (mType == ChargeType.Start) {
                try {
                    mEstimatedFinish = new Date(mDate.getTime() + notification.getLong("remaining_time"));
                } catch (JSONException e) {
                    // This is a weird edge-case, but it does happen
                    mEstimatedFinish = null;
                }
            } else {
                mEstimatedFinish = null;
            }
        }

        public Date getDate() {
            return mDate;
        }

        public ChargeType getType() {
            return mType;
        }

        public PointType getPoint() {
            return mPoint;
        }

        public int getLevelPercentage() {
            return mLevelPercentage;
        }

        public int getRangeKm() {
            return mRangeKm;
        }

        public Date getEstimatedFinish() {
            return mEstimatedFinish;
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
