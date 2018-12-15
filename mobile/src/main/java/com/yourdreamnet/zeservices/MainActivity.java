package com.yourdreamnet.zeservices;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.material.navigation.NavigationView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager.widget.ViewPager;

public class MainActivity extends AppCompatActivity {

    private ZEServicesAPI.AuthenticatedAPI mAuthenticatedApi;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.logout:
                Intent startIntent = new Intent(this, LoginActivity.class);
                startIntent.putExtra("logout", true);
                startActivity(startIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

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
        );
        toggle.setDrawerIndicatorEnabled(true);
        drawer.addDrawerListener(toggle);

        populateVinSelector();

        ViewPager pager = findViewById(R.id.container);
        pager.setAdapter(new MainTabManager(getSupportFragmentManager(), this));

        if (mAuthenticatedApi.getCurrentVin().isEmpty()) {
            // Force the app drawer to be open
            drawer.openDrawer(GravityCompat.START, true);
        }
    }

    private void populateVinSelector() {
        NavigationView nav = findViewById(R.id.nav_view);
        String[] vins = mAuthenticatedApi.getAvailableVins();
        String current = mAuthenticatedApi.getCurrentVin();
        Menu menu = nav.getMenu();
        for (int i = 0; i < vins.length; i++) {
            MenuItem item = menu.add(0, i, i, vins[i]);
            if (vins[i].equals(current)) {
                item.setChecked(true);
            }
        }

        nav.setNavigationItemSelectedListener(item -> {
            DrawerLayout drawer = findViewById(R.id.drawer);
            item.setChecked(true);
            String vin = vins[item.getOrder()];
            if (!vin.equals(mAuthenticatedApi.getCurrentVin())) {
                mAuthenticatedApi.setActive(QueueSingleton.getQueue(), vin).subscribe(result -> {
                    // TODO: Refresh current fragment
                    drawer.closeDrawers();
                }, error -> Log.e("VinSelect", "Unable to pick VIN", error));
            } else {
                drawer.closeDrawers();
            }
            return true;
        });
    }

    public ZEServicesAPI.AuthenticatedAPI getApi() {
        return mAuthenticatedApi;
    }

}
