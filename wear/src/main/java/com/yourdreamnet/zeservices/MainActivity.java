package com.yourdreamnet.zeservices;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeClient;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.yourdreamnet.zecommon.CredentialStore;
import com.yourdreamnet.zecommon.api.QueueSingleton;
import com.yourdreamnet.zecommon.api.ZEServicesApi;

import java.util.Iterator;
import java.util.List;

import androidx.annotation.NonNull;

public class MainActivity extends WearableActivity implements DataClient.OnDataChangedListener {

    private CredentialStore mStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Enables Always-on
        setAmbientEnabled();

        // Check if we've already got the login details saved
        mStore = new CredentialStore(this);
        CredentialStore.Credentials credentials = mStore.loadLoginSecure();
        if (credentials.email().isEmpty() || credentials.password().isEmpty()) {
            pullLoginDetails();
        } else {
            credentialsTransferred(credentials);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Wearable.getDataClient(this).addListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Wearable.getDataClient(this).removeListener(this);
    }

    private void pullLoginDetails() {
        NodeClient client = Wearable.getNodeClient(this);
        client.getConnectedNodes().addOnCompleteListener(results -> {
            List<Node> nodes = results.getResult();
            if (nodes != null) {
                Iterator<Node> it = nodes.iterator();
                tryToGetLoginDetails(it);
            } else {
                setNoLoginAvailable();
            }
        });
    }

    private void tryToGetLoginDetails(Iterator<Node> iterator) {
        if (iterator.hasNext()) {
            Node node = iterator.next();
            if (!node.isNearby()) {
                tryToGetLoginDetails(iterator);
                return;
            }
            Uri uri = new Uri.Builder()
                    .scheme(PutDataRequest.WEAR_URI_SCHEME)
                    .path("/zeservices/credentials")
                    .authority(node.getId())
                    .build();
            Wearable.getDataClient(MainActivity.this).getDataItem(uri).addOnCompleteListener(data -> {
                DataItem item = data.getResult();
                if (item == null) {
                    tryToGetLoginDetails(iterator);
                } else {
                    DataMap map = DataMapItem.fromDataItem(item).getDataMap();
                    String email = map.getString("email");
                    String password = map.getString("password");
                    mStore.saveLoginSecure(email, password);
                    credentialsTransferred(new CredentialStore.Credentials(email, password));
                }
            });
        } else {
            setNoLoginAvailable();
        }
    }

    private void setNoLoginAvailable() {
        TextView loadingText = findViewById(R.id.loading_text);
        loadingText.setText(R.string.load_failed);
        ProgressBar progress = findViewById(R.id.progressBar);
        progress.setVisibility(View.GONE);
    }



    private void credentialsTransferred(CredentialStore.Credentials credentials) {
        final TextView loadingText = findViewById(R.id.loading_text);
        loadingText.setText(R.string.login_complete);
        ProgressBar progress = findViewById(R.id.progressBar);
        progress.setVisibility(View.GONE);

        new ZEServicesApi(credentials.email(), credentials.password()).
                getAuthenticated(QueueSingleton.getQueue()).
                subscribe(
                        api -> runOnUiThread(() -> {
                            Intent startIntent = new Intent(this, CarDetails.class);
                            startIntent.putExtra("api", api);
                            startIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                            startActivity(startIntent);
                        }),
                        error -> runOnUiThread(() -> {
                            Log.e("LoginActivity", "Unable to authenticate", error);
                            loadingText.setText(R.string.login_invalid);
                        })
                );
    }

    @Override
    public void onDataChanged(@NonNull DataEventBuffer dataEventBuffer) {
        for (DataEvent event : dataEventBuffer) {
            DataItem item = event.getDataItem();
            if (item.getUri().getPath().equals("/zeservices/credentials")) {
                switch (event.getType()) {
                    case DataEvent.TYPE_CHANGED: {
                        DataMap map = DataMapItem.fromDataItem(item).getDataMap();
                        mStore.saveLoginSecure(map.getString("email"), map.getString("password"));
                        credentialsTransferred(new CredentialStore.Credentials(map.getString("email"), map.getString("password")));
                        break;
                    }
                    case DataEvent.TYPE_DELETED:
                        mStore.clear();
                        break;
                }
            }
        }
    }

}
