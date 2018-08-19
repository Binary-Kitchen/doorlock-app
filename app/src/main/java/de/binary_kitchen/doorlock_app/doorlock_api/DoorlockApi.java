package de.binary_kitchen.doorlock_app.doorlock_api;

import android.content.Context;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class DoorlockApi {
    private final String username, password;
    private Callback commandCallback;
    private final String baseUrl;
    private final String target;
    private final Context ctx;

    public void setCommandCallback(Callback commandCallback) {
        this.commandCallback = commandCallback;
    }

    public DoorlockApi(Context ctx, String baseUrl, String username, String password,
                       String target) {
        this.baseUrl = baseUrl;
        this.username = username;
        this.password = password;
        this.target = target;
        this.ctx = ctx;
    }

    public void issueCommand(ApiCommand command){
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new FormBody.Builder()
                .add("command", command.toString())
                .add("target", target)
                .add("user", username)
                .add("pass", password)
                .build();
        Request request = new Request.Builder().url(baseUrl + "api").post(requestBody).build();

        client.newCall(request).enqueue(commandCallback);
    }
}
