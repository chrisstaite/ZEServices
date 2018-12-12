package com.yourdreamnet.zeservices;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.yourdreamnet.zeservices.ui.vinselect.VinSelectFragment;

public class MainActivity extends AppCompatActivity {

    private ZEServicesAPI.AuthenticatedAPI mAuthenticatedApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check we are authenticated before we continue, otherwise return to the login screen
        mAuthenticatedApi = getIntent().getParcelableExtra("api");
        if (mAuthenticatedApi == null) {
            Intent startIntent = new Intent(this, LoginActivity.class);
            startActivity(startIntent);
            return;
        }

        setContentView(R.layout.main_activity);
        if (savedInstanceState == null) {
            Fragment vinFragment = VinSelectFragment.newInstance();
            Bundle arguments = new Bundle();
            arguments.putString("current", mAuthenticatedApi.getCurrentVin());
            arguments.putStringArray("available", mAuthenticatedApi.getAvailableVins());
            vinFragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, vinFragment)
                    .commitNow();
        }
    }

}
