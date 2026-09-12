package com.thirtydegreesray.openhub.mvp.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * One file row while composing a gist in CreateGistActivity - separate from
 * GistFile (the read-only API response model) since it also needs to track
 * the filename it originally had (see CreateGistPresenter's rename-as-
 * delete-plus-add diffing against originalFilenames).
 */
public class GistFileEntry implements Parcelable {

    private String filename;
    private String content;

    public GistFileEntry(String filename, String content) {
        this.filename = filename;
        this.content = content;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.filename);
        dest.writeString(this.content);
    }

    protected GistFileEntry(Parcel in) {
        this.filename = in.readString();
        this.content = in.readString();
    }

    public static final Creator<GistFileEntry> CREATOR = new Creator<GistFileEntry>() {
        @Override
        public GistFileEntry createFromParcel(Parcel source) {
            return new GistFileEntry(source);
        }

        @Override
        public GistFileEntry[] newArray(int size) {
            return new GistFileEntry[size];
        }
    };
}
