

package com.thirtydegreesray.openhub.mvp.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;
import com.thirtydegreesray.openhub.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A GitHub Gist. files comes back from the API as a JSON object keyed by
 * filename (not an array), e.g. {"a.txt": {...}, "b.py": {...}} - kept as a
 * Map here to match that shape directly rather than translating it, since
 * Gson maps a JSON object straight onto a Map<String, GistFile> with no
 * custom deserializer needed.
 */
public class Gist implements Parcelable {

    @SerializedName("id") private String id;
    private String description;
    @SerializedName("html_url") private String htmlUrl;
    @SerializedName("created_at") private Date createdAt;
    @SerializedName("updated_at") private Date updatedAt;
    private int comments;
    private User owner;
    private Map<String, GistFile> files;

    public Gist() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
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

    public int getComments() {
        return comments;
    }

    public void setComments(int comments) {
        this.comments = comments;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public Map<String, GistFile> getFiles() {
        return files;
    }

    public void setFiles(Map<String, GistFile> files) {
        this.files = files;
    }

    public ArrayList<GistFile> getFileList() {
        ArrayList<GistFile> list = new ArrayList<>();
        if (files != null) list.addAll(files.values());
        return list;
    }

    /**
     * Mirrors FastHub-RE's Gist.getDisplayTitle(): first filename if there is
     * one, falling back to the description, falling back to a placeholder -
     * a gist very often has a blank description, so showing "untitled" for
     * every one of those would be far less useful than the first filename.
     */
    public String getDisplayTitle() {
        if (files != null && !files.isEmpty()) {
            return files.values().iterator().next().getFilename();
        }
        if (!StringUtils.isBlank(description)) {
            return description;
        }
        return id;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.description);
        dest.writeString(this.htmlUrl);
        dest.writeLong(this.createdAt != null ? this.createdAt.getTime() : -1);
        dest.writeLong(this.updatedAt != null ? this.updatedAt.getTime() : -1);
        dest.writeInt(this.comments);
        dest.writeParcelable(this.owner, flags);
        dest.writeInt(this.files == null ? -1 : this.files.size());
        if (this.files != null) {
            for (Map.Entry<String, GistFile> entry : this.files.entrySet()) {
                dest.writeString(entry.getKey());
                dest.writeParcelable(entry.getValue(), flags);
            }
        }
    }

    protected Gist(Parcel in) {
        this.id = in.readString();
        this.description = in.readString();
        this.htmlUrl = in.readString();
        long tmpCreatedAt = in.readLong();
        this.createdAt = tmpCreatedAt == -1 ? null : new Date(tmpCreatedAt);
        long tmpUpdatedAt = in.readLong();
        this.updatedAt = tmpUpdatedAt == -1 ? null : new Date(tmpUpdatedAt);
        this.comments = in.readInt();
        this.owner = in.readParcelable(User.class.getClassLoader());
        int filesSize = in.readInt();
        if (filesSize == -1) {
            this.files = null;
        } else {
            this.files = new LinkedHashMap<>();
            for (int i = 0; i < filesSize; i++) {
                String key = in.readString();
                GistFile value = in.readParcelable(GistFile.class.getClassLoader());
                this.files.put(key, value);
            }
        }
    }

    public static final Creator<Gist> CREATOR = new Creator<Gist>() {
        @Override
        public Gist createFromParcel(Parcel source) {
            return new Gist(source);
        }

        @Override
        public Gist[] newArray(int size) {
            return new Gist[size];
        }
    };
}
