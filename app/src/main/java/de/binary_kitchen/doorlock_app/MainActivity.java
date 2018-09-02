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

import android.Manifest;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.List;
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
    private final static int POS_PERM_REQUEST = 0;

    private ScanReceiver scanReceiver;
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

        connectivity = false;
        if (prefs.getBoolean("wifiSwitchEnabled", false)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O  &&
                    ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_COARSE_LOCATION) !=
                            PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, POS_PERM_REQUEST);
            } else {
                WifiManager wifiManager;
                LocationManager lm;
                int wifi_state;

                lm =  (LocationManager)getSystemService(Context.LOCATION_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                        !lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    Toast.makeText(this, R.string.err_location_provider,
                            Toast.LENGTH_LONG).show();
                    connectivity = true;
                } else {
                    wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                    wifi_state = wifiManager.getWifiState();

                    if (wifi_state == WIFI_STATE_DISABLED || wifi_state == WIFI_STATE_DISABLING ||
                            wifi_state == WIFI_STATE_UNKNOWN) {
                        scanReceiver = new ScanReceiver();
                        IntentFilter ifilter = new IntentFilter();
                        ifilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
                        registerReceiver(scanReceiver, ifilter);
                        wifiManager.setWifiEnabled(true);
                    } else {
                        switch_wifi();
                    }
                }
            }
        } else {
            connectivity = true;
        }

        update_status();
    }

    @Override
    public void onPause()
    {
        super.onPause();

        if (scanReceiver != null) {
            unregisterReceiver(scanReceiver);
            scanReceiver = null;
        }

        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver);
            broadcastReceiver = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        final Context ctx = this;
        final SharedPreferences prefs;
        prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

        switch(requestCode) {
            case POS_PERM_REQUEST:
                if (!(grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    AlertDialog.Builder dialog;

                    prefs.edit().putBoolean("wifiSwitchEnabled", false).apply();

                    dialog = new AlertDialog.Builder(new ContextThemeWrapper(
                            this, R.style.Theme_AppCompat_Light_Dialog_Alert));
                    dialog.setMessage(R.string.dialog_wifi_access);
                    dialog.setPositiveButton("Change", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                            prefs.edit().putBoolean("wifiSwitchEnabled", true).apply();
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
                                return;
                            requestPermissions(
                                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                    POS_PERM_REQUEST);
                        }
                    });
                    dialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        }
                    });

                    dialog.show();
                }
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

        if (resp.open) {
            statusView.setText(R.string.open);
            logo_set_color(R.color.colorUnlocked);
        } else {
            statusView.setText(R.string.closed);
            logo_set_color(R.color.colorLocked);
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

    private void switch_wifi() {
        List<WifiConfiguration> configured_networks;
        List<ScanResult> scan_results;
        WifiManager wifiManager;
        boolean in_range;
        String ssid;

        if (broadcastReceiver == null) {
            broadcastReceiver = new WifiReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            registerReceiver(broadcastReceiver, intentFilter);
        }

        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        /* Are we already connected to some kitchen network? */
        ssid = wifiManager.getConnectionInfo().getSSID();
        if (is_ssid_valid(ssid)) {
            connectivity = true;
            return;
        }

        /* Let's see if any kitchen network is actually in range */
        in_range = false;
        scan_results = wifiManager.getScanResults();
        if (scan_results != null)
            for (ScanResult scan_result: scan_results)
                if (scan_result.SSID.contains(getString(R.string.ssid_top_level))) {
                    in_range = true;
                    break;
                }

        if (!in_range) {
            Toast.makeText(this, R.string.out_of_range, Toast.LENGTH_LONG).show();
            return;
        }

        configured_networks = wifiManager.getConfiguredNetworks();
        if (configured_networks == null)
            return;

        /*
         * First step: search if user has secure.binary.kitchen configured. Prefer this network over
         * others
         */
        for (WifiConfiguration networkConf: configured_networks)
            if (networkConf.SSID.equals(getString(R.string.ssid_secure))) {
                wifiManager.disconnect();
                wifiManager.enableNetwork(networkConf.networkId, true);
                wifiManager.reconnect();
                return;
            }


        /* Second step: Fall back to legacy.binary.kitchen */
        for (WifiConfiguration networkConf: configured_networks)
            if (networkConf.SSID.equals(getString(R.string.ssid_legacy))) {
                wifiManager.disconnect();
                wifiManager.enableNetwork(networkConf.networkId, true);
                wifiManager.reconnect();
                return;
            }

        Toast.makeText(this, R.string.unable_to_connect, Toast.LENGTH_LONG).show();
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

    public class ScanReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch_wifi();
            unregisterReceiver(scanReceiver);
            scanReceiver = null;
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