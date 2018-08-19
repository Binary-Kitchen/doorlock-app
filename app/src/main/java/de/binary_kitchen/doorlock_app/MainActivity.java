package de.binary_kitchen.doorlock_app;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.List;

import de.binary_kitchen.doorlock_app.doorlock_api.ApiCommand;
import de.binary_kitchen.doorlock_app.doorlock_api.ApiErrorCode;
import de.binary_kitchen.doorlock_app.doorlock_api.DoorlockApi;
import de.binary_kitchen.doorlock_app.doorlock_api.ApiResponse;
import de.binary_kitchen.doorlock_app.doorlock_api.LockState;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private final static String base_url = "https://lock.binary.kitchen/";
    private DoorlockApi api;
    private TextView statusView;
    private ImageView logo;
    private SwipeRefreshLayout swipeRefreshLayout;
    private final static int POS_PERM_REQUEST = 0;

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
                checkPreconditions();
                getStatus();
            }
        });
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String username = prefs.getString("username", "");
        String password = prefs.getString("password", "");
        api = new DoorlockApi(base_url, username, password);

        api.setCommandCallback(new ApiCommandResponseCallback(getApplicationContext()));

        checkPreconditions();
        getStatus();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch(requestCode) {
            case POS_PERM_REQUEST:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkPreconditions();
                } else {
                    checkAndRequestSSIDAccess();
                }
                return;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        return true;
    }

    public void onUnlock(View view){
        unlock();
    }

    public void onLock(View view){
        lock();
    }

    public void lock()
    {
        api.issueCommand(ApiCommand.LOCK, "kitchen");
    }

    public void unlock()
    {
        api.issueCommand(ApiCommand.UNLOCK, "kitchen");
    }

    public void getStatus()
    {
        api.issueCommand(ApiCommand.STATUS, "kitchen");
    }

    public class ApiCommandResponseCallback implements Callback{
        private Context context;
        public ApiCommandResponseCallback(Context context){
            this.context = context;
        }

        @Override
        public void onFailure(Call call, IOException e) {
             Log.d("RESPONSE_ERROR", e.toString());
             if(swipeRefreshLayout.isRefreshing()){
                 swipeRefreshLayout.setRefreshing(false);
             }
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            Log.d("RESPONSE",response.toString());
            Handler mainHandler = new Handler(context.getMainLooper());
            FormBody requestBody = (FormBody)call.request().body();
            ApiCommand issuedCommand = ApiCommand.fromString(requestBody.value(0));

            Log.d("ISSUEDCOMMAND",issuedCommand.toString());

            if(response.code() == 200){
                final ApiResponse resp = new Gson().fromJson(response.body().string(),ApiResponse.class);
                if(resp.getErrorCode() == ApiErrorCode.SUCCESS){
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            updateStatus(resp.getStatus());
                            if(swipeRefreshLayout.isRefreshing())
                                swipeRefreshLayout.setRefreshing(false);
                        }
                    });
                }

                if(PreferenceManager.getDefaultSharedPreferences(context).getBoolean("soundsEnabled",true)){
                    if(issuedCommand == ApiCommand.LOCK || issuedCommand == ApiCommand.UNLOCK){
                        if(resp.getErrorCode() == ApiErrorCode.SUCCESS ||
                                resp.getErrorCode() == ApiErrorCode.ALREADY_LOCKED ||
                                resp.getErrorCode() == ApiErrorCode.ALREADY_OPEN){
                            MediaPlayer.create(context,R.raw.input_ok_3_clean).start();
                        }else{
                            MediaPlayer.create(context,R.raw.voy_chime_2).start();
                        }
                        Runnable toast = new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, resp.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        };
                        mainHandler.post(toast);
                    }
                }
            }
        }
    }


    public void updateStatus(LockState state){
        statusView.setText(state.toString());
        if(state == LockState.CLOSED){
            logo.setImageResource(R.drawable.ic_binary_kitchen_bw_border_closed);
        }else{
            logo.setImageResource(R.drawable.ic_binary_kitchen_bw_border_open);
        }
    }

    boolean checkAndRequestLocationService(){
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

    /**
     * Checks permissions and location service status to read ssids and change wifi state.
     * If permissions are not granted, request permissions.
     */
    void checkPreconditions(){
        WifiManager wifiManager = (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
        String ssid = wifiManager.getConnectionInfo().getSSID();
        if(checkAndRequestSSIDAccess()){
            if(!checkSsid(wifiManager.getConnectionInfo().getSSID())){
                changeToSupportedWifi();
            }
        } else{
            onWifiChangeFail();
        }
    }

    void onWifiChangeFail(){
        Toast.makeText(this,
                "Unable to change wifi. Make sure you are on the correct network",Toast.LENGTH_LONG).show();
    }

    boolean checkSsid(String ssid){
        return ssid != null
                && (ssid.equals("\"legacy.binary-kitchen.de\"")
                || ssid.equals("\"secure.binary-kitchen.de\"")
                || isEmulator());
    }

    public static boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT);
    }

    boolean checkAndRequestSSIDAccess(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //for versions greater android 8 we need coarse position permissions to get ssid
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                this.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        0);
                return false;
            }
            if(!checkAndRequestLocationService()){
                return false;
            }
        }
        return true;
    }

    boolean changeToSupportedWifi(){
        WifiManager wifiManager =
                (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
        List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();
        if(configuredNetworks != null){
            for(WifiConfiguration networkConf: configuredNetworks){
                Log.d("SSIDCHECK",networkConf.SSID);
                if(checkSsid(networkConf.SSID)){
                    wifiManager.disconnect();
                    return wifiManager.enableNetwork(networkConf.networkId,true);
                }
            }
        }
        onWifiChangeFail();
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.settingsMenuSettingsItem:
                this.startActivity(new Intent(this, SettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
