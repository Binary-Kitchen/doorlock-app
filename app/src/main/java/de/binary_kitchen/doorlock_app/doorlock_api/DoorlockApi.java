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

    private Request buildCommandRequest(ApiCommand command, String user, String password, String target){
        RequestBody requestBody = new FormBody.Builder()
                .add("command", command.toString())
                .add("target",target)
                .add("user",user)
                .add("pass",password)
                .build();
        return new Request.Builder()
                .url(baseUrl+"api")
                .post(requestBody)
                .build();
    }

    public void issueCommand(ApiCommand command){
        OkHttpClient client = new OkHttpClient();
        client.newCall(buildCommandRequest(command, username, password, target))
                .enqueue(commandCallback);
    }

    private static OkHttpClient getUnsafeOkHttpClient() {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain,
                                                       String authType) throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain,
                                                       String authType) throws CertificateException {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    }
            };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier(new HostnameVerifier() {
                        @Override
                        public boolean verify(String hostname, SSLSession session) {
                            return true;
                        }
                    }).build();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
