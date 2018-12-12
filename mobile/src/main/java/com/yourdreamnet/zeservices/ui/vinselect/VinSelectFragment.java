package com.yourdreamnet.zeservices.ui.vinselect;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.yourdreamnet.zeservices.R;
import com.yourdreamnet.zeservices.ZEServicesAPI;

import java.util.Arrays;

public class VinSelectFragment extends Fragment {

    private VinSelectViewModel mViewModel;
    private String mCurrentVin;
    private String[] mAvailableVins;

    public static VinSelectFragment newInstance() {
        return new VinSelectFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle arguments = getArguments();
        if (arguments != null) {
            mCurrentVin = arguments.getString("current");
            mAvailableVins = arguments.getStringArray("available");
        } else {
            mCurrentVin = "";
            mAvailableVins = new String[0];
        }

        Log.d("VinSelect", "Current VIN: " + mCurrentVin);
        Log.d("VinSelect", "Available VINs: " + Arrays.toString(mAvailableVins));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.vin_select_fragment, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(VinSelectViewModel.class);
        // TODO: Use the ViewModel
    }

}
