package com.sam_chordas.android.stockhawk.service;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.QuoteHistoryResult;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.utils.IntentExtras;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

import static com.sam_chordas.android.stockhawk.rest.Utils.notifyAppWidgetViewDataChanged;

/**
 * Created by sam_chordas on 9/30/15.
 * The GCMTask service is primarily for periodic tasks. However, OnRunTask can be called directly
 * and is used for the initialization and adding task as well.
 */
public class StockTaskService extends GcmTaskService {
    static final private String LOG_TAG = StockTaskService.class.getSimpleName();

    public static final String TAG = "TAG";
    public static final String INIT = "INIT";
    public static final String PERIODIC = "PERIODIC";
    public static final String ADD = "ADD";
    public static final String SYMBOL = "SYMBOL";
    public static final String HISTORICAL_DATA = "HISTORICAL_DATA";

    private static final String YAHOOAPI_YQL_URL = "https://query.yahooapis.com/v1/public/yql?q=";
    private static final String YQL_QUOTE_QUERY = "select * from yahoo.finance.quotes where symbol in (%s)";
    private static final String YQL_HISTORY_QUERY = "select * from yahoo.finance.historicaldata where symbol = \"%s\" and startDate = \"%s\" and endDate = \"%s\"";
    private static final String YQL_OPTIONS = "&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys&callback=";
    private OkHttpClient client = new OkHttpClient();
    private Context context;
    private boolean isUpdate;

    public StockTaskService() {
    }

    public StockTaskService(Context context) {
        this.context = context;
    }

    private String fetchData(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    @Override
    public int onRunTask(TaskParams params) {
        if (context == null) {
            context = this;
        }
        int result;
        if (params.getTag().equals(HISTORICAL_DATA)) {
            result = handleHistoryQuery(params);
        } else {
            result = handleQuoteQuery(params);
            notifyAppWidgetViewDataChanged(context);
        }

        return result;
    }

    private int handleHistoryQuery(TaskParams params) {
        StringBuilder urlStringBuilder = new StringBuilder(YAHOOAPI_YQL_URL);

        String stockInput = params.getExtras().getString(SYMBOL);

        String startDate = Utils.getStartDate();
        String endDate = Utils.getEndDate();

        String query = String.format(YQL_HISTORY_QUERY, stockInput, startDate, endDate);
        try {
            urlStringBuilder.append(URLEncoder.encode(query, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        urlStringBuilder.append(YQL_OPTIONS);

        String getResponse;
        int result = GcmNetworkManager.RESULT_FAILURE;
        ArrayList<QuoteHistoryResult> quotes = null;

        String urlString = urlStringBuilder.toString();
        try {
            Log.d(LOG_TAG, "Query: " + urlString);
            getResponse = fetchData(urlString);
            Log.d(LOG_TAG, "Response: " + getResponse);
            quotes = Utils.processHistoryResponse(getResponse);
            result = GcmNetworkManager.RESULT_SUCCESS;
        } catch (IOException e) {
            e.printStackTrace();
        }
        broadcastHistoryResponse(quotes, result);
        return result;
    }

    private int handleQuoteQuery(TaskParams params) {
        Cursor initQueryCursor;
        StringBuilder urlStringBuilder = new StringBuilder(YAHOOAPI_YQL_URL);

        if (params.getTag().equals(INIT) || params.getTag().equals(PERIODIC)) {
            isUpdate = true;
            initQueryCursor = context.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                    new String[]{"Distinct " + QuoteColumns.SYMBOL}, null,
                    null, null);
            if (initQueryCursor == null || initQueryCursor.getCount() == 0) {
                // Init task. Populates DB with quotes for the symbols seen below
                try {
                    String initialSymbolsQuery = String.format(YQL_QUOTE_QUERY, "\"YHOO\",\"AAPL\",\"GOOG\",\"TSLA\"");
                    urlStringBuilder.append(
                            URLEncoder.encode(initialSymbolsQuery, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            } else {
                DatabaseUtils.dumpCursor(initQueryCursor);
                initQueryCursor.moveToFirst();
                StringBuilder mStoredSymbols = new StringBuilder();
                for (int i = 0; i < initQueryCursor.getCount(); i++) {
                    mStoredSymbols.append("\"").append(initQueryCursor.getString(initQueryCursor.getColumnIndex("symbol"))).append("\",");
                    initQueryCursor.moveToNext();
                }
                mStoredSymbols.replace(mStoredSymbols.length() - 1, mStoredSymbols.length(), "");
                String query = String.format(YQL_QUOTE_QUERY, mStoredSymbols.toString());
                try {
                    urlStringBuilder.append(URLEncoder.encode(query, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        } else if (params.getTag().equals(ADD)) {
            isUpdate = false;
            // get symbol from params.getExtra and build query
            String stockInput = "\"" + params.getExtras().getString(SYMBOL) + "\"";
            String query = String.format(YQL_QUOTE_QUERY, stockInput);
            try {
                urlStringBuilder.append(URLEncoder.encode(query, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        // finalize the URL for the API query.
        urlStringBuilder.append(YQL_OPTIONS);

        String getResponse;
        int result = GcmNetworkManager.RESULT_FAILURE;
        String urlString = urlStringBuilder.toString();

        try {
            getResponse = fetchData(urlString);
            try {
                ContentValues contentValues = new ContentValues();
                // update ISCURRENT to 0 (false) so new data is current
                if (isUpdate) {
                    contentValues.put(QuoteColumns.ISCURRENT, 0);
                    context.getContentResolver().update(QuoteProvider.Quotes.CONTENT_URI, contentValues,
                            null, null);
                }
                ArrayList contentVals = Utils.quoteJsonToContentVals(getResponse);
                if (contentVals != null && !contentVals.isEmpty()) {
                    context.getContentResolver().applyBatch(QuoteProvider.AUTHORITY, contentVals);
                    result = GcmNetworkManager.RESULT_SUCCESS;
                }
            } catch (RemoteException | OperationApplicationException e) {
                Log.e(LOG_TAG, "Error applying batch insert", e);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        broadcastQueryResponse(result);
        return result;
    }

    private void broadcastQueryResponse(int result) {
        Intent intent = new Intent(IntentExtras.QUOTE_QUERY_INTENT);
        intent.putExtra(IntentExtras.QUOTE_RESULT, result);
        context.sendBroadcast(intent);
    }

    private void broadcastHistoryResponse(ArrayList<QuoteHistoryResult> quotes, int result) {
        Intent intent = new Intent(IntentExtras.QUOTE_HISTORY_INTENT);
        intent.putExtra(IntentExtras.QUOTE_RESULT, result);
        if (result == GcmNetworkManager.RESULT_SUCCESS) {
            intent.putParcelableArrayListExtra(IntentExtras.QUOTE_HISTORY_VALUES, quotes);
        }
        context.sendBroadcast(intent);
    }


}
