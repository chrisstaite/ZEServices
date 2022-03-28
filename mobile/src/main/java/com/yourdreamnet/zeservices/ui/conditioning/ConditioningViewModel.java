package com.yourdreamnet.zeservices.ui.conditioning;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.lifecycle.ViewModel;

@SuppressWarnings("WeakerAccess")
public class ConditioningViewModel extends ViewModel {

    private Date mLastPrecondition;
    private int mThreshold;  // Percentage where we allow this to be used, we ignore this of course...
    private String mLastResult;  // Seen off

    boolean setConditioningData(JSONObject conditioningData) {
        try {
            SimpleDateFormat dateParser = new SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()
            );
            try {
                mLastPrecondition = dateParser.parse(conditioningData.getString("lastUpdateTime"));
            } catch (ParseException e) {
                Log.e("Conditioning", "Unable to parse conditioning date", e);
                return false;
            }
            mThreshold = conditioningData.getInt("socThreshold");
            mLastResult = conditioningData.getString("hvacStatus");
            return true;
        } catch (JSONException e) {
            Log.e("Conditioning", "Unable to parse conditioning data", e);
            return false;
        }
    }

    Date getLastPrecondition() {
        return mLastPrecondition;
    }

    int getSocThreshold() {
        return mThreshold;
    }

    String getLastResult() {
        return mLastResult;
    }

}
