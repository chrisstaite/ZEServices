package com.yourdreamnet.zeservices.ui.chargedata;

import androidx.lifecycle.ViewModelProviders;

import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.yourdreamnet.zeservices.MainActivity;
import com.yourdreamnet.zeservices.QueueSingleton;
import com.yourdreamnet.zeservices.R;
import com.yourdreamnet.zeservices.api.AuthenticatedApi;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChargeDataFragment extends Fragment {

    private ChargeDataViewModel mViewModel;

    public static ChargeDataFragment newInstance() {
        return new ChargeDataFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.charge_data_fragment, container, false);
    }

    private Locale getCurrentLocale(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            return getResources().getConfiguration().getLocales().get(0);
        } else{
            //noinspection deprecation
            return getResources().getConfiguration().locale;
        }
    }

    private static class MonthData {
        int mMonth;
        int mYear;

        MonthData(int month, int year) {
            mMonth = month;
            mYear = year;
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(ChargeDataViewModel.class);

        // Create a spinner with the last 13 months in it
        final int MONTH_LENGTH = 13;
        Spinner spinner = getView().findViewById(R.id.month_picker);
        List<CharSequence> months = new ArrayList<>(MONTH_LENGTH);
        final List<MonthData> data = new ArrayList<>(MONTH_LENGTH);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        Locale locale = getCurrentLocale();
        DateFormat dateFormat = new SimpleDateFormat(android.text.format.DateFormat.getBestDateTimePattern(locale, "MMMM yyyy"), locale);
        for (int i = 0; i < MONTH_LENGTH; i++) {
            months.add(dateFormat.format(calendar.getTime()));
            data.add(new MonthData(calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR)));
            calendar.add(Calendar.MONTH, -1);
        }
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(
            getContext(), android.R.layout.simple_spinner_item, months
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                MonthData monthData = data.get(adapterView.getSelectedItemPosition());
                loadChargeData(monthData.mMonth, monthData.mYear);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                mViewModel.clearChargeData();
                renderList();
            }
        });

        RecyclerView dataList = getView().findViewById(R.id.data_list);
        dataList.setLayoutManager(new LinearLayoutManager(getContext()));
        dataList.setAdapter(new ChargeDataAdapter(mViewModel));
    }

    private AuthenticatedApi getApi() {
        return ((MainActivity) getActivity()).getApi();
    }

    private void renderList() {
        RecyclerView dataList = getView().findViewById(R.id.data_list);
        dataList.getAdapter().notifyDataSetChanged();
    }

    private void loadChargeData(int month, int year) {
        AuthenticatedApi api = getApi();
        api.getChargeHistory(QueueSingleton.getQueue(), api.getCurrentVin(), month, year).
            subscribe(
                data -> {
                    mViewModel.setChargeData(data);
                    getActivity().runOnUiThread(this::renderList);
                },
                error -> {
                    Log.e("ChargeData", "Unable to load charge history", error);
                    mViewModel.clearChargeData();
                    getActivity().runOnUiThread(this::renderList);
                }
            );
    }

}
