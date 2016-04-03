package kitchen.binary.kitchendoorlock;

import android.os.Build;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;


public class About extends ActionBarActivity {

    TextView versionText;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);

        versionText = (TextView) findViewById(R.id.versionText);
        versionText.setText("Version: " + BuildConfig.VERSION_NAME);
    }

    public void onBack(View view)
    {
        finish();
    }
}
