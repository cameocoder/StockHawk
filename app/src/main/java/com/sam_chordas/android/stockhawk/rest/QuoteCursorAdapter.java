package com.sam_chordas.android.stockhawk.rest;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.touch_helper.ItemTouchHelperAdapter;
import com.sam_chordas.android.stockhawk.touch_helper.ItemTouchHelperViewHolder;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by sam_chordas on 10/6/15.
 * Credit to skyfishjy gist:
 * https://gist.github.com/skyfishjy/443b7448f59be978bc59
 * for the code structure
 */
public class QuoteCursorAdapter extends CursorRecyclerViewAdapter<QuoteCursorAdapter.ViewHolder>
        implements ItemTouchHelperAdapter {

    private Context context;
    private static Typeface robotoLight;
    //private final OnStartDragListener mDragListener;
    private boolean isPercent;

    public QuoteCursorAdapter(Context context, Cursor cursor) {
        super(context, cursor);
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        robotoLight = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Light.ttf");
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_quote, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, final Cursor cursor) {
        viewHolder.symbol.setText(cursor.getString(cursor.getColumnIndex("symbol")));
        viewHolder.bidPrice.setText(cursor.getString(cursor.getColumnIndex("bid_price")));
        if (cursor.getInt(cursor.getColumnIndex("is_up")) == 1) {
            viewHolder.change.setBackground(
                    context.getResources().getDrawable(R.drawable.percent_change_pill_green));
        } else {
            viewHolder.change.setBackground(
                    context.getResources().getDrawable(R.drawable.percent_change_pill_red));
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (prefs.getBoolean(Utils.PREF_SHOW_PERCENT, true)) {
            viewHolder.change.setText(cursor.getString(cursor.getColumnIndex("percent_change")));
        } else {
            viewHolder.change.setText(cursor.getString(cursor.getColumnIndex("change")));
        }
    }

    @Override
    public void onItemDismiss(int position) {
        Cursor c = getCursor();
        c.moveToPosition(position);
        String symbol = c.getString(c.getColumnIndex(QuoteColumns.SYMBOL));
        context.getContentResolver().delete(QuoteProvider.Quotes.withSymbol(symbol), null, null);
        notifyItemRemoved(position);
        Utils.notifyAppWidgetViewDataChanged(context);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
            implements ItemTouchHelperViewHolder, View.OnClickListener {
        @Bind(R.id.stock_symbol)
        TextView symbol;
        @Bind(R.id.bid_price)
        TextView bidPrice;
        @Bind(R.id.change)
        TextView change;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            symbol.setTypeface(robotoLight);
        }

        @Override
        public void onItemSelected() {
            itemView.setBackgroundColor(Color.DKGRAY);
        }

        @Override
        public void onItemClear() {
            itemView.setBackgroundColor(0);
        }

        @Override
        public void onClick(View v) {

        }
    }
}
