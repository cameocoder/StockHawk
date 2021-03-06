package com.sam_chordas.android.stockhawk.ui;

import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.melnykov.fab.FloatingActionButton;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.QuoteCursorAdapter;
import com.sam_chordas.android.stockhawk.rest.RecyclerViewItemClickListener;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.service.StockTaskService;
import com.sam_chordas.android.stockhawk.touch_helper.SimpleItemTouchHelperCallback;
import com.sam_chordas.android.stockhawk.utils.IntentExtras;

import java.util.Date;

import butterknife.Bind;
import butterknife.ButterKnife;

import static com.sam_chordas.android.stockhawk.service.StockTaskService.ADD;
import static com.sam_chordas.android.stockhawk.service.StockTaskService.INIT;
import static com.sam_chordas.android.stockhawk.service.StockTaskService.PERIODIC;
import static com.sam_chordas.android.stockhawk.service.StockTaskService.SYMBOL;
import static com.sam_chordas.android.stockhawk.utils.IntentExtras.QUOTE_QUERY_INTENT;
import static com.sam_chordas.android.stockhawk.utils.IntentExtras.QUOTE_RESULT;

public class MyStocksActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int CURSOR_LOADER_ID = 0;

    @Bind(R.id.stock_list)
    RecyclerView stockList;
    @Bind(R.id.empty_view)
    TextView emptyView;
    @Bind(R.id.last_updated)
    TextView lastUpdated;
    @Bind(R.id.fab)
    FloatingActionButton fab;
    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;
    private Intent mServiceIntent;
    private QuoteCursorAdapter mCursorAdapter;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equalsIgnoreCase(QUOTE_QUERY_INTENT)) {
                int resultCode = intent.getExtras().getInt(QUOTE_RESULT);
                if (resultCode == GcmNetworkManager.RESULT_FAILURE) {
                    Toast.makeText(context, getString(R.string.fetch_failure), Toast.LENGTH_LONG).show();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_stocks);
        ButterKnife.bind(this);

        registerReceiver(broadcastReceiver, new IntentFilter(QUOTE_QUERY_INTENT));
        // The intent service is for executing immediate pulls from the Yahoo API
        // GCMTaskService can only schedule tasks, they cannot execute immediately
        mServiceIntent = new Intent(this, StockIntentService.class);
        if (savedInstanceState == null) {
            // Run the initialize task service so that some stocks appear upon an empty database
            mServiceIntent.putExtra(StockTaskService.TAG, INIT);
            if (isConnected()) {
                startService(mServiceIntent);
            } else {
                networkToast();
            }
        }

        stockList.setLayoutManager(new LinearLayoutManager(this));
        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);

        mCursorAdapter = new QuoteCursorAdapter(this, null);
        stockList.addOnItemTouchListener(new RecyclerViewItemClickListener(this,
                new RecyclerViewItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View v, int position) {
                        Cursor cursor = mCursorAdapter.getCursor();
                        cursor.moveToPosition(position);
                        String symbol = cursor.getString(cursor.getColumnIndex(QuoteColumns.SYMBOL));
                        final Bundle arguments = new Bundle();
                        arguments.putString(IntentExtras.ARG_SYMBOL, symbol);
                        Intent intent = new Intent(getApplicationContext(), SymbolDetailActivity.class);
                        intent.putExtras(arguments);
                        startActivity(intent);
                    }
                }));
        stockList.setAdapter(mCursorAdapter);

        fab.attachToRecyclerView(stockList);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleFabOnClick();

            }
        });

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mCursorAdapter);
        ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(stockList);

        mTitle = getTitle();
        if (isConnected()) {
            long period = 3600L;
            long flex = 10L;
            String periodicTag = PERIODIC;

            // create a periodic task to pull stocks once every hour after the app has been opened. This
            // is so Widget data stays up to date.
            PeriodicTask periodicTask = new PeriodicTask.Builder()
                    .setService(StockTaskService.class)
                    .setPeriod(period)
                    .setFlex(flex)
                    .setTag(periodicTag)
                    .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                    .setRequiresCharging(false)
                    .build();
            // Schedule task with tag "periodic." This ensure that only the stocks present in the DB
            // are updated.
            GcmNetworkManager.getInstance(this).schedule(periodicTask);
        }

        updateLastUpdated();
    }

    private void updateLastUpdated() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Date lastUpdatedDate = new Date(prefs.getLong(Utils.PREF_LAST_UPDATED_TIME, 0));
        if (lastUpdatedDate.getTime() > 0) {
            String lastUpdatedString = DateUtils.formatDateTime(this, lastUpdatedDate.getTime(),
                    DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_ALL);
            lastUpdated.setText(String.format(getString(R.string.last_updated), lastUpdatedString));
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }

    public void networkToast() {
        Toast.makeText(this, getString(R.string.network_toast), Toast.LENGTH_LONG).show();
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(mTitle);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_stocks, menu);
        restoreActionBar();

        MenuItem actionChangeUnits = menu.findItem(R.id.action_change_units);
        updateChangeUnitsMenuItem(actionChangeUnits);
        return true;
    }

    private void updateChangeUnitsMenuItem(MenuItem actionChangeUnits) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean showPercent = prefs.getBoolean(Utils.PREF_SHOW_PERCENT, true);
        if (showPercent) {
            actionChangeUnits.setIcon(R.drawable.ic_attach_money_white_24dp);
        } else {
            actionChangeUnits.setIcon(R.drawable.ic_percent_white_24dp);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_change_units) {
            // this is for changing stock changes from percent value to dollar value
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(Utils.PREF_SHOW_PERCENT, !prefs.getBoolean(Utils.PREF_SHOW_PERCENT, true));
            editor.apply();

            updateChangeUnitsMenuItem(item);

            Utils.notifyAppWidgetViewDataChanged(this);

            this.getContentResolver().notifyChange(QuoteProvider.Quotes.CONTENT_URI, null);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This narrows the return to only the stocks that are most current.
        return new CursorLoader(this, QuoteProvider.Quotes.CONTENT_URI,
                new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                        QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                QuoteColumns.ISCURRENT + " = ?",
                new String[]{"1"},
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mCursorAdapter.swapCursor(data);
        showEmptyView(data.getCount() == 0);
        updateLastUpdated();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursorAdapter.swapCursor(null);
    }

    private void handleFabOnClick() {
        if (isConnected()) {
            new MaterialDialog.Builder(this).title(R.string.symbol_search)
                    .content(R.string.content_test)
                    .inputType(InputType.TYPE_CLASS_TEXT)
                    .input(R.string.input_hint, R.string.input_prefill, new MaterialDialog.InputCallback() {
                        @Override
                        public void onInput(MaterialDialog dialog, CharSequence input) {
                            // On FAB click, receive user input. Make sure the stock doesn't already exist
                            // in the DB and proceed accordingly
                            if (symbolExists(input)) {
                                Toast toast =
                                        Toast.makeText(MyStocksActivity.this, getString(R.string.stock_already_saved_toast),
                                                Toast.LENGTH_LONG);
                                toast.setGravity(Gravity.CENTER, Gravity.CENTER, 0);
                                toast.show();
                            } else {
                                // Add the stock to DB
                                mServiceIntent.putExtra(StockTaskService.TAG, ADD);
                                mServiceIntent.putExtra(SYMBOL, input.toString());
                                startService(mServiceIntent);
                            }
                        }
                    })
                    .show();
        } else {
            networkToast();
        }
    }

    private boolean symbolExists(CharSequence input) {
        Cursor c = null;
        try {
            c = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                    new String[]{QuoteColumns.SYMBOL}, QuoteColumns.SYMBOL + "= ?",
                    new String[]{input.toString()}, null);
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return (c != null) && (c.getCount() != 0);
    }

    private void showEmptyView(boolean isVisible) {
        if (isVisible) {
            emptyView.setVisibility(View.VISIBLE);
        } else {
            emptyView.setVisibility(View.GONE);
        }
    }

    private boolean isConnected() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

}
