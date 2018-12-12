package com.yourdreamnet.zeservices.ui.carstatus;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.yourdreamnet.zeservices.LoginActivity;
import com.yourdreamnet.zeservices.QueueSingleton;
import com.yourdreamnet.zeservices.R;
import com.yourdreamnet.zeservices.ZEServicesAPI;

import java.text.DateFormat;
import java.util.Objects;

public class CarStatusFragment extends Fragment {

    private CarStatusViewModel mViewModel;
    private ZEServicesAPI.AuthenticatedAPI mAuthenticatedApi;
    private String mVin;

    public static CarStatusFragment newInstance() {
        return new CarStatusFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle arguments = getArguments();
        if (arguments != null) {
            mAuthenticatedApi = arguments.getParcelable("api");
            mVin = arguments.getString("vin");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.car_status_fragment, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(CarStatusViewModel.class);

        mAuthenticatedApi.getBattery(QueueSingleton.getQueue(getContext()), mVin).subscribe(
            batteryData -> {
                if (!mViewModel.setBatteryData(batteryData)) {
                    startActivity(new Intent(getContext(), LoginActivity.class));
                } else {
                    Objects.requireNonNull(getActivity()).runOnUiThread(this::updateView);
                }
            },
            error -> {
                Log.e("CarStatus", "Could not retrieve car status", error);
                startActivity(new Intent(getContext(), LoginActivity.class));
            }
        );

        Button toggle = getView().findViewById(R.id.rangeToggle);
        toggle.setText(mViewModel.isRangeMiles() ? R.string.miles : R.string.km);
        toggle.setOnClickListener(this::toggleRange);
    }

    private void updateView() {
        DateFormat dateFormat = android.text.format.DateFormat.getLongDateFormat(getContext());
        String lastUpdated = dateFormat.format(mViewModel.getLastUpdate());
        TextView lastUpdatedView = Objects.requireNonNull(getView()).findViewById(R.id.lastUpdated);
        lastUpdatedView.setText(lastUpdated);

        ProgressBar progressBar = getView().findViewById(R.id.chargeLevel);
        TextView percentageView = getView().findViewById(R.id.chargePercentage);
        int level = mViewModel.getChargeLevel();
        progressBar.setMax(100);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            progressBar.setProgress(level, true);
        } else {
            progressBar.setProgress(level);
        }
        percentageView.setText(String.format(getString(R.string.charging_percentage), String.valueOf(level)));

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
    }

    void toggleRange(View button) {
        boolean miles = !mViewModel.isRangeMiles();
        ((Button) button).setText(miles ? R.string.miles : R.string.km);
        mViewModel.setRangeMiles(miles);
        TextView range = getView().findViewById(R.id.range);
        range.setText(String.valueOf(mViewModel.getRange()));
    }

}
