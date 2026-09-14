

package com.thirtydegreesray.openhub.mvp.presenter;

import com.thirtydegreesray.dataautoaccess.annotation.AutoAccess;
import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.common.Event;
import com.thirtydegreesray.openhub.dao.Bookmark;
import com.thirtydegreesray.openhub.dao.BookmarkDao;
import com.thirtydegreesray.openhub.dao.DaoSession;
import com.thirtydegreesray.openhub.dao.LocalUser;
import com.thirtydegreesray.openhub.dao.Trace;
import com.thirtydegreesray.openhub.dao.TraceDao;
import com.thirtydegreesray.openhub.http.core.HttpObserver;
import com.thirtydegreesray.openhub.http.core.HttpResponse;
import com.thirtydegreesray.openhub.http.error.HttpPageNoFoundError;
import com.thirtydegreesray.openhub.mvp.contract.IUserListContract;
import com.thirtydegreesray.openhub.mvp.model.SearchModel;
import com.thirtydegreesray.openhub.mvp.model.SearchResult;
import com.thirtydegreesray.openhub.mvp.model.User;
import com.thirtydegreesray.openhub.mvp.presenter.base.BasePagerPresenter;
import com.thirtydegreesray.openhub.ui.fragment.UserListFragment;
import com.thirtydegreesray.openhub.util.StringUtils;

import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import retrofit2.Response;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by ThirtyDegreesRay on 2017/8/16 17:38:43
 */

