package com.sam_chordas.android.stockhawk.rest;

import android.os.Parcel;
import android.os.Parcelable;

public class QuoteHistoryResult implements Parcelable {

    private String Symbol;
    private String Date;
    private float Open;
    private float High;
    private float Low;
    private float Close;
    private String Volume;
    private float AdjClose;

    /**
     * No args constructor for use in serialization
     */
    public QuoteHistoryResult() {
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
     * @return The Date
     */
    public String getDate() {
        return Date;
    }

    /**
     * @param Date The Date
     */
    public void setDate(String Date) {
        this.Date = Date;
    }

    /**
     * @return The opening bid
     */
    public float getOpen() {
        return Open;
    }

    /**
     * @param Open The Opening bid
     */
    public void setOpen(float Open) {
        this.Open = Open;
    }

    /**
     * @return The High bid
     */
    public float getHigh() {
        return High;
    }

    /**
     * @param High The High bid
     */
    public void setHigh(float High) {
        this.High = High;
    }

    /**
     * @return The Low bid
     */
    public float getLow() {
        return Low;
    }

    /**
     * @param Low The Low bid
     */
    public void setLow(float Low) {
        this.Low = Low;
    }

    /**
     * @return The Close bid
     */
    public float getClose() {
        return Close;
    }

    /**
     * @param Close The Close bid
     */
    public void setClose(float Close) {
        this.Close = Close;
    }

    /**
     * @return The Volume
     */
    public String getVolume() {
        return Volume;
    }

    /**
     * @param Volume The Volume
     */
    public void setVolume(String Volume) {
        this.Volume = Volume;
    }

    /**
     * @return The Adjusted Close price
     */
    public float getAdjClose() {
        return AdjClose;
    }

    /**
     * @param AdjClose The Adjusted Close price
     */
    public void setAdjClose(float AdjClose) {
        this.AdjClose = AdjClose;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(Symbol);
        dest.writeString(Date);
        dest.writeFloat(Open);
        dest.writeFloat(High);
        dest.writeFloat(Low);
        dest.writeFloat(Close);
        dest.writeString(Volume);
        dest.writeFloat(AdjClose);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public QuoteHistoryResult createFromParcel(Parcel in) {
            return new QuoteHistoryResult(in);
        }

        public QuoteHistoryResult[] newArray(int size) {
            return new QuoteHistoryResult[size];
        }
    };

    private QuoteHistoryResult(Parcel in) {
        Symbol = in.readString();
        Date = in.readString();
        Open = in.readFloat();
        High = in.readFloat();
        Low = in.readFloat();
        Close = in.readFloat();
        Volume = in.readString();
        AdjClose = in.readFloat();
    }

}