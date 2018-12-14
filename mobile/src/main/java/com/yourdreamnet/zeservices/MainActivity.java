package com.yourdreamnet.zeservices;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager.widget.ViewPager;

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

        DrawerLayout drawer = findViewById(R.id.drawer);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,
                drawer,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        ){
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getActionBar().setTitle(getString(R.string.app_name));
                invalidateOptionsMenu();
            }

            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getActionBar().setTitle(getString(R.string.app_name));
                invalidateOptionsMenu();
            }
        };
        toggle.setDrawerIndicatorEnabled(true);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        ViewPager pager = findViewById(R.id.container);
        pager.setAdapter(new MainTabManager(getSupportFragmentManager(), this));

        if (mAuthenticatedApi.getCurrentVin().isEmpty()) {
            // Force the app drawer to be open
            // TODO: Add VIN selection back in
            drawer.openDrawer(GravityCompat.START, true);
        }
    }

    public ZEServicesAPI.AuthenticatedAPI getApi() {
        return mAuthenticatedApi;
    }

}
