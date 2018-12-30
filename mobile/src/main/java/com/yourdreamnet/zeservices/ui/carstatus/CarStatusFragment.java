package com.yourdreamnet.zeservices.ui.carstatus;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.yourdreamnet.zeservices.LoginActivity;
import com.yourdreamnet.zeservices.MainActivity;
import com.yourdreamnet.zecommon.api.QueueSingleton;
import com.yourdreamnet.zeservices.R;
import com.yourdreamnet.zecommon.api.AuthenticatedApi;

import java.text.DateFormat;
import java.util.Date;
import java.util.Objects;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import static android.content.Context.MODE_PRIVATE;

public class CarStatusFragment extends Fragment {

    private static final String PREFERENCE_FILE = "options";
    private static final String MILES = "miles";

    private CarStatusViewModel mViewModel;

    @SuppressWarnings("unused")
    public static CarStatusFragment newInstance() {
        return new CarStatusFragment();
    }

    private AuthenticatedApi getApi() {
        return ((MainActivity) getActivity()).getApi();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.car_status_fragment, container, false);
    }

    private void setLoading(boolean loading) {
        ProgressBar progress = Objects.requireNonNull(getView()).findViewById(R.id.loading);
        ConstraintLayout layout = getView().findViewById(R.id.layout);
        for (int i = 0; i < layout.getChildCount(); i++) {
            View view = layout.getChildAt(i);
            if (view != progress) {
                view.setVisibility(loading ? View.GONE : View.VISIBLE);
            }
        }
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void goToLogin() {
        Intent loginIntent = new Intent(getContext(), LoginActivity.class);
        loginIntent.setFlags(loginIntent.getFlags() | Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(loginIntent);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(CarStatusViewModel.class);

        SharedPreferences sharedPref = getContext().getSharedPreferences(
            PREFERENCE_FILE, MODE_PRIVATE
        );
        mViewModel.setRangeMiles(sharedPref.getBoolean(MILES, true));

        Button toggle = Objects.requireNonNull(getView()).findViewById(R.id.rangeToggle);
        toggle.setText(mViewModel.isRangeMiles() ? R.string.miles : R.string.km);
        toggle.setOnClickListener(this::toggleRange);

        Button startCharging = getView().findViewById(R.id.startCharge);
        startCharging.setOnClickListener(this::startCharging);

        if (mViewModel.getLastUpdate() == null) {
            loadData();
        } else {
            updateView();
            // The car status is updated every 15 minutes
            Date fifteenMinutesAgo = new Date(new Date().getTime() - (15 * 60 * 1000));
            if (mViewModel.getLastUpdate().before(fifteenMinutesAgo)) {
                loadData();
            }
        }
    }

    private void loadData() {
        setLoading(true);
        AuthenticatedApi api = getApi();
        api.getBattery(QueueSingleton.getQueue(), api.getCurrentVin()).subscribe(
            batteryData -> Objects.requireNonNull(getActivity()).runOnUiThread(() -> {
                setLoading(false);
                if (!mViewModel.setBatteryData(batteryData)) {
                    goToLogin();
                } else {
                    updateView();
                }
            }),
            error -> {
                Log.e("CarStatus", "Could not retrieve car status", error);
                Objects.requireNonNull(getActivity()).runOnUiThread(() -> {
                    setLoading(false);
                    goToLogin();
                });
            }
        );
    }

    private void updateView() {
        DateFormat dateFormat = android.text.format.DateFormat.getLongDateFormat(getContext());
        DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(getContext());
        String lastUpdated = timeFormat.format(mViewModel.getLastUpdate()) + " " + dateFormat.format(mViewModel.getLastUpdate());
        TextView lastUpdatedView = Objects.requireNonNull(getView()).findViewById(R.id.lastUpdated);
        lastUpdatedView.setText(lastUpdated);

        ProgressBar progressBar = getView().findViewById(R.id.chargeLevel);
        TextView percentageView = getView().findViewById(R.id.chargePercentage);
        int level = mViewModel.getChargeLevel();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            progressBar.setProgress(level, true);
        } else {
            progressBar.setProgress(level);
        }
        percentageView.setText(String.format(getString(R.string.charging_percentage), level));

        TextView chargingText = getView().findViewById(R.id.chargingText);
        ImageView charging = getView().findViewById(R.id.charging);
        if (mViewModel.isCharging()) {
            chargingText.setText(R.string.charging);
            charging.setImageResource(R.drawable.ic_baseline_charging);
        } else {
            chargingText.setText(R.string.not_charging);
            charging.setImageResource(R.drawable.ic_baseline_not_charging);
        }

        TextView pluggedText = getView().findViewById(R.id.pluggedText);
        ImageView plugged = getView().findViewById(R.id.plugged);
        if (mViewModel.isPluggedIn()) {
            pluggedText.setText(R.string.plugged_in);
            plugged.setImageResource(R.drawable.ic_baseline_power);
        } else {
            pluggedText.setText(R.string.not_plugged_in);
            plugged.setImageResource(R.drawable.ic_baseline_power_off);
        }

        TextView range = getView().findViewById(R.id.range);
        range.setText(String.valueOf(mViewModel.getRange()));

        Button startCharging = getView().findViewById(R.id.startCharge);
        startCharging.setVisibility(
            !mViewModel.isCharging() ? View.VISIBLE : View.INVISIBLE
        );
    }

    private void toggleRange(View button) {
        boolean miles = !mViewModel.isRangeMiles();
        SharedPreferences sharedPref = getContext().getSharedPreferences(
            PREFERENCE_FILE, MODE_PRIVATE
        );
        sharedPref.edit().putBoolean(MILES, miles).apply();
        ((Button) button).setText(miles ? R.string.miles : R.string.km);
        mViewModel.setRangeMiles(miles);
        TextView range = Objects.requireNonNull(getView()).findViewById(R.id.range);
        range.setText(String.valueOf(mViewModel.getRange()));
    }

    private void startCharging(View button) {
        button.setVisibility(View.INVISIBLE);
        AuthenticatedApi api = getApi();
        api.startCharge(QueueSingleton.getQueue(), api.getCurrentVin()).subscribe(
            response -> {
                mViewModel.setCharging(true);
                Objects.requireNonNull(getActivity()).runOnUiThread(this::updateView);
            },
            error -> {
                Log.e("CarStatus", "Unable to start charging", error);
                Objects.requireNonNull(getActivity()).runOnUiThread(
                    () -> button.setVisibility(View.VISIBLE)
                );
            }
        );
    }

}
