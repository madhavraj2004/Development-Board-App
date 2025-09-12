package com.example.whatapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.whatapp.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_NIGHT_MODE = "night_mode"; // "system" | "light" | "dark"

    private ActivityMainBinding binding;

    // Apply saved theme BEFORE super.onCreate to reduce flicker
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applySavedThemeMode();

        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_map)
                .build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
    }

    /**
     * Read preference and apply AppCompatDelegate mode.
     * Safe: no unreachable statements here.
     */
    private void applySavedThemeMode() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String mode = prefs.getString(KEY_NIGHT_MODE, "system");

        if ("light".equals(mode)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if ("dark".equals(mode)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    /**
     * Save + apply the choice and recreate the activity.
     * recreate() is the last operation here.
     */
    public void setThemeChoice(String choice) {
        if (choice == null) choice = "system";

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_NIGHT_MODE, choice).apply();

        if ("light".equals(choice)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if ("dark".equals(choice)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }

        // Recreate to apply theme (no code after this)
        recreate();
    }

    /**
     * Return the currently saved choice.
     */
    public String getSavedThemeChoice() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_NIGHT_MODE, "system");
    }

    // ----------------------------
    // Menu + Theme dialog handling
    // ----------------------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // requires res/menu/menu_main.xml to exist
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_theme) {
            showThemeDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showThemeDialog() {
        final String[] choices = {"System default", "Light", "Dark"};

        int checkedItem = 0;
        String saved = getSavedThemeChoice();
        if ("light".equals(saved)) checkedItem = 1;
        else if ("dark".equals(saved)) checkedItem = 2;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose theme")
                .setSingleChoiceItems(choices, checkedItem, (dialog, which) -> {
                    // nothing here; apply on positive button
                })
                .setPositiveButton("Apply", (dialog, which) -> {
                    int selected = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    switch (selected) {
                        case 1:
                            setThemeChoice("light");
                            break;
                        case 2:
                            setThemeChoice("dark");
                            break;
                        default:
                            setThemeChoice("system");
                            break;
                    }
                })
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .show();
    }
}
