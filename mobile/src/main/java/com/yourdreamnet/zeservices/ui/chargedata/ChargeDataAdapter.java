package com.yourdreamnet.zeservices.ui.chargedata;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.yourdreamnet.zeservices.R;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import androidx.recyclerview.widget.RecyclerView;

public class ChargeDataAdapter extends RecyclerView.Adapter<ChargeDataAdapter.ViewHolder> {

    private ChargeDataViewModel mViewModel;

    static class ViewHolder extends RecyclerView.ViewHolder {

        private View mView;

        ViewHolder(View view) {
            super(view);
            mView = view;
        }

        void displayItem(ChargeDataViewModel.ChargeNotification item) {
            TextView timestamp = mView.findViewById(R.id.timestamp);
            DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(mView.getContext());
            DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(mView.getContext());
            String lastUpdated = timeFormat.format(item.getDate()) + " " + dateFormat.format(item.getDate());
            timestamp.setText(lastUpdated);

            TextView type = mView.findViewById(R.id.type);
            type.setText(item.getType().getResource());

            TextView point = mView.findViewById(R.id.point);
            point.setText(item.getPoint().getResource());

            TextView chargeLevel = mView.findViewById(R.id.charge_level);
            chargeLevel.setText(String.format(mView.getContext().getString(R.string.charging_percentage), item.getLevelPercentage()));

            TextView completeTime = mView.findViewById(R.id.complete_time);
            Date finish = item.getEstimatedFinish();
            completeTime.setText(finish == null ? "" : timeFormat.format(finish));
        }

    }

    ChargeDataAdapter(ChargeDataViewModel viewModel) {
        mViewModel = viewModel;
    }

    @Override
    public ChargeDataAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).
            inflate(R.layout.charge_data_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.displayItem(mViewModel.getNotifications().get(position));
    }

    @Override
    public int getItemCount() {
        List<ChargeDataViewModel.ChargeNotification> notifications = mViewModel.getNotifications();
        if (notifications == null) {
            return 0;
        }
        return notifications.size();
    }

}