public class UserListPresenter extends BasePagerPresenter<IUserListContract.View>
        implements IUserListContract.Presenter {

    @AutoAccess UserListFragment.UserListType type;
    @AutoAccess String user;
    @AutoAccess String repo;

    @AutoAccess SearchModel searchModel;

    private ArrayList<User> users;

    @Inject
    public UserListPresenter(DaoSession daoSession) {
        super(daoSession);
    }

    @Override
    public void onViewInitialized() {
        super.onViewInitialized();
        if (type.equals(UserListFragment.UserListType.SEARCH)) {
            setEventSubscriber(true);
        }
    }

    @Override
    protected void loadData() {
        if(UserListFragment.UserListType.SEARCH.equals(type)){
            if(searchModel != null) searchUsers(1);
        } else if(UserListFragment.UserListType.TRACE.equals(type)){
            loadTrace(1);
        } else if(UserListFragment.UserListType.BOOKMARK.equals(type)){
            loadBookmarks(1);
        } else {
            loadUsers(1, false);
        }
    }

    @Override
    public void loadUsers(final int page, final boolean isReload) {
        if (type.equals(UserListFragment.UserListType.SEARCH)) {
            searchUsers(page);
            return;
        }
        if(UserListFragment.UserListType.TRACE.equals(type)){
            loadTrace(page);
            return;
        }
        if(UserListFragment.UserListType.BOOKMARK.equals(type)){
            loadBookmarks(page);
            return;
        }
        mView.showLoading();
        final boolean readCacheFirst = page == 1 && !isReload;
        HttpObserver<ArrayList<User>> httpObserver =
                new HttpObserver<ArrayList<User>>() {
                    @Override
                    public void onError(Throwable error) {
                        mView.hideLoading();
                        handleError(error);
                    }

                    @Override
                    public void onSuccess(HttpResponse<ArrayList<User>> response) {
                        mView.hideLoading();
                        if (isReload || users == null || readCacheFirst) {
                            users = response.body();
                        } else {
                            users.addAll(response.body());
                        }
                        if(response.body().size() == 0 && users.size() != 0){
                            mView.setCanLoadMore(false);
                        } else {
                            mView.showUsers(users);
                        }
                    }
                };
        generalRxHttpExecute(new IObservableCreator<ArrayList<User>>() {
            @Override
            public Observable<Response<ArrayList<User>>> createObservable(boolean forceNetWork) {
                if (type.equals(UserListFragment.UserListType.STARGAZERS)) {
                    return getRepoService().getStargazers(forceNetWork, user, repo, page);
                } else if (type.equals(UserListFragment.UserListType.WATCHERS)) {
                    return getRepoService().getWatchers(forceNetWork, user, repo, page);
                } else if (type.equals(UserListFragment.UserListType.FOLLOWERS)) {
                    return getUserService().getFollowers(forceNetWork, user, page);
                } else if (type.equals(UserListFragment.UserListType.FOLLOWING)) {
                    return getUserService().getFollowing(forceNetWork, user, page);
                } else if (type.equals(UserListFragment.UserListType.ORG_MEMBERS)) {
                    return getUserService().getOrgMembers(forceNetWork, user, page);
                } else {
                    throw new IllegalArgumentException(type.name());
                }
            }
        }, httpObserver, readCacheFirst);
    }

    private void searchUsers(final int page) {
        mView.showLoading();
        HttpObserver<SearchResult<User>> httpObserver =
                new HttpObserver<SearchResult<User>>() {
                    @Override
                    public void onError(Throwable error) {
                        mView.hideLoading();
                        handleError(error);
                    }

                    @Override
                    public void onSuccess(HttpResponse<SearchResult<User>> response) {
                        mView.hideLoading();
                        if (users == null || page == 1) {
                            users = response.body().getItems();
                        } else {
                            users.addAll(response.body().getItems());
                        }
                        if(response.body().getItems().size() == 0 && users.size() != 0){
                            mView.setCanLoadMore(false);
                        } else {
                            mView.showUsers(users);
                        }
                    }
                };
        generalRxHttpExecute(new IObservableCreator<SearchResult<User>>() {
            @Override
            public Observable<Response<SearchResult<User>>> createObservable(boolean forceNetWork) {
                return getSearchService().searchUsers(searchModel.getQuery(), searchModel.getSort(),
                        searchModel.getOrder(), page);
            }
        }, httpObserver);
    }

    @Subscribe
    public void onSearchEvent(Event.SearchEvent searchEvent) {
        if (!searchEvent.searchModel.getType().equals(SearchModel.SearchType.User)) return;
        setLoaded(false);
        this.searchModel = searchEvent.searchModel;
        prepareLoadData();
    }

    private void handleError(Throwable error){
        if(!StringUtils.isBlankList(users)){
            mView.showErrorToast(getErrorTip(error));
        } else if(error instanceof HttpPageNoFoundError){
            if (isStargazersOrWatchers()) {
                // GitHub restricted these two list endpoints in mid-2026 to
                // the repo's owner/collaborators only - everyone else gets a
                // 404 regardless of how valid their token otherwise is (see
                // https://github.blog/changelog/2026-06-30-upcoming-access-restrictions-to-public-api-endpoints-and-ui-views/).
                // Not fixable client-side, so say so instead of silently
                // showing an empty list that looks like "zero stargazers".
                mView.showLoadError(getString(R.string.stargazers_watchers_restricted));
            } else {
                mView.showUsers(new ArrayList<User>());
            }
        } else {
            mView.showLoadError(getErrorTip(error));
        }
    }

    private boolean isStargazersOrWatchers() {
        return UserListFragment.UserListType.STARGAZERS.equals(type)
                || UserListFragment.UserListType.WATCHERS.equals(type);
    }

    private void loadTrace(final int page){
        mView.showLoading();
        Observable.fromCallable(() -> {
            // See RepositoriesPresenter.loadTrace()'s comment: TraceUserDao is a
            // pre-v4-schema table nothing writes to anymore (or, on a DB that's
            // gone through that migration, doesn't even exist) - trace tracking
            // now lives in the unified Trace table (repoId/userId + type) plus
            // LocalUser for the actual profile data.
            List<Trace> traces = daoSession.getTraceDao().queryBuilder()
                    .where(TraceDao.Properties.Type.eq("user"))
                    .orderDesc(TraceDao.Properties.LatestTime)
                    .offset((page - 1) * 30)
                    .limit(30)
                    .list();
            ArrayList<User> queryUsers = new ArrayList<>();
            for(Trace trace : traces){
                LocalUser localUser = daoSession.getLocalUserDao().load(trace.getUserId());
                if (localUser != null) {
                    queryUsers.add(User.generateFromLocalUser(localUser));
                }
            }
            return queryUsers;
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(queryUsers -> {
            if (mView == null) return;
            showQueryUsers(queryUsers, page);
        }, error -> {
            if (mView == null) return;
            mView.hideLoading();
            mView.showLoadError(getErrorTip(error));
        });
    }

    private void loadBookmarks(final int page){
        mView.showLoading();
        Observable.fromCallable(() -> {
            // Same stale-table bug as loadTrace() (see its comment): BookMarkUserDao
            // is a pre-v4-schema table nothing writes to anymore - bookmarking now
            // writes into the unified Bookmark table (repoId/userId + type) plus
            // LocalRepo/LocalUser for the actual repo/user data.
            List<Bookmark> bookmarks = daoSession.getBookmarkDao().queryBuilder()
                    .where(BookmarkDao.Properties.Type.eq("user"))
                    .orderDesc(BookmarkDao.Properties.MarkTime)
                    .offset((page - 1) * 30)
                    .limit(30)
                    .list();
            ArrayList<User> queryUsers = new ArrayList<>();
            for(Bookmark bookmark : bookmarks){
                LocalUser localUser = daoSession.getLocalUserDao().load(bookmark.getUserId());
                if (localUser != null) {
                    queryUsers.add(User.generateFromLocalUser(localUser));
                }
            }
            return queryUsers;
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(queryUsers -> {
            if (mView == null) return;
            showQueryUsers(queryUsers, page);
        }, error -> {
            if (mView == null) return;
            mView.hideLoading();
            mView.showLoadError(getErrorTip(error));
        });
    }

    private void showQueryUsers(ArrayList<User> queryUsers, int page){
        if(users == null || page == 1){
            users = queryUsers;
        } else {
            users.addAll(queryUsers);
        }

        mView.showUsers(users);
        mView.hideLoading();
    }

}
