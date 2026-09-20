

package com.thirtydegreesray.openhub.mvp.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

/**
 * Created by ThirtyDegreesRay on 2017/8/15 17:56:03
 */

public class Branch implements Parcelable {

    private String name;
    @SerializedName("zipball_url") private String zipballUrl;
    @SerializedName("tarball_url") private String tarballUrl;
    // The branches/tags list endpoints only ever populate sha+url here (no
    // date - see the sample response at the bottom of this file). Once the
    // presenter fetches that commit's full info, it swaps this reference for
    // a RepoCommitExt (a RepoCommit subclass) carrying the committer date.
    private RepoCommit commit;

    private boolean isBranch = true;

    public Branch() {
    }

    public Branch(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getZipballUrl() {
        return zipballUrl;
    }

    public void setZipballUrl(String zipballUrl) {
        this.zipballUrl = zipballUrl;
    }

    public String getTarballUrl() {
        return tarballUrl;
    }

    public void setTarballUrl(String tarballUrl) {
        this.tarballUrl = tarballUrl;
    }

    public boolean isBranch() {
        return isBranch;
    }

    public void setBranch(boolean branch) {
        isBranch = branch;
    }

    public RepoCommit getCommit() {
        return commit;
    }

    public void setCommit(RepoCommit commit) {
        this.commit = commit;
    }

    /**
     * Null until the presenter has fetched this entry's commit info (the
     * branches/tags list endpoints themselves never include a date - see
     * the sample response at the bottom of this file).
     */
    @Nullable
    public Date getUpdatedAt() {
        if (commit == null || commit.getCommit() == null || commit.getCommit().getCommitter() == null) {
            return null;
        }
        return commit.getCommit().getCommitter().getDate();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
        dest.writeString(this.zipballUrl);
        dest.writeString(this.tarballUrl);
        dest.writeByte(this.isBranch ? (byte) 1 : (byte) 0);
        dest.writeParcelable(this.commit, flags);
    }

    protected Branch(Parcel in) {
        this.name = in.readString();
        this.zipballUrl = in.readString();
        this.tarballUrl = in.readString();
        this.isBranch = in.readByte() != 0;
        this.commit = in.readParcelable(RepoCommit.class.getClassLoader());
    }

    public static final Parcelable.Creator<Branch> CREATOR = new Parcelable.Creator<Branch>() {
        @Override
        public Branch createFromParcel(Parcel source) {
            return new Branch(source);
        }

        @Override
        public Branch[] newArray(int size) {
            return new Branch[size];
        }
    };
}

//{
//        "name": "2.1-nightly-26",
//        "zipball_url": "https://api.github.com/repos/pxb1988/dex2jar/zipball/2.1-nightly-26",
//        "tarball_url": "https://api.github.com/repos/pxb1988/dex2jar/tarball/2.1-nightly-26",
//        "commit": {
//        "sha": "d69d6c17c67e0e34862cb1f688905017d106c5b7",
//        "url": "https://api.github.com/repos/pxb1988/dex2jar/commits/d69d6c17c67e0e34862cb1f688905017d106c5b7"
//        }
//        }