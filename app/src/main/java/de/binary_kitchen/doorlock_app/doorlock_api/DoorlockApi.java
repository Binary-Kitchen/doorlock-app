package de.binary_kitchen.doorlock_app.doorlock_api;

import android.content.Context;
import android.net.Uri;

import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class DoorlockApi {
    private final String username, password;
    private Callback commandCallback;
    private final Request.Builder request_uri;
    private final String target;
    private final Context ctx;

    public void setCommandCallback(Callback commandCallback) {
        this.commandCallback = commandCallback;
    }

    public DoorlockApi(Context ctx, String fqdn, String username, String password,
                       String target)
    {
        Uri.Builder builder = new Uri.Builder();

        builder.scheme("https").authority(fqdn).appendPath("api");

        this.request_uri = new Request.Builder().url(builder.build().toString());
        this.username = username;
        this.password = password;
        this.target = target;
        this.ctx = ctx;
    }

    private void issueCommand(ApiCommand command){
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new FormBody.Builder()
                .add("command", command.toString())
                .add("target", target)
                .add("user", username)
                .add("pass", password)
                .build();
        Request request = request_uri.post(requestBody).build();

        client.newCall(request).enqueue(commandCallback);
    }

    public void unlock()
    {
        issueCommand(ApiCommand.UNLOCK);
    }

    public void lock()
    {
        issueCommand(ApiCommand.LOCK);
    }

    public void status()
    {
        issueCommand(ApiCommand.STATUS);
    }
}
