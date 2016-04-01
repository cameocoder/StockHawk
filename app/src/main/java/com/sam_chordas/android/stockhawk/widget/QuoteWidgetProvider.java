package com.sam_chordas.android.stockhawk.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.ui.MyStocksActivity;
import com.sam_chordas.android.stockhawk.ui.SymbolDetailActivity;
import com.sam_chordas.android.stockhawk.ui.SymbolDetailFragment;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link QuoteWidgetConfigureActivity QuoteWidgetConfigureActivity}
 */
public class QuoteWidgetProvider extends AppWidgetProvider {

    public static String ACTION_DETAIL = "com.sam_chordas.android.stockhawk.widget.ACTION_DETAIL";

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

//        CharSequence widgetText = QuoteWidgetConfigureActivity.loadTitlePref(context, appWidgetId);

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_collection);
//        views.setTextViewText(R.id.appwidget_text, widgetText);

        // Create an Intent to launch MyStocksActivity
        Intent intent = new Intent(context, MyStocksActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.widget, pendingIntent);

        // Create an Intent to launch SymbolDetailFragment
        Intent detailIntent = new Intent(context, QuoteWidgetProvider.class);
        detailIntent.setAction(ACTION_DETAIL);
        detailIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        PendingIntent toastPendingIntent = PendingIntent.getBroadcast(context, 0, detailIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        views.setPendingIntentTemplate(R.id.widget_list, toastPendingIntent);

        // Instruct the widget manager to update the widget
        views.setRemoteAdapter(R.id.widget_list, new Intent(context, QuoteWidgetRemoteViewsService.class));
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it.
//        for (int appWidgetId : appWidgetIds) {
//            QuoteWidgetConfigureActivity.deleteTitlePref(context, appWidgetId);
//        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    @Override
    public void onReceive(Context context, Intent intent) {
//        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        if (intent.getAction().equals(ACTION_DETAIL)) {
            String symbol = intent.getStringExtra(SymbolDetailFragment.ARG_SYMBOL);

            final Bundle arguments = new Bundle();
            arguments.putString(SymbolDetailFragment.ARG_SYMBOL, symbol);
            Intent detailIntent = new Intent(context, SymbolDetailActivity.class);
            detailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            detailIntent.putExtras(arguments);
            context.startActivity(detailIntent);
            Toast.makeText(context, "onReceive " + ACTION_DETAIL, Toast.LENGTH_SHORT).show();
        }
        super.onReceive(context, intent);
    }

}

