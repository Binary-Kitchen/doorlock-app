package de.binary_kitchen.doorlock_app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
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

import de.binary_kitchen.doorlock_app.doorlock_api.ApiCommand;
import de.binary_kitchen.doorlock_app.doorlock_api.ApiErrorCode;
import de.binary_kitchen.doorlock_app.doorlock_api.DoorlockApi;
import de.binary_kitchen.doorlock_app.doorlock_api.ApiResponse;
import de.binary_kitchen.doorlock_app.doorlock_api.LockState;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private DoorlockApi api;
    private TextView statusView;

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
        getStatus();
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

        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            Log.d("RESPONSE",response.toString());
            Handler mainhandler = new Handler(context.getMainLooper());
            if(response.code() == 200){
                final ApiResponse resp = new Gson().fromJson(response.body().string(),ApiResponse.class);
                Log.d("RESPONSE",resp.toString());
                if(resp.getErrorCode() == ApiErrorCode.SUCCESS){
                    mainhandler.post(new Runnable() {
                        @Override
                        public void run() {
                            updateStatus(resp.getStatus());
                        }
                    });
                }



                Runnable toast = new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, resp.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                };

                mainhandler.post(toast);
            }
        }
    }


    public void updateStatus(LockState state){
        statusView.setText(state.toString());
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