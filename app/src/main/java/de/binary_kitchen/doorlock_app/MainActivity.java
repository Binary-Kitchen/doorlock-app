package de.binary_kitchen.doorlock_app;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
    private DoorlockApi api;
    private TextView statusView;
    private final static int POS_PERM_REQUEST = 0;
    public MainActivity(){
        api = new DoorlockApi(Configuration.getBaseUrl());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.mainToolbar);
        setSupportActionBar(toolbar);

        statusView = findViewById(R.id.statusTextView);

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

    public void lock(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        api.issueCommand(ApiCommand.LOCK,
                prefs.getString("username", ""),
                prefs.getString("password", ""),
                "kitchen");
    }

    public void unlock(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        api.issueCommand(ApiCommand.UNLOCK,
                prefs.getString("username", ""),
                prefs.getString("password", ""),
                "kitchen");
    }
    public void getStatus(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        api.issueCommand(ApiCommand.STATUS,
                prefs.getString("username", ""),
                prefs.getString("password", ""),
                "kitchen");
    }

    public class ApiCommandResponseCallback implements Callback{
        private Context context;
        public ApiCommandResponseCallback(Context context){
            this.context = context;
        }

        @Override
        public void onFailure(Call call, IOException e) {
             Log.d("RESPONSE_ERROR", e.toString());
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
                    }
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


    public void updateStatus(LockState state){
        statusView.setText(state.toString());
    }

    void checkPreconditions(){
        if(checkAndRequestSSIDAccess()){
            WifiManager wifiManager
                    = (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
            if(!checkSsid(wifiManager.getConnectionInfo().getSSID())){
                changeToSupportedWifi();
            }
        }
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