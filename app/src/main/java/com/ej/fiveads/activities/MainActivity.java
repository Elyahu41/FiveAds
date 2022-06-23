package com.ej.fiveads.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.ej.fiveads.R;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    public static String mDeviceDefaults = "DeviceDefaults";
    private FloatingActionButton mSettingFab;
    private AppOpenManager appOpenManager;
    private AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        appOpenManager = new AppOpenManager(getApplication());
        MobileAds.initialize(this, initializationStatus -> appOpenManager.showAdIfAvailable());

        setTheme(R.style.AppTheme);//splash screen
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Objects.requireNonNull(getSupportActionBar()).hide();

        setupFAB();

        BottomNavigationView navView = findViewById(R.id.nav_view);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_earn, R.id.navigation_spend, R.id.navigation_leaderboard, R.id.navigation_redeem)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.toString().contains("Spend")) {
                mSettingFab.hide();
            }
            if (destination.toString().contains("Earn")) {
                mSettingFab.show();
            }
            if (destination.toString().contains("Leaderboard")) {
                mSettingFab.hide();
            }
            if (destination.toString().contains("Redeem")) {
                mSettingFab.hide();
            }
        });

        mAdView = findViewById(R.id.adView);
        mAdView.loadAd(new AdRequest.Builder().build());
    }

    private void setupFAB() {
        mSettingFab = findViewById(R.id.settings_fab);
        mSettingFab.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAdView != null) {
            mAdView.resume();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAdView != null) {
            mAdView.destroy();
        }
    }

    @Override
    public void onPause() {
        if (mAdView != null) {
            mAdView.pause();
        }
        super.onPause();
    }
}