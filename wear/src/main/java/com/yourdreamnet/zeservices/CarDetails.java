package com.yourdreamnet.zeservices;

import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.yourdreamnet.zecommon.api.QueueSingleton;
import com.yourdreamnet.zecommon.api.Vehicle;

import java.util.List;

public class CarDetails extends WearableActivity {

    private List<Vehicle> mApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_details);

        // Enables Always-on
        setAmbientEnabled();

        Button startButton = findViewById(R.id.precondition);
        startButton.setOnClickListener(view -> {
            final TextView status = findViewById(R.id.status);
            mApi.get(0).startPrecondition(QueueSingleton.getQueue()).
                    subscribe(
                            result -> runOnUiThread(() -> status.setText(R.string.started_condition)),
                            error -> runOnUiThread(() -> {
                                Log.e("Conditioning", "Unable to pre-condition car", error);
                                status.setText(R.string.error_conditioning);
                            })
                    );
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        List<Vehicle> intentApi = getIntent().getParcelableExtra("api");
        if (intentApi == null) {
            if (mApi == null) {
                Intent startIntent = new Intent(this, MainActivity.class);
                startIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(startIntent);
            }
        } else {
            mApi = intentApi;
        }
    }

}
