package com.thirtydegreesray.openhub.util;

import androidx.annotation.NonNull;

import com.thirtydegreesray.openhub.AppApplication;
import com.thirtydegreesray.openhub.dao.DaoSession;
import com.thirtydegreesray.openhub.dao.IgnoredRepo;
import com.thirtydegreesray.openhub.mvp.model.Repository;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import rx.Observable;
import rx.schedulers.Schedulers;

/**
 * Ignore list for repos (Trending/Created filtering), keyed by fullName
 * ("owner/repo") rather than Repository.getId(): Trending's scraped repos
 * never get a numeric id set at all (RepositoriesPresenter.
 * parseTrendingRepositoryData() has no id to scrape from GitHub's HTML), so
 * every Trending Repository's id defaults to 0 - using that as a key would
 * conflate every ignored Trending repo into one entry. fullName is reliably
 * present from both Trending (HTML) and Created (search API).
 *
 * Checked once per row while binding a page of results, so this keeps every
 * ignored fullName cached in memory (trivial size even at tens of thousands
 * of entries) instead of a DB round trip per row.
 *
 * The cache is a single mutable synchronized Set, always non-null, so
 * isIgnored()/ignore()/unignore() never block on DB access. preload() (called
 * once from AppApplication.onCreate()) merges in whatever was already
 * persisted from a background thread - merge rather than replace, so a swipe
 * that happens before preload() finishes is never clobbered by it landing
 * afterward.
 */
public class IgnoredRepoHelper {

    private static final Set<String> ignoredFullNames = Collections.synchronizedSet(new HashSet<>());
    private static volatile boolean preloadStarted = false;

    private static DaoSession getDaoSession() {
        return AppApplication.get().getAppComponent().getDaoSession();
    }

    public static void preload() {
        if (preloadStarted) return;
        preloadStarted = true;
        Observable.fromCallable(() -> getDaoSession().getIgnoredRepoDao().loadAll())
                .subscribeOn(Schedulers.io())
                .subscribe(all -> {
                    for (IgnoredRepo ignoredRepo : all) {
                        ignoredFullNames.add(ignoredRepo.getFullName());
                    }
                }, Throwable::printStackTrace);
    }

    public static boolean isIgnored(String fullName) {
        return fullName != null && ignoredFullNames.contains(fullName);
    }

    public static void ignore(@NonNull Repository repository) {
        final String fullName = repository.getFullName();
        ignoredFullNames.add(fullName);

        IgnoredRepo ignoredRepo = new IgnoredRepo(fullName);
        ignoredRepo.setName(repository.getName());
        ignoredRepo.setDescription(repository.getDescription());
        ignoredRepo.setLanguage(repository.getLanguage());
        ignoredRepo.setStargazersCount(repository.getStargazersCount());
        ignoredRepo.setForksCount(repository.getForksCount());
        if (repository.getOwner() != null) {
            ignoredRepo.setOwnerLogin(repository.getOwner().getLogin());
            ignoredRepo.setOwnerAvatarUrl(repository.getOwner().getAvatarUrl());
        }
        ignoredRepo.setIgnoredAt(new Date());

        final DaoSession daoSession = getDaoSession();
        daoSession.rxTx().run(() -> daoSession.getIgnoredRepoDao().insertOrReplace(ignoredRepo)).subscribe();
    }

    public static void unignore(String fullName) {
        ignoredFullNames.remove(fullName);
        final DaoSession daoSession = getDaoSession();
        daoSession.rxTx().run(() -> daoSession.getIgnoredRepoDao().deleteByKey(fullName)).subscribe();
    }

}
