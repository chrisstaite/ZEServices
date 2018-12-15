package com.yourdreamnet.zeservices.ui.conditioning;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

import androidx.lifecycle.ViewModel;

public class ConditioningViewModel extends ViewModel {

    private Date mLastPrecondition;
    private String mLastType;  // Seen USER_REQUEST
    private String mLastResult;  // Seen SUCCESS

    boolean setConditioningData(JSONObject conditioningData) {
        try {
            mLastPrecondition = new Date(conditioningData.getLong("date"));
            mLastType = conditioningData.getString("type");
            mLastResult = conditioningData.getString("result");
            return true;
        } catch (JSONException e) {
            Log.e("Conditioning", "Unable to parse conditioning data", e);
            return false;
        }
    }

    Date getLastPrecondition() {
        return mLastPrecondition;
    }

    String getLastType() {
        return mLastType;
    }

    String getLastResult() {
        return mLastResult;
    }

}
