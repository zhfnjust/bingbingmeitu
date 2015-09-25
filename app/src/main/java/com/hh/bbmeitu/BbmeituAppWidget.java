package com.hh.bbmeitu;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.RemoteViews;

import com.hh.bwall.R;

import timber.log.Timber;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link BbmeituAppWidgetConfigureActivity BwallAppWidgetConfigureActivity}
 */
public class BbmeituAppWidget extends AppWidgetProvider {
    public static final String ACTION_TITLE_CLICK = "com.hh.bwall.action.TITLE_CLICK";
    public static final String ACTION_SEARCH_CLICK = "com.hh.bwall.action.SEARCH_CLICK";
    public static final String EXTRA_ID = "com.hh.bwall.extra.id";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            updateAppWidget(context, appWidgetManager, appWidgetIds[i]);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it.
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            BbmeituAppWidgetConfigureActivity.deleteTitlePref(context, appWidgetIds[i]);
            BbmeituAppWidgetConfigureActivity.deleteMktPref(context, appWidgetIds[i]);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

        static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

            UpdateIntentService.startActionUpdate(context, appWidgetId);
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.bbmeitu_app_widget);

            {
                Intent intent = new Intent(ACTION_TITLE_CLICK);
                intent.putExtra(EXTRA_ID, appWidgetId);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                views.setOnClickPendingIntent(R.id.tv_title, pendingIntent);
            }

            {
                Intent intent = new Intent(ACTION_SEARCH_CLICK);
                intent.putExtra(EXTRA_ID, appWidgetId);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                views.setOnClickPendingIntent(R.id.btnSearch, pendingIntent);
            }

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        Timber.i("action=" + action);
        if (ACTION_TITLE_CLICK.equals(action)){
            int appWidgetId = intent.getIntExtra(EXTRA_ID, -1);
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.bbmeitu_app_widget);
            views.setTextViewText(R.id.tv_title, context.getString(R.string.loading));
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            appWidgetManager.updateAppWidget(appWidgetId, views);
            UpdateIntentService.startActionUpdate(context, appWidgetId);
        } else if (ACTION_SEARCH_CLICK.equals(action)){
            int appWidgetId = intent.getIntExtra(EXTRA_ID, -1);
            String url = UpdateIntentService.loadSearchUrlPref(context, appWidgetId);
            if (!TextUtils.isEmpty(url)){
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.setData(Uri.parse(url));
                context.startActivity(i);
            }
        }
    }
}

