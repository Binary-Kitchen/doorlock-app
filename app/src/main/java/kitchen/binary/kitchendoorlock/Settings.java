package kitchen.binary.kitchendoorlock;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;


public class Settings extends ActionBarActivity
{

    EditText userText;
    EditText passwordText;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        userText = (EditText)findViewById(R.id.userText);
        passwordText = (EditText)findViewById(R.id.passwordText);

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preferences), MODE_PRIVATE);
        if (sharedPref.contains(getString(R.string.username))) {
            String username = sharedPref.getString(getString(R.string.username), "");
            userText.setText(username);
        }
        if (sharedPref.contains(getString(R.string.password))) {
            String password = sharedPref.getString(getString(R.string.password), "");
            passwordText.setText(password);
        }
    }

    public void onSave(View view)
    {
        String username = userText.getText().toString();
        String password = passwordText.getText().toString();

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preferences), MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getString(R.string.username), username);
        editor.putString(getString(R.string.password), password);
        editor.commit();

        Context context = getApplicationContext();
        CharSequence text = "Saved!";
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();

        Intent returnIntent = new Intent();
        returnIntent.putExtra(getString(R.string.username), username);
        returnIntent.putExtra(getString(R.string.password), password);
        setResult(RESULT_OK, returnIntent);
        finish();
    }

    public void onBack(View view)
    {
        Intent returnIntent = new Intent();
        setResult(RESULT_CANCELED, returnIntent);
        finish();
    }
}
