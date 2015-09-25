package com.hh.bbmeitu;

import android.app.IntentService;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.widget.RemoteViews;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.hh.bwall.R;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;

import timber.log.Timber;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class UpdateIntentService extends IntentService {
    private static final String PREFS_NAME = "com.hh.bwall.bingjs";
    private static final String PREF_PREFIX_SEARCH_KEY = "search_url";
    private static final String ACTION_UPDATE = "com.hh.bwall.action.UPDATE";
    private static final String EXTRA_PARAM_ID = "com.hh.bwall.extra.ID";
    private static final String BING_URL = "https://www.bing.com/HPImageArchive.aspx";
    private static final String BING_WALLPAPER_ROOT_URL = "https://www.bing.com";
    private RequestQueue mQueue;
    public static void startActionUpdate(Context context, int id) {
        Intent intent = new Intent(context, UpdateIntentService.class);
        intent.setAction(ACTION_UPDATE);
        intent.putExtra(EXTRA_PARAM_ID, id);
        context.startService(intent);
    }




    public UpdateIntentService() {
        super("UpdateIntentService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (mQueue == null){
            mQueue = Volley.newRequestQueue(this);
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_UPDATE.equals(action)) {
                final int id = intent.getIntExtra(EXTRA_PARAM_ID, -1);
                handleActionFoo(id);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void handleActionFoo(final int id) {
        if (id < 0) return;
        String mkt = BbmeituAppWidgetConfigureActivity.loadMktPref(this, id);

        Uri.Builder builder = Uri.parse(BING_URL).buildUpon();
        builder.appendQueryParameter("format", "js")
                .appendQueryParameter("idx", "0")
                .appendQueryParameter("n", "1")
                .appendQueryParameter("mkt", mkt);
        RequestFuture<JSONObject> future = RequestFuture.newFuture();
        JsonObjectRequest request = new JsonObjectRequest(builder.build().toString(), null, future, future);

        long start = System.currentTimeMillis();
        mQueue.add(request);

        try {
            JSONObject response = future.get(); // this will block (forever)

            Timber.d(response.toString());

            try {
                String text = response.getJSONArray("images").getJSONObject(0).getString("copyright");
                String url = response.getJSONArray("images").getJSONObject(0).getString("url");
                String search_url = response.getJSONArray("images").getJSONObject(0).getString("copyrightlink");

                saveSearchUrlPref(getApplicationContext(), id, search_url);

                Bitmap bitmap = Glide.with(getApplicationContext())
                        .load(BING_WALLPAPER_ROOT_URL.concat(url))
                        .asBitmap()
                        .centerCrop()
                        .into(SimpleTarget.SIZE_ORIGINAL, SimpleTarget.SIZE_ORIGINAL)
                        .get();

                WallpaperManager wallpaperManager
                        = WallpaperManager.getInstance(getApplicationContext());
                Timber.i("width="  + bitmap.getWidth());
                Timber.i("height=" + bitmap.getHeight());
                try {
                    wallpaperManager.setBitmap(bitmap);
                    File file =  new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "wall.jpg");
                    OutputStream outputStream =  new FileOutputStream(file.getAbsoluteFile());
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                    outputStream.close();

                    // Construct the RemoteViews object
                    RemoteViews views = new RemoteViews(getPackageName(), R.layout.bbmeitu_app_widget);
                    views.setTextViewText(R.id.tv_title, text);
                    Timber.i("file " + file.getAbsolutePath() + " exist " + file.exists());
                    Uri uri =  Uri.fromFile(file);
                    Timber.i("uri " + uri);
                    views.setImageViewUri(R.id.iv_wallpaper, uri);

                    // Instruct the widget manager to update the widget
                    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(UpdateIntentService.this);
                    appWidgetManager.updateAppWidget(id, views);


                } catch (IOException e) {
                    e.printStackTrace();
                }





            } catch (Exception e){
                Timber.e(e, "error save");
            }
        } catch (InterruptedException e) {
            Timber.e(e, "InterruptedException");
        } catch (ExecutionException e) {
            Timber.e(e, "ExecutionException");
        }

        long time = System.currentTimeMillis() - start;
        Timber.i("cost time " + time);

    }


    public static void saveSearchUrlPref(Context context, int appWidgetId, String text) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_PREFIX_SEARCH_KEY + appWidgetId, text);
        prefs.commit();
    }

    public static String loadSearchUrlPref(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        String titleValue = prefs.getString(PREF_PREFIX_SEARCH_KEY + appWidgetId, null);
        if (titleValue != null) {
            return titleValue;
        } else {
            return context.getString(R.string.unknown);
        }
    }

    public static void deleteSearchUrlPref(Context context, int appWidgetId) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.remove(PREF_PREFIX_SEARCH_KEY + appWidgetId);
        prefs.commit();
    }
}
