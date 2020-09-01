package com.yourdreamnet.zeservices;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.android.volley.RequestQueue;
import com.google.android.material.navigation.NavigationView;
import com.yourdreamnet.zecommon.api.QueueSingleton;
import com.yourdreamnet.zecommon.api.Vehicle;
import com.yourdreamnet.zecommon.api.VehicleAccount;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.ViewPager;

import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private static final String API_STORE = "api";
    private VehicleAccount mAuthenticatedApi;
    private List<Vehicle> mVehicles = null;
    private int mSelected = -1;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.logout) {
            Intent startIntent = new Intent(this, LoginActivity.class);
            startIntent.putExtra("logout", true);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(startIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mAuthenticatedApi = savedInstanceState.getParcelable(API_STORE);
        }

        setContentView(R.layout.main_activity);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putParcelable(API_STORE, mAuthenticatedApi);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();

        // Check we are authenticated before we continue, otherwise return to the login screen
        VehicleAccount intentApi = getIntent().getParcelableExtra("api");
        if (intentApi == null) {
            if (mAuthenticatedApi == null) {
                Intent startIntent = new Intent(this, LoginActivity.class);
                startIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(startIntent);
                return;
            }
        } else {
            mAuthenticatedApi = intentApi;
        }

        DrawerLayout drawer = findViewById(R.id.drawer);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,
                drawer,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        toggle.setDrawerIndicatorEnabled(true);
        drawer.addDrawerListener(toggle);

        ViewPager pager = findViewById(R.id.container);
        pager.setAdapter(new MainTabManager(getSupportFragmentManager(), this));

        RequestQueue queue = QueueSingleton.getQueue();
        mAuthenticatedApi.getVehicles(queue).subscribe(
                vehicles -> {
                    mVehicles = vehicles;
                    if (mVehicles.size() == 1) {
                        mSelected = 0;
                    }
                    runOnUiThread(() -> {
                        populateVinSelector();
                        if (mVehicles.size() > 1) {
                            drawer.openDrawer(GravityCompat.START, true);
                        } else {
                            Log.i("MainActivity", "Reloading main tab");
                            pager.setCurrentItem(-1);
                        }
                    });
                },
                error -> Log.e("VinSelect", "Unable to find vehicles", error)
        );
    }

    private void populateVinSelector() {
        NavigationView nav = findViewById(R.id.nav_view);
        Menu menu = nav.getMenu();
        for (int i = 0; i < mVehicles.size(); i++) {
            String vin = mVehicles.get(i).vin();
            MenuItem item = menu.add(0, i, i, vin);
            if (i == mSelected) {
                item.setChecked(true);
            }
        }

        nav.setNavigationItemSelectedListener(item -> {
            DrawerLayout drawer = findViewById(R.id.drawer);
            item.setChecked(true);
            if (mSelected != item.getOrder())
            {
                mSelected = item.getOrder();
                drawer.closeDrawers();
            }
            return true;
        });
    }

    public Vehicle getApi() {
        if (mSelected == -1 || mVehicles == null) {
            return null;
        }
        return mVehicles.get(mSelected);
    }

}
