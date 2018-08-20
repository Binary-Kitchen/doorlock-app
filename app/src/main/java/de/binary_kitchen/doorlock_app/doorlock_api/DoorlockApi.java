package de.binary_kitchen.doorlock_app.doorlock_api;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;

import com.google.gson.Gson;

import java.io.IOException;

import de.binary_kitchen.doorlock_app.MainActivity;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DoorlockApi {
    private final Request.Builder request_uri;
    private final String username, password;
    private Callback commandCallback;
    private OkHttpClient client;
    private final String target;

    public DoorlockApi(Context ctx, String fqdn, String username, String password,
                       String target)
    {
        Uri.Builder builder = new Uri.Builder();

        builder.scheme("https").authority(fqdn).appendPath("api");

        this.request_uri = new Request.Builder().url(builder.build().toString());
        this.username = username;
        this.password = password;
        this.target = target;
        this.client = new OkHttpClient();
        this.commandCallback = new ApiCommandResponseCallback(ctx);

    }

    private void issueCommand(ApiCommand command){
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

    public class ApiCommandResponseCallback implements Callback
    {
        private Context context;
        public ApiCommandResponseCallback(Context context)
        {
            this.context = context;
        }

        @Override
        public void onFailure(Call call, final IOException e)
        {
            Handler handler;

            handler = new Handler(context.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    MainActivity act = (MainActivity) context;
                    act.onError(e.toString());
                }
            });
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException
        {
            final ApiCommand issuedCommand;
            final ApiResponse resp;
            FormBody requestBody;
            Handler handler;
            String json_body;

            if (response.code() != 200) {
                /* TBD: Low level HTTP error */
                return;
            }

            handler = new Handler(context.getMainLooper());
            requestBody = (FormBody)call.request().body();
            issuedCommand =  ApiCommand.fromString(requestBody.value(0));
            json_body = response.body().string();
            resp = new Gson().fromJson(json_body, ApiResponse.class);

            handler.post(new Runnable() {
                @Override
                public void run() {
                    MainActivity act = (MainActivity) context;
                    act.onUpdateStatus(issuedCommand, resp);
                }
            });
        }
    }
}
