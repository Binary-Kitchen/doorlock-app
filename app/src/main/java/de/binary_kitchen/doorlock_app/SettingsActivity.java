/*
 * Doorlock, Binary Kitchen's Open Sesame
 *
 * Copyright (c) Binary Kitchen e.V., 2018
 *
 * Authors:
 *  Ralf Ramsauer <ralf@binary-kitchen.de>
 *  Thomas Schmid <tom@binary-kitchen.de>
 *
 * This work is licensed under the terms of the GNU GPL, version 3.  See
 * the COPYING file in the top-level directory.
 */

package de.binary_kitchen.doorlock_app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.Toast;

import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompatDividers;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.settingsToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settingsFragmentContainer,new AppPreferenceFragment())
                .commit();
    }

    public static boolean check_settings(Context context)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String username = prefs.getString("username", "");
        String password = prefs.getString("password", "");
        String hostname = prefs.getString("hostname", "");

        if (username.isEmpty() || password.isEmpty() || hostname.isEmpty()) {
            Toast.makeText(context, R.string.message_invalid_credentials,
                    Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home) {
            if (!check_settings(this))
                return false;

            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static class AppPreferenceFragment extends PreferenceFragmentCompatDividers {

        @Override
        public void onCreatePreferencesFix(@Nullable Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.app_preferences,rootKey);
            setDividerPreferences(DIVIDER_DEFAULT);
        }
    }
}
