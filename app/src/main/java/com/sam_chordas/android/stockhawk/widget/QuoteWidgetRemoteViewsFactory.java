package com.sam_chordas.android.stockhawk.widget;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Binder;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.QuoteQueryResult;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.ui.SymbolDetailActivity;
import com.sam_chordas.android.stockhawk.utils.IntentExtras;

import java.util.ArrayList;
import java.util.List;

public class QuoteWidgetRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private static final String LOG_TAG = QuoteWidgetRemoteViewsFactory.class.getSimpleName();

    private final Context context;
    private List<QuoteQueryResult> stocks = new ArrayList<>();

    public QuoteWidgetRemoteViewsFactory(Context context) {
        this.context = context;
    }

    @Override
    public void onCreate() {
        getQuotes();
    }

    @Override
    public void onDataSetChanged() {
        getQuotes();
    }

    @Override
    public void onDestroy() {
        // not implemented
    }

    @Override
    public int getCount() {
        return stocks.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews view = new RemoteViews(context.getPackageName(),
                R.layout.widget_collection_item);

        QuoteQueryResult quote = stocks.get(position);
        view.setTextViewText(R.id.stock_symbol, quote.getSymbol());
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (prefs.getBoolean(Utils.PREF_SHOW_PERCENT, true)) {
            view.setTextViewText(R.id.change, quote.getPercentChange());
        } else {
            view.setTextViewText(R.id.change, quote.getChange());
        }

        if (quote.getIsUp() > 0) {
            view.setInt(R.id.change, "setBackgroundColor", ContextCompat.getColor(context, R.color.material_green_700));
        } else {
            view.setInt(R.id.change, "setBackgroundColor", ContextCompat.getColor(context, R.color.material_red_700));
        }

        final Bundle arguments = new Bundle();
        arguments.putString(IntentExtras.ARG_SYMBOL, quote.getSymbol());
        Intent intent = new Intent(context, SymbolDetailActivity.class);
        intent.putExtras(arguments);

        view.setOnClickFillInIntent(R.id.widget_list_item, intent);

        return view;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    private void getQuotes() {
        stocks.clear();

        final long token = Binder.clearCallingIdentity();
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(
                    QuoteProvider.Quotes.CONTENT_URI,
                    new String[]{QuoteColumns.SYMBOL, QuoteColumns.CHANGE,
                            QuoteColumns.PERCENT_CHANGE, QuoteColumns.ISUP,
                            QuoteColumns._ID},
                    QuoteColumns.ISCURRENT + " = ?",
                    new String[]{"1"},
                    QuoteColumns.SYMBOL + " ASC, " + QuoteColumns._ID + " DESC"
            );


            if (cursor != null && cursor.getCount() != 0) {
                Log.d(LOG_TAG, "Symbol count: " + cursor.getCount());
                while (cursor.moveToNext()) {
                    String symbol = cursor.getString(cursor.getColumnIndex(QuoteColumns.SYMBOL));
                    String change = cursor.getString(cursor.getColumnIndex(QuoteColumns.CHANGE));
                    String percentChange = cursor.getString(cursor.getColumnIndex(QuoteColumns.PERCENT_CHANGE));
                    int isup = cursor.getInt(cursor.getColumnIndex(QuoteColumns.ISUP));

                    if (!symbol.isEmpty()) {
                        QuoteQueryResult quote = new QuoteQueryResult();
                        quote.setSymbol(symbol);
                        quote.setChange(change);
                        quote.setPercentChange(percentChange);
                        quote.setIsUp(isup);
                        stocks.add(quote);
//                        Log.d(LOG_TAG, "Added symbol: " + symbol + " change: " + change);
                    }
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            Binder.restoreCallingIdentity(token);
        }
    }

}
