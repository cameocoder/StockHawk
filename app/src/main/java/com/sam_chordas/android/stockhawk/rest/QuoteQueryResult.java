package com.sam_chordas.android.stockhawk.rest;

import android.os.Parcel;
import android.os.Parcelable;

public class QuoteQueryResult implements Parcelable {

    private String Symbol;
    private String PercentChange;
    private String Change;
    private String BidPrice;
    private String Created;
    private int isUp;
    private int isCurrent;

    /**
     * No args constructor for use in serialization
     */
    public QuoteQueryResult() {
    }

    /**
     * @return The Symbol
     */
    public String getSymbol() {
        return Symbol;
    }

    /**
     * @param Symbol The Symbol
     */
    public void setSymbol(String Symbol) {
        this.Symbol = Symbol;
    }

    /**
     * @return The PercentChange
     */
    public String getPercentChange() {
        return PercentChange;
    }

    /**
     * @param Date The PercentChange
     */
    public void setPercentChange(String Date) {
        this.PercentChange = Date;
    }

    /**
     * @return The opening bid
     */
    public String getChange() {
        return Change;
    }

    /**
     * @param Open The Opening bid
     */
    public void setChange(String Open) {
        this.Change = Open;
    }

    /**
     * @return The BidPrice bid
     */
    public String getBidPrice() {
        return BidPrice;
    }

    /**
     * @param High The BidPrice bid
     */
    public void setBidPrice(String High) {
        this.BidPrice = High;
    }

    /**
     * @return The Created bid
     */
    public String getCreated() {
        return Created;
    }

    /**
     * @param Low The Created bid
     */
    public void setCreated(String Low) {
        this.Created = Low;
    }

    /**
     * @return The isUp
     */
    public int getIsUp() {
        return isUp;
    }

    /**
     * @param Volume The isUp
     */
    public void setIsUp(int Volume) {
        this.isUp = Volume;
    }

    /**
     * @return The Adjusted Close price
     */
    public int getIsCurrent() {
        return isCurrent;
    }

    /**
     * @param AdjClose The Adjusted Close price
     */
    public void setIsCurrent(int AdjClose) {
        this.isCurrent = AdjClose;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(Symbol);
        dest.writeString(PercentChange);
        dest.writeString(Change);
        dest.writeString(BidPrice);
        dest.writeString(Created);
        dest.writeInt(isUp);
        dest.writeInt(isCurrent);
    }

    public static final Creator CREATOR = new Creator() {
        public QuoteQueryResult createFromParcel(Parcel in) {
            return new QuoteQueryResult(in);
        }

        public QuoteQueryResult[] newArray(int size) {
            return new QuoteQueryResult[size];
        }
    };

    private QuoteQueryResult(Parcel in) {
        Symbol = in.readString();
        PercentChange = in.readString();
        Change = in.readString();
        BidPrice = in.readString();
        Created = in.readString();
        isUp = in.readInt();
        isCurrent = in.readInt();
    }

}