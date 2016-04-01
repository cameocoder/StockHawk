package com.sam_chordas.android.stockhawk.service;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.rest.Utils;

import java.util.Date;

import static com.sam_chordas.android.stockhawk.service.StockTaskService.ADD;
import static com.sam_chordas.android.stockhawk.service.StockTaskService.HISTORICAL_DATA;
import static com.sam_chordas.android.stockhawk.service.StockTaskService.SYMBOL;
import static com.sam_chordas.android.stockhawk.service.StockTaskService.TAG;

/**
 * Created by sam_chordas on 10/1/15.
 */
public class StockIntentService extends IntentService {

    public StockIntentService() {
        super(StockIntentService.class.getName());
    }

    public StockIntentService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(StockIntentService.class.getSimpleName(), "Stock Intent Service");
        StockTaskService stockTaskService = new StockTaskService(this);
        Bundle args = new Bundle();
        if (intent.getStringExtra(TAG).equals(ADD)
                || intent.getStringExtra(TAG).equals(HISTORICAL_DATA)) {
            args.putString(SYMBOL, intent.getStringExtra(SYMBOL));
        }
        
        // We can call OnRunTask from the intent service to force it to run immediately instead of
        // scheduling a task.
        int rc = stockTaskService.onRunTask(new TaskParams(intent.getStringExtra(TAG), args));
        if (rc == GcmNetworkManager.RESULT_SUCCESS) {
            lastUpdated();
        }
    }

    private void lastUpdated() {
        Date date = new Date();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(Utils.PREF_LAST_UPDATED_TIME, date.getTime());
        editor.apply();
    }
}
