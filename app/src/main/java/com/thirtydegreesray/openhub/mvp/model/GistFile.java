

package com.thirtydegreesray.openhub.mvp.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

/**
 * One file within a Gist. GitHub's gist API returns files as a map keyed by
 * filename (see Gist.files) rather than an array, so this model only carries
 * the per-file fields - the filename itself lives as the map key.
 *
 * content is present inline on a single-gist fetch for text files under
 * GitHub's size threshold; null for list responses (which never include file
 * bodies) and for files GitHub truncates (see truncated).
 */
public class GistFile implements Parcelable {

    private String filename;
    private String type;
    private String language;
    @SerializedName("raw_url") private String rawUrl;
    private long size;
    private String content;
    private boolean truncated;

    public GistFile() {
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getRawUrl() {
        return rawUrl;
    }

    public void setRawUrl(String rawUrl) {
        this.rawUrl = rawUrl;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isTruncated() {
        return truncated;
    }

    public void setTruncated(boolean truncated) {
        this.truncated = truncated;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.filename);
        dest.writeString(this.type);
        dest.writeString(this.language);
        dest.writeString(this.rawUrl);
        dest.writeLong(this.size);
        dest.writeString(this.content);
        dest.writeByte(this.truncated ? (byte) 1 : (byte) 0);
    }

    protected GistFile(Parcel in) {
        this.filename = in.readString();
        this.type = in.readString();
        this.language = in.readString();
        this.rawUrl = in.readString();
        this.size = in.readLong();
        this.content = in.readString();
        this.truncated = in.readByte() != 0;
    }

    public static final Creator<GistFile> CREATOR = new Creator<GistFile>() {
        @Override
        public GistFile createFromParcel(Parcel source) {
            return new GistFile(source);
        }

        @Override
        public GistFile[] newArray(int size) {
            return new GistFile[size];
        }
    };
}
