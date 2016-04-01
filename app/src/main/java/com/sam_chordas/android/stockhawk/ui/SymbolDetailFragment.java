package com.sam_chordas.android.stockhawk.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.db.chart.model.LineSet;
import com.db.chart.model.Point;
import com.db.chart.view.LineChartView;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.rest.QuoteHistoryResult;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.service.StockTaskService;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;

import static com.sam_chordas.android.stockhawk.service.StockTaskService.HISTORICAL_DATA;
import static com.sam_chordas.android.stockhawk.service.StockTaskService.SYMBOL;
import static com.sam_chordas.android.stockhawk.service.StockTaskService.TAG;

/**
 * A fragment representing a single Symbol detail screen.
 * This fragment is either contained in a {@link SymbolListActivity}
 * in two-pane mode (on tablets) or a {@link SymbolDetailActivity}
 * on handsets.
 */
public class SymbolDetailFragment extends Fragment {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_SYMBOL = "symbol";

    private String symbol;

    @Bind(R.id.linechart)
    LineChartView lineChart;

    private ArrayList<QuoteHistoryResult> quotes;

    private BroadcastReceiver historyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equalsIgnoreCase(StockTaskService.QUOTE_HISTORY_INTENT)) {
                int resultCode = intent.getExtras().getInt(StockTaskService.QUOTE_HISTORY_RESULT);
                if (resultCode == GcmNetworkManager.RESULT_SUCCESS) {
                    quotes = intent.getExtras().getParcelableArrayList(StockTaskService.QUOTE_HISTORY_VALUES);
                    processQuotes();
                }
            }
        }
    };

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SymbolDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_SYMBOL)) {
            symbol = getArguments().getString(ARG_SYMBOL);
        }

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null) {
            ActionBar actionBar = activity.getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(symbol);
            }

            activity.registerReceiver(historyReceiver, new IntentFilter(StockTaskService.QUOTE_HISTORY_INTENT));
        }
        if (savedInstanceState == null) {
            if (activity != null) {
                Intent mServiceIntent = new Intent(activity, StockIntentService.class);
                mServiceIntent.putExtra(TAG, HISTORICAL_DATA);
                mServiceIntent.putExtra(SYMBOL, symbol);
                activity.startService(mServiceIntent);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.symbol_detail, container, false);
        ButterKnife.bind(this, view);

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList(StockTaskService.QUOTE_HISTORY_VALUES, quotes);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(StockTaskService.QUOTE_HISTORY_VALUES)) {
                quotes = savedInstanceState.getParcelableArrayList(StockTaskService.QUOTE_HISTORY_VALUES);
                processQuotes();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null) {
            activity.unregisterReceiver(historyReceiver);
        }
    }

    private void processQuotes() {
        LineSet dataset = new LineSet();
        int line = getResources().getColor(R.color.chart_line);
        int fill = getResources().getColor(R.color.chart_fill);
        dataset.setColor(line);
        dataset.setFill(fill);

        float minValue = quotes.get(0).getClose();
        float maxValue = 0;
        for (QuoteHistoryResult quote : quotes) {
            float close = quote.getClose();
            dataset.addPoint(new Point(quote.getDate(), close));
            if (close > maxValue) {
                maxValue = close;
            }
            if (close < minValue) {
                minValue = close;
            }
        }

        lineChart.setAxisBorderValues((int) Math.floor(minValue), (int) Math.ceil(maxValue));
        lineChart.addData(dataset);
        lineChart.show();
    }

}
