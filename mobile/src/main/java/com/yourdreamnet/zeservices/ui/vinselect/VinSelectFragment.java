package com.yourdreamnet.zeservices.ui.vinselect;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.yourdreamnet.zeservices.ui.carstatus.CarStatusFragment;
import com.yourdreamnet.zeservices.QueueSingleton;
import com.yourdreamnet.zeservices.R;
import com.yourdreamnet.zeservices.ZEServicesAPI;

public class VinSelectFragment extends Fragment {

    private VinSelectViewModel mViewModel;
    private ZEServicesAPI.AuthenticatedAPI mAuthenticatedApi;

    public static VinSelectFragment newInstance() {
        return new VinSelectFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle arguments = getArguments();
        if (arguments != null) {
            mAuthenticatedApi = arguments.getParcelable("api");
        }
    }

    private void selectVin(String vin) {
        Fragment carFragment = CarStatusFragment.newInstance();
        Bundle arguments = new Bundle();
        arguments.putParcelable("api", mAuthenticatedApi);
        arguments.putString("vin", vin);
        carFragment.setArguments(arguments);
        getFragmentManager().beginTransaction().replace(R.id.container, carFragment).commitNow();
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

        ListView vinList = getView().findViewById(R.id.vinList);
        final String[] vins = mAuthenticatedApi.getAvailableVins();
        if (vins.length == 1) {
            if (!mAuthenticatedApi.getCurrentVin().equals(vins[0])) {
                mAuthenticatedApi.setActive(QueueSingleton.getQueue(getContext()), vins[0]);
            }
            new Handler().post(() -> selectVin(vins[0]));
        } else {

            vinList.setAdapter(new ArrayAdapter<>(
                    getContext(),
                    android.R.layout.simple_list_item_1,
                    vins
            ));
            vinList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    selectVin(vins[i]);
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }

            });
        }
    }

}
