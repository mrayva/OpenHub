

package com.thirtydegreesray.openhub.mvp.presenter;

import androidx.annotation.NonNull;

import com.thirtydegreesray.dataautoaccess.annotation.AutoAccess;
import com.thirtydegreesray.openhub.dao.DaoSession;
import com.thirtydegreesray.openhub.http.core.HttpObserver;
import com.thirtydegreesray.openhub.http.core.HttpResponse;
import com.thirtydegreesray.openhub.mvp.contract.INotificationsContract;
import com.thirtydegreesray.openhub.mvp.model.Notification;
import com.thirtydegreesray.openhub.mvp.model.Repository;
import com.thirtydegreesray.openhub.mvp.model.request.MarkNotificationReadRequestModel;
import com.thirtydegreesray.openhub.mvp.presenter.base.BasePagerPresenter;
import com.thirtydegreesray.openhub.ui.adapter.base.DoubleTypesModel;
import com.thirtydegreesray.openhub.ui.fragment.NotificationsFragment;
import com.thirtydegreesray.openhub.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

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
    // Marking read is a fire-and-forget PATCH (generalRxHttpExecute(..., null)
    // below - no completion callback), and readCacheFirst's cache-then-network
    // double fetch on the very first load can deliver a second, fresher
    // onSuccess() for the same page after the user has already acted on the
    // cached one. Without this, that second response's full list replace
    // (page == 1 branch below) silently reverted every mark-as-read made in
    // between back to unread - remembering the ids here and reapplying them
    // onto any freshly fetched batch closes that window.
    private final Set<String> locallyReadIds = new HashSet<>();

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
                    if (locallyReadIds.contains(notification.getId())) {
                        notification.setUnread(false);
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
    public void markNotificationAsRead(String threadId) {
        generalRxHttpExecute(getNotificationsService().markNotificationAsRead(threadId), null);
        locallyReadIds.add(threadId);
    }

    @Override
    public void markAllNotificationsAsRead() {
        generalRxHttpExecute(getNotificationsService().markAllNotificationsAsRead(
                MarkNotificationReadRequestModel.newInstance()), null);

        for(DoubleTypesModel<Repository, Notification> model : sortedNotifications){
            if(model.getM2() != null){
                model.getM2().setUnread(false);
                locallyReadIds.add(model.getM2().getId());
            }
        }
        mView.showNotifications(sortedNotifications);
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
        generalRxHttpExecute(getNotificationsService().markRepoNotificationsAsRead(
                MarkNotificationReadRequestModel.newInstance(),
                repository.getOwner().getLogin(), repository.getName()), null);

        for(DoubleTypesModel<Repository, Notification> model : sortedNotifications){
            if(model.getM1() != null && model.getM1().getId() == repository.getId()){
                model.setAllRead(true);
            } else if(model.getM2() != null && model.getM2().getRepository().getId() == repository.getId()){
                model.getM2().setUnread(false);
                locallyReadIds.add(model.getM2().getId());
            }
        }
        mView.showNotifications(sortedNotifications);
    }

    @Override
    public void removeRepoNotifications(@NonNull Repository repository) {
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
