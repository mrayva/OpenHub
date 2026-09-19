package com.thirtydegreesray.openhub.mvp.model.request;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

/**
 * Created by ThirtyDegreesRay on 2017/12/28 11:08:16
 *
 * lastReadAt is deliberately left null: GitHub requires this field, when
 * present, to be an ISO-8601 timestamp, but Gson's default Date
 * serialization (no custom adapter is registered anywhere in this app - see
 * AppRetrofit's plain GsonConverterFactory.create()) writes Date as a
 * locale-formatted string like "Sep 19, 2026, 12:27:59 PM" instead. That
 * malformed value was silently rejected/ignored by GitHub's API on every
 * "mark as read" call (PUT /repos/{owner}/{repo}/notifications and PUT
 * /notifications both take this model as their body), so notifications
 * marked read locally reverted to unread the moment the screen was
 * reloaded from the network - it never actually took effect server-side.
 * Per GitHub's docs, omitting last_read_at entirely defaults to the
 * current timestamp and marks everything read, which is exactly the
 * intended behavior here, without needing to hand-format ISO-8601 at all.
 */
public class MarkNotificationReadRequestModel {

    @SerializedName("last_read_at") private Date lastReadAt;

    public static MarkNotificationReadRequestModel newInstance(){
        return new MarkNotificationReadRequestModel();
    }

    public Date getLastReadAt() {
        return lastReadAt;
    }

    public void setLastReadAt(Date lastReadAt) {
        this.lastReadAt = lastReadAt;
    }
}
