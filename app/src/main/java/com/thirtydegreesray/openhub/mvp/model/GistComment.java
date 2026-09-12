

package com.thirtydegreesray.openhub.mvp.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

/**
 * A comment on a Gist. Same shape as an issue comment - GitHub's gist
 * comments API returns the identical {id, body, user, created_at, updated_at}
 * fields - but kept as its own model rather than reusing IssueEvent, since
 * IssueEvent also carries issue-timeline-specific fields (event type, actor,
 * etc.) that don't apply here.
 */
public class GistComment implements Parcelable {

    private long id;
    private String body;
    private User user;
    @SerializedName("created_at") private Date createdAt;
    @SerializedName("updated_at") private Date updatedAt;

    public GistComment() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.id);
        dest.writeString(this.body);
        dest.writeParcelable(this.user, flags);
        dest.writeLong(this.createdAt != null ? this.createdAt.getTime() : -1);
        dest.writeLong(this.updatedAt != null ? this.updatedAt.getTime() : -1);
    }

    protected GistComment(Parcel in) {
        this.id = in.readLong();
        this.body = in.readString();
        this.user = in.readParcelable(User.class.getClassLoader());
        long tmpCreatedAt = in.readLong();
        this.createdAt = tmpCreatedAt == -1 ? null : new Date(tmpCreatedAt);
        long tmpUpdatedAt = in.readLong();
        this.updatedAt = tmpUpdatedAt == -1 ? null : new Date(tmpUpdatedAt);
    }

    public static final Creator<GistComment> CREATOR = new Creator<GistComment>() {
        @Override
        public GistComment createFromParcel(Parcel source) {
            return new GistComment(source);
        }

        @Override
        public GistComment[] newArray(int size) {
            return new GistComment[size];
        }
    };
}
