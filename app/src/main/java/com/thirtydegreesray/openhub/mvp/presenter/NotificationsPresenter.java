

package com.thirtydegreesray.openhub.mvp.presenter;

import androidx.annotation.NonNull;

import com.thirtydegreesray.dataautoaccess.annotation.AutoAccess;
import com.thirtydegreesray.openhub.dao.DaoSession;
import com.thirtydegreesray.openhub.http.core.HttpObserver;
import com.thirtydegreesray.openhub.http.core.HttpResponse;
import com.thirtydegreesray.openhub.http.core.HttpSubscriber;
import com.thirtydegreesray.openhub.mvp.contract.INotificationsContract;
import com.thirtydegreesray.openhub.mvp.model.Notification;
import com.thirtydegreesray.openhub.mvp.model.Repository;
import com.thirtydegreesray.openhub.mvp.model.request.MarkNotificationReadRequestModel;
import com.thirtydegreesray.openhub.mvp.presenter.base.BasePagerPresenter;
import com.thirtydegreesray.openhub.ui.adapter.base.DoubleTypesModel;
import com.thirtydegreesray.openhub.ui.fragment.NotificationsFragment;
import com.thirtydegreesray.openhub.util.LocallyReadNotificationsHelper;
import com.thirtydegreesray.openhub.util.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Inject;

import okhttp3.ResponseBody;
import retrofit2.Response;
import rx.Observable;

/**
 * Created by ThirtyDegreesRay on 2017/11/6 20:52:55
 */

