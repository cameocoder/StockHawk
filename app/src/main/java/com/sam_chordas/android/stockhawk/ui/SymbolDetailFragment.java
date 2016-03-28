package com.sam_chordas.android.stockhawk.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.service.StockTaskService;
import com.sam_chordas.android.stockhawk.ui.dummy.DummyContent;

import butterknife.ButterKnife;

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

    /**
     * The dummy content this fragment is presenting.
     */
    private DummyContent.DummyItem mItem;

    private String symbol;
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
            Intent mServiceIntent = new Intent(activity, StockIntentService.class);

            mServiceIntent.putExtra(StockTaskService.TAG, StockTaskService.HISTORICAL_DATA);
            mServiceIntent.putExtra(StockTaskService.SYMBOL, symbol);
            activity.startService(mServiceIntent);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.symbol_detail, container, false);
        ButterKnife.bind(this, view);

        if (mItem != null) {
            ((TextView) view.findViewById(R.id.symbol_detail)).setText(mItem.details);
        }

        return view;
    }
}
