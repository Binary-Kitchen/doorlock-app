package kitchen.binary.kitchendoorlock;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class Start extends ActionBarActivity {

    String username, password;
    String token;

    boolean tokenValid = false;
    boolean scanFinished = false;
    boolean isConfigured = false;

    final int qrRequestCode = 0;
    final int settingsRequestCode = 1;

    final String webServer = "https://lock.binary.kitchen/";

    final String actionLock = "lock";
    final String actionUnlock = "unlock";

    final String postArgCommand = "command";
    final String postArgToken = "token";
    final String postArgUser = "user";
    final String postArgPassword = "pass";
    final String postArgApi = "api";

    TextView statusText;
    Button scanButton;

    @Override
    protected void onResume()
    {
        super.onResume();

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preferences), MODE_PRIVATE);

        if (sharedPref.contains(getString(R.string.username)) == false ||
                sharedPref.contains(getString(R.string.password)) == false)
        {
            Intent intent = new Intent(this, Settings.class);
            startActivityForResult(intent, settingsRequestCode);
        } else {
            username = sharedPref.getString(getString(R.string.username), "");
            password = sharedPref.getString(getString(R.string.password), "");
            isConfigured = true;

            if (checkState() && scanFinished == false) {
                scanButton.performClick();
            } else {
                scanFinished = false;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start);

        statusText = (TextView) findViewById(R.id.statusText);
        scanButton = (Button) findViewById(R.id.scanButton);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case settingsRequestCode:
                if (resultCode == RESULT_OK)
                {
                    SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preferences), MODE_PRIVATE);
                    username = sharedPref.getString(getString(R.string.username), "");
                    password = sharedPref.getString(getString(R.string.password), "");
                }

                if (username == "" || password == "") {
                    statusText.setText(R.string.setup_credentials);
                } else {
                    isConfigured = true;
                }
                break;

            case qrRequestCode:
                scanFinished = true;

                if (resultCode == RESULT_OK)
                {
                    String contents = data.getStringExtra("SCAN_RESULT");

                    if (contents.length() == webServer.length() + 16
                            && contents.substring(0, webServer.length()).equals(webServer))
                    {
                        token = contents.substring(webServer.length());

                        if (token.length() != 16 || !token.matches("-?[0-9a-fA-F]+"))
                        {
                            statusText.setText("Don't cheat on me...");
                        } else {
                            tokenValid = true;
                            statusText.setText("Found Token: " + token);
                        }
                    } else {
                        statusText.setText("WTF did you just scan?");
                    }
                } else {
                    tokenValid = false;
                    statusText.setText(R.string.token_failed);
                }
                break;

            default:
                break;
        }
        ;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        Intent intent = null;

        switch (id)
        {
            case R.id.action_settings:
                intent = new Intent(this, Settings.class);
                startActivityForResult(intent, settingsRequestCode);
                return true;

            case R.id.action_about:
                intent = new Intent(this, About.class);
                startActivity(intent);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onLock(View view)
    {
        if (checkState() == false)
        {
            return;
        } else if (tokenValid == false) {
            statusText.setText(R.string.scan_token);
            return;
        }

        MediaPlayer mPlayer = MediaPlayer.create(Start.this, R.raw.voy_chime_2);
        mPlayer.start();

        statusText.setText(R.string.try_lock);
        new PerformActionTask().execute(actionLock);
    }

    public void onUnlock(View view)
    {
        if (checkState() == false)
        {
            return;
        } else if (tokenValid == false) {
            statusText.setText(R.string.scan_token);
            return;
        }

        MediaPlayer mPlayer = MediaPlayer.create(Start.this, R.raw.voy_chime_2);
        mPlayer.start();

        statusText.setText(R.string.try_unlock);
        new PerformActionTask().execute(actionUnlock);
    }

    enum SSID_TYPE { NONE, LEGACY, SECURE };

    SSID_TYPE checkSSID(String ssid) {
        if (ssid == null) {
            return SSID_TYPE.NONE;
        } else if (ssid.eqauls("\"legacy.binary-kitchen.de\"")) {
            return SSID_TYPE.LEGACY;
        } else if (ssid.equals("\"secure.binary-kitchen.de\"")) {
            return SSID_TYPE.SECURE;
        } else {
            return SSID_TYPE.NONE;
        }
    }

    boolean checkState()
    {
        if (!isConfigured)
        {
            statusText.setText(R.string.setup_credentials);
            return false;
        }

        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);

        SSID_TYPE currentType = checkSSID(wifiManager.getConnectionInfo().getSSID());

        if (currentType == SSID_TYPE.SECURE)
        {
            // Already connected to secure.
            return true;
        } else {
            int foundConfigId;
            SSID_TYPE foundType = SSID_TYPE.NONE;
            List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
            for (WifiConfiguration wifiConfig : list) {
                SSID_TYPE type = checkSSID(wifiConfig.SSID);
                if (type > foundType) {
                    foundType = type;
                    foundConfigId = wifiConfig.networkId;
                }
            }
            if (foundType >= SSID_TYPE.LEGACY && foundType > currentType) {
                // We found a network
                if (wifiManager.enableNetwork(foundConfigId, true)) {
		    // Connection successful
                    return true;
                } else {
		    // Error connecting
                    statusText.setText(R.string.wifi_error);
                }
            } else {
		// Couldn't find doorlock-enabled WiFi
                statusText.setText(R.string.wrong_wifi);
                return false;
            }
        }
    }

    public void onScan(View view)
    {
        if (checkState() == false)
        {
            return;
        }

        try
        {
            scanFinished = false;
            Intent intent = new Intent("com.google.zxing.client.android.SCAN");
            intent.putExtra("SCAN_MODE", "QR_CODE_MODE");

            startActivityForResult(intent, qrRequestCode);

        }
        catch (Exception e)
        {
            Uri marketUri = Uri.parse("market://details?id=com.google.zxing.client.android");
            Intent marketIntent = new Intent(Intent.ACTION_VIEW,marketUri);
            startActivity(marketIntent);
            scanFinished = true;
        }
    }

    private class PerformActionTask extends AsyncTask<String, Void, Integer> {
        @Override
        protected Integer doInBackground(String... urls) {
            String action = urls[0];
            HttpURLConnection connection = null;

            try
            {
                URL url = new URL(webServer);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");

                String parameters = postArgCommand + "=" + URLEncoder.encode(action, "UTF-8") + "&"
                        + postArgUser + "=" + URLEncoder.encode(username, "UTF-8") + "&"
                        + postArgPassword + "=" + URLEncoder.encode(password, "UTF-8") + "&"
                        + postArgToken + "=" + URLEncoder.encode(token, "UTF-8") + "&"
                        + postArgApi + "=" + "true";

                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                connection.setRequestProperty("Content-Length", "" +
                        Integer.toString(parameters.getBytes().length));
                connection.setRequestProperty("Content-Language", "en-US");

                connection.setUseCaches (false);
                connection.setDoInput(true);
                connection.setDoOutput(true);

                DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
                wr.writeBytes(parameters);
                wr.flush();
                wr.close();

                //Get Response
                InputStream is = connection.getInputStream();
                BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                String line;
                StringBuffer response = new StringBuffer();
                while ((line = rd.readLine()) != null)
                {
                    response.append(line);
                    response.append('\r');
                }
                rd.close();

                return Integer.parseInt(response.toString().replaceAll("\\r|\\n", ""));
            }
            catch (Exception e)
            {
                return 1;
            }
            finally
            {
                if (connection != null)
                {
                    connection.disconnect();
                }
            }
        }

        @Override
        protected void onPostExecute(Integer result)
        {
            String str = err2str(result);
            statusText.setText(str);

            MediaPlayer mPlayer = null;
            switch (result)
            {
                case 0:
                case 2:
                    mPlayer = MediaPlayer.create(Start.this, R.raw.input_ok_3_clean);
                    break;
                default:
                    mPlayer = MediaPlayer.create(Start.this, R.raw.alert20);
                    break;

            }
            mPlayer.start();
        }

        String err2str(int code)
        {
            switch (code)
            {
                case 0:
                    return "Success";
                case 1:
                    return "Fail";
                case 2:
                    return "Already Unlocked"; // Authentication successful, but door is already unlocked
                case 3:
                    return "Already Locked"; // Authentication successful, but door is already locked
                case 4:
                    return "NotJson"; // Request is not a valid JSON object
                case 5:
                    return "Json Error"; // Request is valid JSON, but does not contain necessary material
                case 6:
                    return "Invalid Token"; // Request contains invalid token
                case 7:
                    return "Invalid Credentials"; // Invalid LDAP credentials
                case 8:
                    return "Invalid IP";
                case 9:
                    return "Unknown Action"; // Unknown action
                case 10:
                    return "LDAP Init error"; // Ldap initialization failed
                default:
                    return "Unknown error";
            }
        }
    }
}