public class NotificationsPresenter extends BasePagerPresenter<INotificationsContract.View>
        implements INotificationsContract.Presenter {

    @AutoAccess NotificationsFragment.NotificationsType type;
    private ArrayList<Notification> notifications;
    private ArrayList<DoubleTypesModel<Repository, Notification>> sortedNotifications;

    @Inject
    public NotificationsPresenter(DaoSession daoSession) {
        super(daoSession);
    }

    @Override
    protected void loadData() {
        loadNotifications(1, false);
    }

    @Override
    public void loadNotifications(final int page, boolean isReload) {
        android.util.Log.d("NOTIF_DEBUG", "loadNotifications page=" + page + " isReload=" + isReload
                + " type=" + type);
        mView.showLoading();
        // Deliberately never cache-first here, unlike every other page==1
        // load in this app: cacheFirstEnable defaults on with a 4-week
        // Cache-Control max-age (AppConfig.CACHE_MAX_AGE), and the
        // cache-then-network path in generalRxHttpExecute() paints that
        // stale cached response immediately before quietly replacing it with
        // the real one - fine for largely-static data (a repo's info, a
        // profile), but for a live, per-account feed like this it reads as
        // "didn't load everything" followed by items mysteriously appearing
        // a moment later. Notifications must always reflect what the server
        // has right now.
        final boolean readCacheFirst = false;

        HttpObserver<ArrayList<Notification>> httpObserver = new HttpObserver<ArrayList<Notification>>() {
            @Override
            public void onError(Throwable error) {
                mView.hideLoading();
                if (!StringUtils.isBlankList(notifications)) {
                    mView.showErrorToast(getErrorTip(error));
                } else {
                    mView.showLoadError(getErrorTip(error));
                }
            }

            @Override
            public void onSuccess(HttpResponse<ArrayList<Notification>> response) {
                mView.hideLoading();
                int rawCount = response.body().size();
                for (Notification notification : response.body()) {
                    boolean serverUnread = notification.isUnread();
                    LocallyReadNotificationsHelper.applyOverride(notification);
                    if (serverUnread != notification.isUnread()) {
                        android.util.Log.d("NOTIF_DEBUG", "OVERRIDE APPLIED id=" + notification.getId()
                                + " repo=" + notification.getRepository().getFullName()
                                + " serverSaidUnread=" + serverUnread + " updatedAt=" + notification.getUpdateAt());
                    } else if (serverUnread) {
                        android.util.Log.d("NOTIF_DEBUG", "server still unread, no override id=" + notification.getId()
                                + " repo=" + notification.getRepository().getFullName()
                                + " updatedAt=" + notification.getUpdateAt());
                    }
                }
                if (notifications == null || page == 1) {
                    notifications = response.body();
                } else {
                    notifications.addAll(response.body());
                }
                // Stop only when a page comes back genuinely empty - same
                // "empty page means done" rule every other paginated
                // presenter in this app uses (RepositoriesPresenter etc.).
                // GitHub's notifications per_page isn't a documented fixed
                // constant we can safely assume here (unlike this app's own
                // REST endpoints), so comparing the raw count against a
                // guessed page size - the previous approach - could cut
                // pagination off after page 1 whenever the real page size
                // differed from the guess, which is exactly what happened.
                if (rawCount == 0 && notifications.size() != 0) {
                    mView.setCanLoadMore(false);
                } else {
                    mView.setCanLoadMore(true);
                    sortedNotifications = sortNotifications(notifications);
                    mView.showNotifications(sortedNotifications);
                }
            }
        };

        generalRxHttpExecute(new IObservableCreator<ArrayList<Notification>>() {
            @Override
            public Observable<Response<ArrayList<Notification>>> createObservable(boolean forceNetWork) {
                if (NotificationsFragment.NotificationsType.Unread.equals(type)) {
                    return getNotificationsService().getMyNotifications(forceNetWork, false, false, page);
                } else if (NotificationsFragment.NotificationsType.Participating.equals(type)) {
                    return getNotificationsService().getMyNotifications(forceNetWork, true, true, page);
                } else if (NotificationsFragment.NotificationsType.All.equals(type)) {
                    return getNotificationsService().getMyNotifications(forceNetWork, true, false, page);
                } else {
                    return null;
                }
            }
        }, httpObserver, readCacheFirst);

    }

    @Override
    public void markNotificationAsRead(@NonNull final Notification notification) {
        // Same optimistic-then-revert-on-error shape as markAllNotificationsAsRead()/
        // markRepoNotificationsAsRead() below - this was the one remaining
        // fire-and-forget path (generalRxHttpExecute(..., null), no
        // completion callback, no override tracking at all), and it's the
        // most commonly hit one, since tapping a notification to open its
        // issue/PR is the normal way to read one - confirmed live this is
        // the repro for notifications reverting to unread after leaving and
        // returning to this screen (NotificationsActivity has no launchMode
        // override, so navigating away and back via MainActivity's drawer,
        // as opposed to the system Back button, creates a fresh instance).
        final String threadId = notification.getId();
        android.util.Log.d("NOTIF_DEBUG", "markNotificationAsRead PUT id=" + threadId
                + " repo=" + notification.getRepository().getFullName() + " updatedAt=" + notification.getUpdateAt());
        LocallyReadNotificationsHelper.markRead(notification);
        generalRxHttpExecute(getNotificationsService().markNotificationAsRead(threadId),
                new HttpSubscriber<>(new HttpObserver<ResponseBody>() {
                    @Override
                    public void onError(Throwable error) {
                        android.util.Log.d("NOTIF_DEBUG", "markNotificationAsRead FAILED id=" + threadId
                                + " error=" + error.getClass().getSimpleName() + ":" + error.getMessage());
                        LocallyReadNotificationsHelper.undoMarkRead(threadId);
                        notification.setUnread(true);
                        mView.showErrorToast(getErrorTip(error));
                        if (sortedNotifications != null) mView.showNotifications(sortedNotifications);
                    }

                    @Override
                    public void onSuccess(HttpResponse<ResponseBody> response) {
                        android.util.Log.d("NOTIF_DEBUG", "markNotificationAsRead OK id=" + threadId
                                + " httpCode=" + response.getOriResponse().code());
                    }
                }));
    }

    @Override
    public void markAllNotificationsAsRead() {
        // Optimistic - flips every row before the network call even starts,
        // so the UI feels instant - but reverted in onError() below rather
        // than fire-and-forget, so a failed request (the previous
        // MarkNotificationReadRequestModel bug, or a genuine network error)
        // is visible immediately instead of silently un-doing itself the
        // next time this screen is reloaded from a fresh Activity instance.
        final ArrayList<Notification> markedByThisCall = new ArrayList<>();
        for(DoubleTypesModel<Repository, Notification> model : sortedNotifications){
            if(model.getM2() != null && model.getM2().isUnread()){
                model.getM2().setUnread(false);
                LocallyReadNotificationsHelper.markRead(model.getM2());
                markedByThisCall.add(model.getM2());
            }
        }
        mView.showNotifications(sortedNotifications);

        android.util.Log.d("NOTIF_DEBUG", "markAllNotificationsAsRead PATCH, marking " + markedByThisCall.size() + " ids");
        generalRxHttpExecute(getNotificationsService().markAllNotificationsAsRead(
                MarkNotificationReadRequestModel.newInstance()), new HttpSubscriber<>(
                new HttpObserver<ResponseBody>() {
                    @Override
                    public void onError(Throwable error) {
                        android.util.Log.d("NOTIF_DEBUG", "markAllNotificationsAsRead FAILED error="
                                + error.getClass().getSimpleName() + ":" + error.getMessage());
                        for (Notification notification : markedByThisCall) {
                            notification.setUnread(true);
                            LocallyReadNotificationsHelper.undoMarkRead(notification.getId());
                        }
                        mView.showErrorToast(getErrorTip(error));
                        mView.showNotifications(sortedNotifications);
                    }

                    @Override
                    public void onSuccess(HttpResponse<ResponseBody> response) {
                        android.util.Log.d("NOTIF_DEBUG", "markAllNotificationsAsRead OK httpCode="
                                + response.getOriResponse().code());
                    }
                }));
    }

    @Override
    public boolean isNotificationsAllRead() {
        if(notifications == null){
            return true;
        }
        for(DoubleTypesModel<Repository, Notification> model : sortedNotifications){
            if(model.getM2() != null && model.getM2().isUnread()){
                return false;
            }
        }
        return true;
    }

    @Override
    public void markRepoNotificationsAsRead(@NonNull Repository repository) {
        // Same optimistic-then-revert-on-error shape as markAllNotificationsAsRead()
        // above, for the same reason.
        final ArrayList<Notification> markedByThisCall = new ArrayList<>();
        for(DoubleTypesModel<Repository, Notification> model : sortedNotifications){
            if(model.getM1() != null && model.getM1().getId() == repository.getId()){
                model.setAllRead(true);
            } else if(model.getM2() != null && model.getM2().getRepository().getId() == repository.getId()
                    && model.getM2().isUnread()){
                model.getM2().setUnread(false);
                LocallyReadNotificationsHelper.markRead(model.getM2());
                markedByThisCall.add(model.getM2());
            }
        }
        mView.showNotifications(sortedNotifications);

        android.util.Log.d("NOTIF_DEBUG", "markRepoNotificationsAsRead PUT repo=" + repository.getFullName()
                + ", marking " + markedByThisCall.size() + " ids");
        generalRxHttpExecute(getNotificationsService().markRepoNotificationsAsRead(
                MarkNotificationReadRequestModel.newInstance(),
                repository.getOwner().getLogin(), repository.getName()), new HttpSubscriber<>(
                new HttpObserver<ResponseBody>() {
                    @Override
                    public void onError(Throwable error) {
                        android.util.Log.d("NOTIF_DEBUG", "markRepoNotificationsAsRead FAILED repo="
                                + repository.getFullName() + " error=" + error.getClass().getSimpleName()
                                + ":" + error.getMessage());
                        for (Notification notification : markedByThisCall) {
                            notification.setUnread(true);
                            LocallyReadNotificationsHelper.undoMarkRead(notification.getId());
                        }
                        for(DoubleTypesModel<Repository, Notification> model : sortedNotifications){
                            if(model.getM1() != null && model.getM1().getId() == repository.getId()){
                                model.setAllRead(false);
                            }
                        }
                        mView.showErrorToast(getErrorTip(error));
                        mView.showNotifications(sortedNotifications);
                    }

                    @Override
                    public void onSuccess(HttpResponse<ResponseBody> response) {
                        android.util.Log.d("NOTIF_DEBUG", "markRepoNotificationsAsRead OK repo="
                                + repository.getFullName() + " httpCode=" + response.getOriResponse().code());
                    }
                }));
    }

    @Override
    public void removeRepoNotifications(@NonNull Repository repository) {
        android.util.Log.d("NOTIF_DEBUG", "removeRepoNotifications (client-side only, no server call) repo="
                + repository.getFullName());
        Iterator<DoubleTypesModel<Repository, Notification>> iterator = sortedNotifications.iterator();
        while(iterator.hasNext()){
            DoubleTypesModel<Repository, Notification> model = iterator.next();
            if(model.getM1() != null && model.getM1().getId() == repository.getId()){
                iterator.remove();
            } else if(model.getM2() != null && model.getM2().getRepository().getId() == repository.getId()){
                iterator.remove();
            }
        }
        mView.showNotifications(sortedNotifications);
    }

    private ArrayList<DoubleTypesModel<Repository, Notification>> sortNotifications(
            ArrayList<Notification> notifications) {

        ArrayList<DoubleTypesModel<Repository, Notification>> sortedList = new ArrayList<>();
        Map<String, ArrayList<Notification>> sortedMap = new LinkedHashMap<>();
        for (Notification notification : notifications) {
            ArrayList<Notification> list = sortedMap.get(notification.getRepository().getFullName());
            if (list == null) {
                list = new ArrayList<>();
                sortedMap.put(notification.getRepository().getFullName(), list);
            }
            list.add(notification);
        }

        Iterator<String> iterator = sortedMap.keySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            ArrayList<Notification> list = sortedMap.get(key);
            DoubleTypesModel<Repository, Notification> header =
                    new DoubleTypesModel<Repository, Notification>(list.get(0).getRepository(), null);
            // Derive from the actual per-notification read state every time,
            // rather than defaulting to false - this list gets rebuilt from
            // scratch on every load-more/reload (fresh DoubleTypesModel
            // instances), which was silently discarding markRepoNotificationsAsRead()'s
            // one-off mutation of the old header instance and made every
            // repo checkmark revert to "unread" as soon as the next page
            // loaded, even though the notifications themselves were still
            // correctly marked read.
            boolean allRead = true;
            for (Notification notification : list) {
                if (notification.isUnread()) {
                    allRead = false;
                    break;
                }
            }
            header.setAllRead(allRead);
            sortedList.add(header);
            for(Notification notification : list){
                sortedList.add(new DoubleTypesModel<Repository, Notification>(null, notification));
            }
        }
        return sortedList;
    }

    public NotificationsFragment.NotificationsType getType() {
        return type;
    }
}
