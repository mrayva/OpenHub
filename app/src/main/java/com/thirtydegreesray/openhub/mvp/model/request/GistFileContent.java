package com.thirtydegreesray.openhub.mvp.model.request;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * One file entry within a create/edit gist request body - GitHub's gist
 * create/edit API expects {"files": {"filename": {"content": "..."}}}.
 */
public class GistFileContent implements Parcelable {

    private String content;

    public GistFileContent(String content) {
        this.content = content;
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
        dest.writeString(this.content);
    }

    protected GistFileContent(Parcel in) {
        this.content = in.readString();
    }

    public static final Creator<GistFileContent> CREATOR = new Creator<GistFileContent>() {
        @Override
        public GistFileContent createFromParcel(Parcel source) {
            return new GistFileContent(source);
        }

        @Override
        public GistFileContent[] newArray(int size) {
            return new GistFileContent[size];
        }
    };
}
