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
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

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
    private Context mContext;
    private boolean isUpdate;

    public StockTaskService() {
    }

    public StockTaskService(Context context) {
        mContext = context;
    }

    String fetchData(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    @Override
    public int onRunTask(TaskParams params) {
        if (mContext == null) {
            mContext = this;
        }
        int result;
        if (params.getTag().equals(HISTORICAL_DATA)) {
            result = handleHistoryQuery(params);
        } else {
            result = handleQuoteQuery(params);
        }

        return result;
    }

    private int handleHistoryQuery(TaskParams params) {
        StringBuilder urlStringBuilder = new StringBuilder(YAHOOAPI_YQL_URL);

        String stockInput = params.getExtras().getString(SYMBOL);

        String startDate = getStartDate();
        String endDate = getEndDate();

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
            Log.d(LOG_TAG,"Query: " + urlString);
            getResponse = fetchData(urlString);
            Log.d(LOG_TAG,"Response: " + getResponse);
            quotes = processHistoryResponse(getResponse);
            result = GcmNetworkManager.RESULT_SUCCESS;
        } catch (IOException e) {
            e.printStackTrace();
        }
        broadcastHistoryResponse(quotes, result);
        return result;
    }

    private ArrayList<QuoteHistoryResult> processHistoryResponse(String response) {
        ArrayList<QuoteHistoryResult> quotes = new ArrayList<>();;
        try {
            JSONObject jsonObject = new JSONObject(response);
            if (jsonObject.length() != 0) {
                jsonObject = jsonObject.getJSONObject("query");
                int count = Integer.parseInt(jsonObject.getString("count"));
                if (count > 0) {
                    JSONArray resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");
                    if (resultsArray != null && resultsArray.length() != 0) {
                        for (int i = 0; i < resultsArray.length(); i++) {
                            jsonObject = resultsArray.getJSONObject(i);
                            String date = jsonObject.getString("Date");
                            float open = Float.parseFloat(Utils.truncateBidPrice(jsonObject.getString("Open")));
                            float close = Float.parseFloat(Utils.truncateBidPrice(jsonObject.getString("Close")));
                            QuoteHistoryResult quote = new QuoteHistoryResult();
                            quote.setDate(date);
                            quote.setOpen(open);
                            quote.setClose(close);
                            quotes.add(quote);
                        }
                    }
                }
            }

        } catch (JSONException e) {
            Log.e(LOG_TAG, "String to JSON failed: " + e);
        }

        return quotes;
    }

    private String getEndDate() {
        Calendar c = Calendar.getInstance();
        Date date = c.getTime();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return format.format(date);
    }

    private String getStartDate() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MONTH, -1);
        Date date = c.getTime();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return format.format(date);
    }

    private int handleQuoteQuery(TaskParams params) {
        Cursor initQueryCursor;
        StringBuilder urlStringBuilder = new StringBuilder(YAHOOAPI_YQL_URL);

        if (params.getTag().equals(INIT) || params.getTag().equals(PERIODIC)) {
            isUpdate = true;
            initQueryCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                    new String[]{"Distinct " + QuoteColumns.SYMBOL}, null,
                    null, null);
            if (initQueryCursor == null || initQueryCursor.getCount() == 0) {
                // Init task. Populates DB with quotes for the symbols seen below
                try {
                    String initialSymbolsQuery = String.format(YQL_QUOTE_QUERY, "\"YHOO\",\"AAPL\",\"GOOG\",\"TSLA\")");
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
                    mStoredSymbols.append("\"" +
                            initQueryCursor.getString(initQueryCursor.getColumnIndex("symbol")) + "\",");
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

        String urlString;
        String getResponse;
        int result = GcmNetworkManager.RESULT_FAILURE;

        if (urlStringBuilder != null) {
            urlString = urlStringBuilder.toString();
            try {
                getResponse = fetchData(urlString);
                result = GcmNetworkManager.RESULT_SUCCESS;
                try {
                    ContentValues contentValues = new ContentValues();
                    // update ISCURRENT to 0 (false) so new data is current
                    if (isUpdate) {
                        contentValues.put(QuoteColumns.ISCURRENT, 0);
                        mContext.getContentResolver().update(QuoteProvider.Quotes.CONTENT_URI, contentValues,
                                null, null);
                    }
                    mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
                            Utils.quoteJsonToContentVals(getResponse));
                } catch (RemoteException | OperationApplicationException e) {
                    Log.e(LOG_TAG, "Error applying batch insert", e);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public static final String QUOTE_HISTORY_INTENT = "quote_history_intent";
    public static final String QUOTE_HISTORY_VALUES = "quote_history_values";
    public static final String QUOTE_HISTORY_RESULT = "quote_history_result";

    private void broadcastHistoryResponse(ArrayList<QuoteHistoryResult> quotes, int result) {
        Intent intent = new Intent(QUOTE_HISTORY_INTENT);
        intent.putExtra(QUOTE_HISTORY_RESULT, result);
        if (result == GcmNetworkManager.RESULT_SUCCESS) {
            intent.putParcelableArrayListExtra(QUOTE_HISTORY_VALUES, quotes);
        }
        mContext.sendBroadcast(intent);
    }


}
