package com.sam_chordas.android.stockhawk.rest;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.widget.QuoteWidgetProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by sam_chordas on 10/8/15.
 */
public class Utils {

    public static final String PREF_LAST_UPDATED_TIME = "PREF_LAST_UPDATED_TIME";
    public static final String PREF_SHOW_PERCENT = "PREF_SHOW_PERCENT";
    private static final String LOG_TAG = Utils.class.getSimpleName();

    private static final String JSON_NULL = "null";
    private static final String JSON_QUERY = "query";
    private static final String JSON_COUNT = "count";
    private static final String JSON_RESULTS = "results";
    private static final String JSON_QUOTE = "quote";

    public static ArrayList quoteJsonToContentVals(String JSON) {
        ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
        JSONObject jsonObject;
        JSONArray resultsArray;
        Log.i(LOG_TAG, "GET FB: " + JSON);
        try {
            jsonObject = new JSONObject(JSON);
            if (jsonObject.length() != 0) {

                jsonObject = jsonObject.getJSONObject(JSON_QUERY);
                int count = Integer.parseInt(jsonObject.getString(JSON_COUNT));
                if (count == 1) {
                    jsonObject = jsonObject.getJSONObject(JSON_RESULTS)
                            .getJSONObject(JSON_QUOTE);
                    batchOperations.add(buildBatchOperation(jsonObject));
                } else {
                    resultsArray = jsonObject.getJSONObject(JSON_RESULTS).getJSONArray(JSON_QUOTE);

                    if (resultsArray != null && resultsArray.length() != 0) {
                        for (int i = 0; i < resultsArray.length(); i++) {
                            jsonObject = resultsArray.getJSONObject(i);
                            batchOperations.add(buildBatchOperation(jsonObject));
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "String to JSON failed: " + e);
        }
        return batchOperations;
    }

    public static String truncateBidPrice(String bidPrice) {
        if (!TextUtils.isEmpty(bidPrice) && !JSON_NULL.equals(bidPrice)) {
            bidPrice = String.format("%.2f", Float.parseFloat(bidPrice));
        }
        return bidPrice;
    }

    private static String truncateChange(String change, boolean isPercentChange) {
        if (TextUtils.isEmpty(change) || JSON_NULL.equals(change)) {
            return change;
        }
        String weight = change.substring(0, 1);
        String ampersand = "";
        if (isPercentChange) {
            ampersand = change.substring(change.length() - 1, change.length());
            change = change.substring(0, change.length() - 1);
        }
        change = change.substring(1, change.length());
        double round = (double) Math.round(Double.parseDouble(change) * 100) / 100;
        change = String.format("%.2f", round);
        StringBuilder changeBuffer = new StringBuilder(change);
        changeBuffer.insert(0, weight);
        changeBuffer.append(ampersand);
        change = changeBuffer.toString();
        return change;
    }

    private static ContentProviderOperation buildBatchOperation(JSONObject jsonObject) {
        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
                QuoteProvider.Quotes.CONTENT_URI);
        try {
            String change = jsonObject.getString("Change");
            builder.withValue(QuoteColumns.SYMBOL, jsonObject.getString("symbol"));
            builder.withValue(QuoteColumns.BIDPRICE, truncateBidPrice(jsonObject.getString("Bid")));
            builder.withValue(QuoteColumns.PERCENT_CHANGE, truncateChange(
                    jsonObject.getString("ChangeinPercent"), true));
            builder.withValue(QuoteColumns.CHANGE, truncateChange(change, false));
            builder.withValue(QuoteColumns.ISCURRENT, 1);
            if (change.charAt(0) == '-') {
                builder.withValue(QuoteColumns.ISUP, 0);
            } else {
                builder.withValue(QuoteColumns.ISUP, 1);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return builder.build();
    }

    public static void notifyAppWidgetViewDataChanged(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int appWidgetIds[] = appWidgetManager.getAppWidgetIds(
                new ComponentName(context, QuoteWidgetProvider.class));
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list);
    }
}
