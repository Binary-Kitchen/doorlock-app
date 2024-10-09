/*
 * Doorlock, Binary Kitchen's Open Sesame
 *
 * Copyright (c) Binary Kitchen e.V., 2018
 *
 * Authors:
 *  Ralf Ramsauer <ralf@binary-kitchen.de>
 *  Thomas Schmid <tom@binary-kitchen.de>
 *
 * This work is licensed under the terms of the GNU GPL, version 2.  See
 * the COPYING file in the top-level directory.
 */

package de.binary_kitchen.doorlock_app;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Random;

import de.binary_kitchen.doorlock_app.doorlock_api.ApiCommand;
import de.binary_kitchen.doorlock_app.doorlock_api.DoorlockApi;
import de.binary_kitchen.doorlock_app.doorlock_api.ApiResponse;

import static android.net.wifi.WifiManager.WIFI_STATE_DISABLED;
import static android.net.wifi.WifiManager.WIFI_STATE_DISABLING;
import static android.net.wifi.WifiManager.WIFI_STATE_UNKNOWN;

public class MainActivity extends AppCompatActivity {
    private boolean connectivity;
    private DoorlockApi api;
    private TextView statusView;
    private ImageView logo;
    private SwipeRefreshLayout swipeRefreshLayout;

    private WifiReceiver broadcastReceiver;

    private boolean sounds_enabled;
    public enum SoundType {
        REQUEST,
        OKAY,
        ERROR,
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.mainToolbar);
        setSupportActionBar(toolbar);

        button_set_alpha(R.id.Lock, R.color.colorLocked);
        button_set_alpha(R.id.Present, R.color.colorPresent);
        button_set_alpha(R.id.Unlock, R.color.colorUnlocked);

        statusView = findViewById(R.id.statusTextView);
        logo = findViewById(R.id.logo);
        swipeRefreshLayout = findViewById(R.id.swiperefresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                update_status();
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void button_set_alpha(int resource, int color)
    {
        Drawable db = ((ImageButton)findViewById(resource)).getDrawable();

        db.setAlpha(192);
        db.setColorFilter(ContextCompat.getColor(this, color), PorterDuff.Mode.MULTIPLY);
    }

    private void logo_set_color(int color)
    {
        DrawableCompat.setTint(logo.getDrawable(), ContextCompat.getColor(this, color));
    }

    private void update_widgets()
    {
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(this);
        Intent intent = new Intent(getApplicationContext(), SpaceWidget.class);

        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = widgetManager.getAppWidgetIds(new ComponentName(this, SpaceWidget.class));

        widgetManager.notifyAppWidgetViewDataChanged(ids, android.R.id.list);

        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);
    }

    @Override
    protected void onResume()
    {
        String username, password, hostname;
        SharedPreferences prefs;
        boolean debug;

        super.onResume();

        if (!SettingsActivity.check_settings(this)) {
            this.startActivity(new Intent(this, SettingsActivity.class));
            return;
        }

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        username = prefs.getString("username", "");
        password = prefs.getString("password", "");
        hostname = prefs.getString("hostname", "");
        debug = prefs.getBoolean("debug", false);

        sounds_enabled = prefs.getBoolean("soundsEnabled",true);

        api = new DoorlockApi(this, hostname, username, password, debug);

        connectivity = true;

        update_status();
    }

    @Override
    public void onPause()
    {
        super.onPause();

        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver);
            broadcastReceiver = null;
        }
    }

    private void play(SoundType type)
    {
        MediaPlayer mp;
        Random r = new Random();
        int resid = R.raw.input_request;

        if (!sounds_enabled)
            return;

        switch (type)
        {
            case OKAY:
                if (r.nextDouble() <= 0.05)
                    resid = R.raw.drum;
                else
                    resid = R.raw.input_ok;
                break;
            case ERROR:
                if (r.nextDouble() <= 0.05)
                    resid = R.raw.haha;
                else
                    resid = R.raw.alert;
                break;
            case REQUEST:
                if (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.WEDNESDAY &&
                        r.nextDouble() <= 0.7)
                    resid = R.raw.wednesday;
                break;
        }

        mp =  MediaPlayer.create(this, resid);
        mp.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        return true;
    }

    private void api_request(ApiCommand command)
    {
        if (connectivity) {
            play(SoundType.REQUEST);
            api.issueCommand(command);
        } else {
            play(SoundType.ERROR);
            Toast.makeText(this, R.string.no_connectivity, Toast.LENGTH_LONG).show();
        }
    }

    private void state_unknown()
    {
        logo_set_color(R.color.colorUnknown);
        statusView.setText("");
    }

    public void onUnlock(View view)
    {
        api_request(ApiCommand.UNLOCK);
    }

    public void onPresent(View view)
    {
        api_request(ApiCommand.PRESENT);
    }

    public void onLock(View view)
    {
        api_request(ApiCommand.LOCK);
    }

    public void onError(String err)
    {
        play(SoundType.ERROR);
        state_unknown();
        Toast.makeText(this, err, Toast.LENGTH_SHORT).show();
    }

    private void update_status()
    {
        if (connectivity)
            api.issueCommand(ApiCommand.STATUS);
        else
            state_unknown();
    }

    public void onUpdateStatus(ApiCommand issued_command, ApiResponse resp)
    {
        update_widgets();

        if (resp.error_code == ApiResponse.ApiErrorCode.PERMISSION_DENIED ||
                resp.error_code == ApiResponse.ApiErrorCode.INVALID ||
                resp.error_code == ApiResponse.ApiErrorCode.LDAP_ERROR) {
            String msg;

            msg = resp.error_code.toString() + ": " + resp.message;
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            state_unknown();

            return;
        }

        switch (resp.status)
        {
            case Open:
                statusView.setText(R.string.open);
                logo_set_color(R.color.colorUnlocked);
                break;
            case Present:
                statusView.setText(R.string.present);
                logo_set_color(R.color.colorPresent);
                break;
            case Closed:
                statusView.setText(R.string.closed);
                logo_set_color(R.color.colorLocked);
                break;
        }

        if (issued_command != ApiCommand.STATUS) {
            if (resp.error_code == ApiResponse.ApiErrorCode.SUCCESS ||
                    resp.error_code == ApiResponse.ApiErrorCode.ALREADY_LOCKED ||
                    resp.error_code == ApiResponse.ApiErrorCode.ALREADY_OPEN)
                play(SoundType.OKAY);
            else
                play(SoundType.ERROR);

            Toast.makeText(this, resp.message, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean is_ssid_valid(String ssid)
    {
        return ssid != null
                && (ssid.equals(getResources().getString(R.string.ssid_legacy))
                || ssid.equals(getResources().getString(R.string.ssid_secure)));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId()){
            case R.id.settingsMenuSettingsItem:
                this.startActivity(new Intent(this, SettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public class WifiReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager connManager;
            WifiManager wifiManager;
            NetworkInfo mWifi;
            WifiInfo wifiInfo;
            String ssid;

            connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            wifiManager = (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);

            mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            wifiInfo = wifiManager.getConnectionInfo();
            ssid = wifiInfo.getSSID();

            connectivity = is_ssid_valid(ssid) && mWifi.isConnected();
            update_status();
        }
    }
}
