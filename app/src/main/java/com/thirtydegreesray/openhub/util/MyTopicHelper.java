package com.thirtydegreesray.openhub.util;

import androidx.annotation.NonNull;

import com.thirtydegreesray.openhub.AppApplication;
import com.thirtydegreesray.openhub.dao.DaoSession;
import com.thirtydegreesray.openhub.dao.MyTopic;
import com.thirtydegreesray.openhub.dao.MyTopicDao;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import rx.Observable;
import rx.schedulers.Schedulers;

/**
 * My Topics membership, keyed by slug - mirrors IgnoredRepoHelper's pattern
 * (in-memory synchronized cache, preloaded once at startup, mutated
 * synchronously in-memory + asynchronously to DB) so RepoTopicsFragment's
 * swipe-to-add and TopicRepositoriesActivity's add/remove menu action can
 * check/mutate My Topics membership without going through
 * TopicsEditorPresenter, which is scoped to the My Topics editor screen's
 * own lifecycle and in-memory ordered list.
 */
public class MyTopicHelper {

    private static final Set<String> slugs = Collections.synchronizedSet(new HashSet<>());
    private static volatile boolean preloadStarted = false;

    private static DaoSession getDaoSession() {
        return AppApplication.get().getAppComponent().getDaoSession();
    }

    public static void preload() {
        if (preloadStarted) return;
        preloadStarted = true;
        Observable.fromCallable(() -> getDaoSession().getMyTopicDao().loadAll())
                .subscribeOn(Schedulers.io())
                .subscribe(all -> {
                    for (MyTopic topic : all) {
                        slugs.add(topic.getSlug());
                    }
                }, Throwable::printStackTrace);
    }

    public static boolean isMyTopic(String slug) {
        return slug != null && slugs.contains(normalize(slug));
    }

    private static String normalize(@NonNull String slug) {
        return slug.trim().toLowerCase();
    }

    /**
     * @return false if slug is blank or already present (no-op); true if added.
     */
    public static boolean add(@NonNull String slug) {
        final String normalized = normalize(slug);
        if (StringUtils.isBlank(normalized) || !slugs.add(normalized)) return false;

        final DaoSession daoSession = getDaoSession();
        daoSession.rxTx().run(() -> {
            MyTopicDao dao = daoSession.getMyTopicDao();
            int maxOrder = 0;
            for (MyTopic topic : dao.loadAll()) {
                if (topic.getOrder() > maxOrder) maxOrder = topic.getOrder();
            }
            dao.insertOrReplace(new MyTopic(normalized, maxOrder + 1, true));
        }).subscribe();
        return true;
    }

    public static void remove(@NonNull String slug) {
        final String normalized = normalize(slug);
        slugs.remove(normalized);
        final DaoSession daoSession = getDaoSession();
        daoSession.rxTx().run(() -> daoSession.getMyTopicDao().deleteByKey(normalized)).subscribe();
    }

}
