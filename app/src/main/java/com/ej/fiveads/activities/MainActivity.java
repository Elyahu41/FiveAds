package com.ej.fiveads.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.ej.fiveads.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    public static String mDeviceDefaults = "DeviceDefaults";
    private FloatingActionButton mSettingFab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {//TODO change display name, splash screen, maybe add another ad
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Objects.requireNonNull(getSupportActionBar()).hide();

        setupFAB();

        BottomNavigationView navView = findViewById(R.id.nav_view);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_earn, R.id.navigation_spend)
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
        });
    }

    private void setupFAB() {
        mSettingFab = findViewById(R.id.settings_fab);
        mSettingFab.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
    }
}