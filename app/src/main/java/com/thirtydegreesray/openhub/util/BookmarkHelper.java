package com.thirtydegreesray.openhub.util;

import androidx.annotation.NonNull;

import com.thirtydegreesray.openhub.AppApplication;
import com.thirtydegreesray.openhub.dao.Bookmark;
import com.thirtydegreesray.openhub.dao.BookmarkDao;
import com.thirtydegreesray.openhub.dao.DaoSession;
import com.thirtydegreesray.openhub.dao.LocalRepo;
import com.thirtydegreesray.openhub.dao.LocalUser;
import com.thirtydegreesray.openhub.mvp.model.BookmarkBackup;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import rx.Observable;
import rx.schedulers.Schedulers;

/**
 * Export/import for BookmarksFragment's bookmark list (BookmarkExport-style,
 * mirroring IgnoredRepoHelper's export/import for the Ignore List).
 *
 * A live bookmark isn't one row: it's a Bookmark join row (type + userId or
 * repoId + markTime) plus a LocalRepo or LocalUser row for the actual
 * display data - LocalRepo/LocalUser are a shared cache also written by the
 * unrelated "recently viewed" trace feature, not something bookmarking owns
 * exclusively. Exporting/importing the join table alone would produce
 * dangling references after a data wipe (BookmarkPresenter.loadBookmarks()
 * does an inner join and simply drops any Bookmark row whose LocalRepo is
 * missing - see its "if (localRepo != null)" guard), so both getAllForExport()
 * and importAll() work with the flattened BookmarkBackup DTO instead,
 * reading/writing both tables together.
 */
public class BookmarkHelper {

    private static DaoSession getDaoSession() {
        return AppApplication.get().getAppComponent().getDaoSession();
    }

    public static Observable<ArrayList<BookmarkBackup>> getAllForExport() {
        return Observable.fromCallable(() -> {
            DaoSession daoSession = getDaoSession();
            List<Bookmark> bookmarks = daoSession.getBookmarkDao().queryBuilder()
                    .orderDesc(BookmarkDao.Properties.MarkTime)
                    .list();
            ArrayList<BookmarkBackup> backups = new ArrayList<>();
            for (Bookmark bookmark : bookmarks) {
                BookmarkBackup backup = new BookmarkBackup();
                backup.setType(bookmark.getType());
                backup.setMarkTime(bookmark.getMarkTime());
                if ("user".equals(bookmark.getType())) {
                    LocalUser localUser = daoSession.getLocalUserDao().load(bookmark.getUserId());
                    if (localUser == null) continue;
                    backup.setUserLogin(localUser.getLogin());
                    backup.setUserName(localUser.getName());
                    backup.setUserAvatarUrl(localUser.getAvatarUrl());
                    backup.setUserFollowers(localUser.getFollowers());
                    backup.setUserFollowing(localUser.getFollowing());
                } else {
                    LocalRepo localRepo = daoSession.getLocalRepoDao().load(bookmark.getRepoId());
                    if (localRepo == null) continue;
                    backup.setRepoId(localRepo.getId());
                    backup.setRepoName(localRepo.getName());
                    backup.setRepoDescription(localRepo.getDescription());
                    backup.setRepoLanguage(localRepo.getLanguage());
                    backup.setRepoStargazersCount(localRepo.getStargazersCount());
                    backup.setRepoWatchersCount(localRepo.getWatchersCount());
                    backup.setRepoForksCount(localRepo.getForksCount());
                    backup.setRepoFork(localRepo.getFork());
                    backup.setRepoOwnerLogin(localRepo.getOwnerLogin());
                    backup.setRepoOwnerAvatarUrl(localRepo.getOwnerAvatarUrl());
                }
                backups.add(backup);
            }
            return backups;
        }).subscribeOn(Schedulers.io());
    }

    public static Observable<Integer> importAll(@NonNull List<BookmarkBackup> backups) {
        return Observable.fromCallable(() -> {
            DaoSession daoSession = getDaoSession();
            int count = 0;
            for (BookmarkBackup backup : backups) {
                if (backup.getType() == null) continue;
                if ("user".equals(backup.getType())) {
                    if (backup.getUserLogin() == null) continue;
                    LocalUser localUser = new LocalUser(backup.getUserLogin());
                    localUser.setName(backup.getUserName());
                    localUser.setAvatarUrl(backup.getUserAvatarUrl());
                    localUser.setFollowers(backup.getUserFollowers());
                    localUser.setFollowing(backup.getUserFollowing());
                    daoSession.getLocalUserDao().insertOrReplace(localUser);

                    Bookmark bookmark = daoSession.getBookmarkDao().queryBuilder()
                            .where(BookmarkDao.Properties.UserId.eq(backup.getUserLogin()))
                            .unique();
                    if (bookmark == null) {
                        bookmark = new Bookmark(UUID.randomUUID().toString());
                        bookmark.setType("user");
                        bookmark.setUserId(backup.getUserLogin());
                        bookmark.setMarkTime(backup.getMarkTime());
                        daoSession.getBookmarkDao().insert(bookmark);
                    }
                } else {
                    if (backup.getRepoId() == null) continue;
                    LocalRepo localRepo = new LocalRepo(backup.getRepoId());
                    localRepo.setName(backup.getRepoName());
                    localRepo.setDescription(backup.getRepoDescription());
                    localRepo.setLanguage(backup.getRepoLanguage());
                    localRepo.setStargazersCount(backup.getRepoStargazersCount());
                    localRepo.setWatchersCount(backup.getRepoWatchersCount());
                    localRepo.setForksCount(backup.getRepoForksCount());
                    localRepo.setFork(backup.getRepoFork());
                    localRepo.setOwnerLogin(backup.getRepoOwnerLogin());
                    localRepo.setOwnerAvatarUrl(backup.getRepoOwnerAvatarUrl());
                    daoSession.getLocalRepoDao().insertOrReplace(localRepo);

                    Bookmark bookmark = daoSession.getBookmarkDao().queryBuilder()
                            .where(BookmarkDao.Properties.RepoId.eq(backup.getRepoId()))
                            .unique();
                    if (bookmark == null) {
                        bookmark = new Bookmark(UUID.randomUUID().toString());
                        bookmark.setType("repo");
                        bookmark.setRepoId(backup.getRepoId());
                        bookmark.setMarkTime(backup.getMarkTime());
                        daoSession.getBookmarkDao().insert(bookmark);
                    }
                }
                count++;
            }
            return count;
        }).subscribeOn(Schedulers.io());
    }

}
