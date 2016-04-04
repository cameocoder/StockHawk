package com.sam_chordas.android.stockhawk.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.sam_chordas.android.stockhawk.R;
import com.squareup.picasso.Picasso;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.sam_chordas.android.stockhawk.utils.IntentExtras.ARG_SYMBOL;

/**
 * A fragment representing a single Symbol detail screen.
 * Get the chart data from yahoo chart api.
 */
public class YahooSymbolDetailFragment extends Fragment {

    private static final String BASE_CHART_URL = "http://chart.finance.yahoo.com/z?s=";
    private static final String CHART_ARG_TIME = "&t=";
    private static final String CHART_ARG_TIME_DAY = "1d";
    private static final String CHART_ARG_TIME_WEEK = "5d";
    private static final String CHART_ARG_TIME_MONTH = "1m";
    private static final String CHART_ARG_TIME_YEAR = "1y";
    private static final String CHART_ARG_TIME_MAX = "my";

    @Bind(R.id.chartView)
    ImageView lineChart;
    @Bind(R.id.time_1m)
    Button buttonTime1m;

    private String symbol;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public YahooSymbolDetailFragment() {
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
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.yahoo_symbol_detail, container, false);
        ButterKnife.bind(this, view);

        // load the default chart
        fetchChart(null);

        return view;
    }

    private void fetchChart(String timePeriod) {
        String chartUrl = BASE_CHART_URL + symbol;
        if (timePeriod != null) {
            chartUrl += CHART_ARG_TIME + timePeriod;
        }
        Picasso.with(getContext()).load(chartUrl).into(lineChart);
    }

    @OnClick(R.id.time_1d)
    void fetchChartDay() {
        fetchChart(CHART_ARG_TIME_DAY);
    }

    @OnClick(R.id.time_5d)
    void fetchChartWeek() {
        fetchChart(CHART_ARG_TIME_WEEK);
    }

    @OnClick(R.id.time_1m)
    void fetchChartMonth() {
        fetchChart(CHART_ARG_TIME_MONTH);
    }

    @OnClick(R.id.time_1y)
    void fetchChartYear() {
        fetchChart(CHART_ARG_TIME_YEAR);
    }

    @OnClick(R.id.time_max)
    void fetchChartMax() {
        fetchChart(CHART_ARG_TIME_MAX);
    }

}
