package com.thirtydegreesray.openhub.util;

import com.thirtydegreesray.openhub.AppConfig;
import com.thirtydegreesray.openhub.AppData;
import com.thirtydegreesray.openhub.http.RepoService;
import com.thirtydegreesray.openhub.http.core.AppRetrofit;
import com.thirtydegreesray.openhub.mvp.model.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import rx.Observable;
import rx.schedulers.Schedulers;

/**
 * Cross-screen "am I watching this repo" lookup, backing a small badge on
 * every repo row (RepositoriesAdapter) outside the Notifications list (which
 * isn't a repo list at all). GitHub's repo list/search responses never
 * include the caller's own subscription state - the only way to know is
 * GET /user/subscriptions (paginated) or a per-repo GET
 * /user/subscriptions/{owner}/{repo} check, and doing the latter once per
 * visible row would be one HTTP call per row. So instead this fetches the
 * full watched-repos list once per login session and caches fullNames in
 * memory - same pattern as IgnoredRepoHelper, just sourced from GitHub's API
 * instead of a local DB.
 */
public class WatchedRepoHelper {

    // GitHub's default per_page for user/subscriptions - used only to detect
    // the last page (a short page means no more pages follow).
    private static final int PAGE_SIZE = 30;

    private static final Set<String> watchedFullNames = Collections.synchronizedSet(new HashSet<>());
    private static volatile boolean loaded = false;
    private static volatile boolean refreshing = false;

    public static boolean isWatched(String fullName) {
        return fullName != null && watchedFullNames.contains(fullName);
    }

    /** Cheap to call from every repo-list screen - only fetches once per login session. */
    public static void refreshIfNeeded() {
        if (loaded || refreshing) return;
        refresh();
    }

    /** Re-fetches from scratch - called after logout/account switch so a stale cache isn't reused. */
    public static void refresh() {
        String token = AppData.INSTANCE.getAccessToken();
        if (StringUtils.isBlank(token) || refreshing) return;
        refreshing = true;
        RepoService repoService = AppRetrofit.INSTANCE
                .getRetrofit(AppConfig.GITHUB_API_BASE_URL, token, true)
                .create(RepoService.class);
        fetchPage(repoService, 1, new HashSet<>());
    }

    private static void fetchPage(RepoService repoService, int page, Set<String> accumulated) {
        repoService.getWatchedRepos(true, page)
                .subscribeOn(Schedulers.io())
                .subscribe(response -> {
                    ArrayList<Repository> repos = response.isSuccessful() ? response.body() : null;
                    if (repos != null) {
                        for (Repository repo : repos) {
                            accumulated.add(repo.getFullName());
                        }
                    }
                    if (repos != null && repos.size() == PAGE_SIZE) {
                        fetchPage(repoService, page + 1, accumulated);
                    } else {
                        watchedFullNames.clear();
                        watchedFullNames.addAll(accumulated);
                        loaded = true;
                        refreshing = false;
                    }
                }, error -> refreshing = false); // loaded stays as-is; next refreshIfNeeded() retries
    }

    /** Optimistic local update from RepositoryActivity's own Watch/Unwatch toggle. */
    public static void setWatched(String fullName, boolean watched) {
        if (fullName == null) return;
        if (watched) {
            watchedFullNames.add(fullName);
        } else {
            watchedFullNames.remove(fullName);
        }
    }

    /** Called on logout/account switch (MainPresenter) - the cache is per-account. */
    public static void reset() {
        watchedFullNames.clear();
        loaded = false;
        refreshing = false;
    }

}
