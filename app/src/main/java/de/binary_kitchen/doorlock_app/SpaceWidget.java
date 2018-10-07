/*
 * Doorlock, Binary Kitchen's Open Sesame
 *
 * Copyright (c) Binary Kitchen e.V., 2018
 *
 * Authors:
 *  Thomas Schmid <tom@binary-kitchen.de>
 *  Ralf Ramsauer <ralf@binary-kitchen.de>
 *
 * This work is licensed under the terms of the GNU GPL, version 3.  See
 * the COPYING file in the top-level directory.
 */

package de.binary_kitchen.doorlock_app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class SpaceWidget extends AppWidgetProvider {
    private final OkHttpClient client = new OkHttpClient();
    private static boolean open, state_valid;

    private static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                        int appWidgetId)
    {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.space_widget);
        int color = R.color.colorUnknown;

        if (state_valid)
            if (open) {
                color = R.color.colorUnlocked;
            } else {
                color = R.color.colorLocked;
            }

        views.setInt(R.id.widgetHeadImageButton,"setColorFilter",
                ContextCompat.getColor(context, color));

        Intent intentUpdate = new Intent(context, SpaceWidget.class);
        intentUpdate.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

        int[] idArray = new int[]{appWidgetId};
        intentUpdate.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, idArray);

        PendingIntent pendingUpdate = PendingIntent.getBroadcast(
                context, appWidgetId, intentUpdate,
                PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.widgetHeadImageButton, pendingUpdate);
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        SharedPreferences prefs;
        String url_spaceapi;

        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        url_spaceapi = prefs.getString("spaceapi", "");
        if (url_spaceapi.isEmpty())
            url_spaceapi = context.getResources().getString(R.string.default_spaceapi);

        SpaceAPICallback spaceAPICallback = new SpaceAPICallback(context);

        try {
            Request.Builder request_uri = new Request.Builder().url(url_spaceapi);
            Request request = request_uri.get().build();
            client.newCall(request).enqueue(spaceAPICallback);
        }
        catch (Exception e) {
            Toast.makeText(context, R.string.invalid_spaceapi_url,
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onEnabled(Context context) {
    }

    @Override
    public void onDisabled(Context context) {
    }

    private class SpaceAPICallback implements Callback
    {
        final private Context context;

        private SpaceAPICallback(Context context)
        {
            this.context = context;
        }

        private void update()
        {
            AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
            int[] ids = widgetManager.getAppWidgetIds(new ComponentName(context, SpaceWidget.class));

            for (int appWidgetId : ids) {
                updateAppWidget(context, widgetManager, appWidgetId);
            }
        }

        @Override
        public void onFailure(Call call, final IOException e)
        {
            state_valid = false;
            update();
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException
        {
            JsonParser parser = new JsonParser();
            String json_body;

            state_valid = false;
            if (response.code() == 200) {
                json_body = response.body().string();
                JsonObject obj = parser.parse(json_body).getAsJsonObject();
                open = obj.getAsJsonObject("state").get("open").getAsBoolean();
                state_valid = true;
            }

            update();
        }
    }
}
