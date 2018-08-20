package de.binary_kitchen.doorlock_app;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
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

import java.util.List;

import de.binary_kitchen.doorlock_app.doorlock_api.ApiCommand;
import de.binary_kitchen.doorlock_app.doorlock_api.ApiErrorCode;
import de.binary_kitchen.doorlock_app.doorlock_api.DoorlockApi;
import de.binary_kitchen.doorlock_app.doorlock_api.ApiResponse;
import de.binary_kitchen.doorlock_app.doorlock_api.LockState;

public class MainActivity extends AppCompatActivity {
    private final static String doorlock_fqdn = "lock.binary.kitchen";
    private DoorlockApi api;
    private TextView statusView;
    private ImageView logo;
    private SwipeRefreshLayout swipeRefreshLayout;
    private final static int POS_PERM_REQUEST = 0;
    private SoundPool sp;

    private int s_ok, s_req, s_alert;

    public MainActivity()
    {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.mainToolbar);
        setSupportActionBar(toolbar);

        statusView = findViewById(R.id.statusTextView);
        logo = findViewById(R.id.logo);
        swipeRefreshLayout = findViewById(R.id.swiperefresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (switch_wifi())
                    api.status();
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    @Override
    protected void onResume()
    {
        String username, password;
        super.onResume();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean("soundsEnabled",true)) {
            sp = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
            s_req = sp.load(this, R.raw.voy_chime_2, 1);
            s_alert = sp.load(this, R.raw.alert20, 1);
            s_ok = sp.load(this, R.raw.input_ok_3_clean, 1);
        } else {
            if (sp != null)
                sp.release();
            sp = null;
        }

        username = prefs.getString("username", "");
        password = prefs.getString("password", "");

        api = new DoorlockApi(this, doorlock_fqdn, username, password, "kitchen");

        if (switch_wifi())
            api.status();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch(requestCode) {
            case POS_PERM_REQUEST:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    switch_wifi();
                } else {
                    has_wifi_permissions();
                }
                return;
        }
    }

    private void play(int id)
    {
        if (sp != null)
            sp.play(id, 1, 1, 0, 0, 1);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        return true;
    }

    private void state_unknown()
    {
        logo.setImageResource(R.drawable.ic_binary_kitchen_bw_border);
        statusView.setText("");
    }

    public void onUnlock(View view)
    {
        if (switch_wifi()) {
            play(s_req);
            api.unlock();
        }
    }

    public void onLock(View view)
    {
        if (switch_wifi()) {
            play(s_req);
            api.lock();
        }
    }

    public void onError(String err)
    {
        play(s_alert);
        state_unknown();
        Toast.makeText(this, err, Toast.LENGTH_SHORT).show();
    }

    public void onUpdateStatus(ApiCommand issued_command, ApiResponse resp)
    {
        LockState state;
        ApiErrorCode err;

        err = resp.getErrorCode();
        if (err == ApiErrorCode.PERMISSION_DENIED || err == ApiErrorCode.INVALID ||
                err == ApiErrorCode.LDAP_ERROR) {
            String msg;

            msg = err.toString() + ": " + resp.getMessage();
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            state_unknown();

            return;
        }

        state = resp.getStatus();
        statusView.setText(state.toString());

        if (state == LockState.CLOSED)
            logo.setImageResource(R.drawable.ic_binary_kitchen_bw_border_closed);
        else
            logo.setImageResource(R.drawable.ic_binary_kitchen_bw_border_open);

        if (issued_command != ApiCommand.STATUS) {
            if (sp != null)
                if (err == ApiErrorCode.SUCCESS || err == ApiErrorCode.ALREADY_LOCKED ||
                        err == ApiErrorCode.ALREADY_OPEN)
                    play(s_ok);
                else
                    play(s_alert);

            Toast.makeText(this, resp.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    boolean checkAndRequestLocationService()
    {
        LocationManager lm = (LocationManager)getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        boolean network_enabled = false;

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch(Exception ex) {}

        if(!network_enabled) {
            // notify user
            AlertDialog.Builder dialog = new AlertDialog.Builder((new ContextThemeWrapper(this, R.style.Theme_AppCompat_Light_Dialog_Alert)));
            dialog.setMessage("To read the ssid of wifis the app needs location information.");
            dialog.setPositiveButton("Change", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    // TODO Auto-generated method stub
                    Intent myIntent = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    getApplicationContext().startActivity(myIntent);
                    //get gps
                }
            });
            dialog.setNegativeButton("No", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    // TODO Auto-generated method stub

                }
            });
            dialog.show();
            return false;
        }
        return true;
    }

    private Boolean switch_wifi()
    {
        Boolean succ;

        succ = __switch_wifi();
        if (!succ)
            state_unknown();

        return succ;
    }

    /**
     * Checks permissions and location service status to read ssids and change wifi state.
     * If permissions are not granted, request permissions.
     */
    private Boolean __switch_wifi()
    {
        WifiManager wifiManager;
        String ssid;
        Boolean success = Boolean.FALSE;

        if (!has_wifi_permissions()) {
            Toast.makeText(this, "Insufficient permissions to change WiFi",
                    Toast.LENGTH_LONG).show();
            return success;
        }

        wifiManager = (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
        ssid = wifiManager.getConnectionInfo().getSSID();

        if (is_ssid_valid(ssid))
            return Boolean.TRUE;

        List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();
        if (configuredNetworks != null){
            for (WifiConfiguration networkConf: configuredNetworks) {
                if (is_ssid_valid(networkConf.SSID)){
                    wifiManager.disconnect();
                    /* TBD: this will return true, but we're actually not connected yet.
                       We should listen on NETWORK_STATE_CHANGE_ACTION */
                    if (wifiManager.enableNetwork(networkConf.networkId,true)) {
                        success = Boolean.TRUE;
                        break;
                    }
                }
            }
        }

        if (!success)
            Toast.makeText(this,
                    "Couldn't find valid WiFi. Maybe kitchen out of range or WiFi disabled?",
                    Toast.LENGTH_LONG).show();
        return success;
    }

    boolean is_ssid_valid(String ssid)
    {
        return ssid != null
                && (ssid.equals("\"legacy.binary-kitchen.de\"")
                || ssid.equals("\"secure.binary-kitchen.de\""));
    }

    boolean has_wifi_permissions()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //for versions greater android 8 we need coarse position permissions to get ssid
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                this.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        0);
                return false;
            }

            if (!checkAndRequestLocationService()) {
                return false;
            }
        }

        return true;
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
}
