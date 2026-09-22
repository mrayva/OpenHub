package com.thirtydegreesray.openhub.util;

import androidx.annotation.NonNull;

import com.thirtydegreesray.openhub.mvp.model.Notification;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Overrides a notification's server-reported unread state right after this
 * app marks it read, until the server's own updated_at moves past what it
 * was at that moment - static (not a NotificationsPresenter field) because
 * NotificationsActivity has no launchMode override, so navigating away
 * (e.g. to Issues) and back via MainActivity's drawer - as opposed to the
 * system Back button, which resumes the same instance - creates a brand
 * new Activity/Fragment/Presenter, discarding any purely in-memory,
 * presenter-scoped record of what was just marked read. Confirmed live:
 * this is exactly the repro behind "check off notifications, switch to
 * Issues, switch back - they're unread again."
 *
 * Keyed by updated_at (not just a boolean/timestamp-of-marking) so a
 * genuinely new notification on the same thread - GitHub reuses the same
 * id and flips unread back to true, bumping updated_at - is never masked:
 * only a server response whose updated_at hasn't moved past what was
 * recorded here gets its unread flag overridden; anything newer clears the
 * entry and is trusted as-is.
 */
public class LocallyReadNotificationsHelper {

    private static final Map<String, Date> readAsOfUpdateAt = Collections.synchronizedMap(new HashMap<>());

    public static void markRead(@NonNull Notification notification) {
        readAsOfUpdateAt.put(notification.getId(), notification.getUpdateAt());
    }

    public static void undoMarkRead(@NonNull String notificationId) {
        readAsOfUpdateAt.remove(notificationId);
    }

    /**
     * Applies the override in place if applicable: sets unread=false when
     * the server still reports this notification at or before the
     * updated_at it had when marked read (propagation lag), or clears the
     * stale entry and leaves the notification untouched when the server's
     * updated_at has moved past that point (genuinely new activity).
     */
    public static void applyOverride(@NonNull Notification notification) {
        Date markedReadAsOf = readAsOfUpdateAt.get(notification.getId());
        if (markedReadAsOf == null) return;
        Date serverUpdateAt = notification.getUpdateAt();
        if (serverUpdateAt == null || !serverUpdateAt.after(markedReadAsOf)) {
            notification.setUnread(false);
        } else {
            readAsOfUpdateAt.remove(notification.getId());
        }
    }

}
