

package com.thirtydegreesray.openhub.mvp.presenter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.thirtydegreesray.dataautoaccess.annotation.AutoAccess;
import com.thirtydegreesray.openhub.AppConfig;
import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.R2;
import com.thirtydegreesray.openhub.common.Event;
import com.thirtydegreesray.openhub.dao.Bookmark;
import com.thirtydegreesray.openhub.dao.BookmarkDao;
import com.thirtydegreesray.openhub.dao.DaoSession;
import com.thirtydegreesray.openhub.dao.IgnoredRepo;
import com.thirtydegreesray.openhub.dao.IgnoredRepoDao;
import com.thirtydegreesray.openhub.dao.LocalRepo;
import com.thirtydegreesray.openhub.dao.Trace;
import com.thirtydegreesray.openhub.dao.TraceDao;
import com.thirtydegreesray.openhub.http.core.HttpObserver;
import com.thirtydegreesray.openhub.http.core.HttpResponse;
import com.thirtydegreesray.openhub.http.error.HttpPageNoFoundError;
import com.thirtydegreesray.openhub.mvp.contract.IRepositoriesContract;
import com.thirtydegreesray.openhub.mvp.model.Collection;
import com.thirtydegreesray.openhub.mvp.model.Repository;
import com.thirtydegreesray.openhub.mvp.model.SearchModel;
import com.thirtydegreesray.openhub.mvp.model.SearchResult;
import com.thirtydegreesray.openhub.mvp.model.Topic;
import com.thirtydegreesray.openhub.mvp.model.TrendingLanguage;
import com.thirtydegreesray.openhub.mvp.model.User;
import com.thirtydegreesray.openhub.mvp.model.filter.RepositoriesFilter;
import com.thirtydegreesray.openhub.mvp.model.filter.TrendingSince;
import com.thirtydegreesray.openhub.mvp.presenter.base.BasePagerPresenter;
import com.thirtydegreesray.openhub.ui.fragment.RepositoriesFragment;
import com.thirtydegreesray.openhub.util.IgnoredRepoHelper;
import com.thirtydegreesray.openhub.util.PrefUtils;
import com.thirtydegreesray.openhub.util.StringUtils;

import org.greenrobot.eventbus.Subscribe;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import okhttp3.ResponseBody;
import retrofit2.Response;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created on 2017/7/18.
 *
 * @author ThirtyDegreesRay
 */

public class RepositoriesPresenter extends BasePagerPresenter<IRepositoriesContract.View>
        implements IRepositoriesContract.Presenter {

    private ArrayList<Repository> repos;

    @AutoAccess RepositoriesFragment.RepositoriesType type;
    @AutoAccess String user;
    @AutoAccess String repo;

    @AutoAccess SearchModel searchModel;
    @AutoAccess TrendingSince since;

    @AutoAccess RepositoriesFilter filter;

    @AutoAccess TrendingLanguage language;

    @AutoAccess Collection collection;
    @AutoAccess Topic topic;

    @AutoAccess ArrayList<String> topicSlugs;
    @AutoAccess String sort;

    // FORKS only - one of GitHub's own sort values for repos/{owner}/{repo}/forks
    // ("newest"/"oldest"/"stargazers"/"watchers"). Kept separate from the
    // "sort" field above, which already means something different (a
    // client-side re-sort key for TOPICS_SEARCH results).
    private String forksSort = "newest";

    /**
     * True only for Trending and Created-tab SEARCH fragments (set by
     * RepositoriesFragment's factories) - gates ignore-list filtering/swipe/
     * dimming so general Search and every other repo list (Starred, Owned,
     * Forks, Bookmarks, Trace, Topic search) stay unaffected.
     */
    @AutoAccess boolean ignoreListEligible;

    private static final int SEARCH_PAGE_SIZE = 30;

    // GitHub's default per_page for the list-repos REST endpoints (Owned/
    // Public) - RepositoriesFilter.Kind (Sources/Forks/Archived) is applied
    // client-side against an already-fetched page, so a full server page can
    // shrink to fewer displayed rows; canLoadMore must be judged from this
    // raw fetch size instead of the (possibly filtered) displayed count.
    private static final int OWNED_PUBLIC_PAGE_SIZE = 30;

    // Per-topic pagination state for searchMultiTopics() - not @AutoAccess,
    // same as repos: lost on process death, which just means the next load
    // starts fresh from page 1 for every topic, exactly like a first visit.
    private Map<String, Integer> multiTopicNextPage;
    private Set<String> multiTopicExhausted;
    private Set<Integer> multiTopicSeenIds;

    @Inject
    public RepositoriesPresenter(DaoSession daoSession) {
        super(daoSession);
    }

    @Override
    public void onViewInitialized() {
        super.onViewInitialized();
        if (type.equals(RepositoriesFragment.RepositoriesType.SEARCH)) {
            setEventSubscriber(true);
        }
    }

    @Override
    protected void loadData() {
        if (RepositoriesFragment.RepositoriesType.SEARCH.equals(type)) {
            if (searchModel != null) searchRepos(1);
            return;
        }
        if (RepositoriesFragment.RepositoriesType.TRACE.equals(type)) {
            loadTrace(1);
            return;
        }
        if (RepositoriesFragment.RepositoriesType.BOOKMARK.equals(type)) {
            loadBookmarks(1);
            return;
        }
        if (RepositoriesFragment.RepositoriesType.IGNORED.equals(type)) {
            loadIgnored(1);
            return;
        }
        if (RepositoriesFragment.RepositoriesType.COLLECTION.equals(type)) {
            loadCollection(false);
            return;
        }
        if (RepositoriesFragment.RepositoriesType.TOPIC.equals(type)) {
            initSearchModelForTopic();
            searchRepos(1);
            return;
        }
        if(RepositoriesFragment.RepositoriesType.TRENDING.equals(type)){
            loadTrending(false);
            return;
        }
        if(RepositoriesFragment.RepositoriesType.TOPICS_SEARCH.equals(type)){
            if (isSingleTopicSearch()) {
                initSearchModelForTopicsSearch();
                searchRepos(1);
            } else {
                searchMultiTopics(true);
            }
            return;
        }
        loadRepositories(false, 1);
    }

    @Override
    public void loadRepositories(final boolean isReLoad, final int page) {
        filter = getFilter();
        if (type.equals(RepositoriesFragment.RepositoriesType.SEARCH)) {
            searchRepos(page);
            return;
        }
        if (RepositoriesFragment.RepositoriesType.TRACE.equals(type)) {
            loadTrace(page);
            return;
        }
        if (RepositoriesFragment.RepositoriesType.BOOKMARK.equals(type)) {
            loadBookmarks(page);
            return;
        }
        if (RepositoriesFragment.RepositoriesType.IGNORED.equals(type)) {
            loadIgnored(page);
            return;
        }
        if (RepositoriesFragment.RepositoriesType.COLLECTION.equals(type)) {
            loadCollection(isReLoad);
            return;
        }
        if (RepositoriesFragment.RepositoriesType.TOPIC.equals(type)) {
            initSearchModelForTopic();
            searchRepos(page);
            return;
        }
        if(RepositoriesFragment.RepositoriesType.TRENDING.equals(type)){
            loadTrending(isReLoad);
            return;
        }
        if(RepositoriesFragment.RepositoriesType.TOPICS_SEARCH.equals(type)){
            if (isSingleTopicSearch()) {
                initSearchModelForTopicsSearch();
                searchRepos(page);
            } else {
                searchMultiTopics(page == 1);
            }
            return;
        }
        mView.showLoading();
        final boolean readCacheFirst = !isReLoad && page == 1;

        HttpObserver<ArrayList<Repository>> httpObserver = new HttpObserver<ArrayList<Repository>>() {
            @Override
            public void onError(@NonNull Throwable error) {
                mView.hideLoading();
                handleError(error);
            }

            @Override
            public void onSuccess(@NonNull HttpResponse<ArrayList<Repository>> response) {
                mView.hideLoading();
                boolean kindFilterable = RepositoriesFragment.RepositoriesType.OWNED.equals(type)
                        || RepositoriesFragment.RepositoriesType.PUBLIC.equals(type);
                ArrayList<Repository> pageItems = response.body();
                int rawCount = pageItems.size();
                if (kindFilterable) {
                    pageItems = filterByKind(pageItems);
                }
                int appendedCount;
                if (isReLoad || readCacheFirst || repos == null || page == 1) {
                    repos = pageItems;
                    appendedCount = 0;
                } else {
                    appendedCount = pageItems.size();
                    repos.addAll(pageItems);
                }
                if (kindFilterable) {
                    // A full raw page can shrink to fewer (or zero) displayed
                    // rows after filtering - decide purely from the raw size.
                    mView.setCanLoadMore(rawCount == OWNED_PUBLIC_PAGE_SIZE);
                    mView.showRepositories(repos, appendedCount);
                } else if (rawCount == 0 && repos.size() != 0) {
                    mView.setCanLoadMore(false);
                } else {
                    mView.showRepositories(repos, appendedCount);
                }
            }
        };

        generalRxHttpExecute(new IObservableCreator<ArrayList<Repository>>() {
            @Nullable
            @Override
            public Observable<Response<ArrayList<Repository>>> createObservable(boolean forceNetWork) {
                return getObservable(forceNetWork, page);
            }
        }, httpObserver, readCacheFirst);

    }

    @Override
    public void loadRepositories(RepositoriesFilter filter) {
        this.filter = filter;
        loadRepositories(false, 1);
    }

    public void setForksSort(String sort) {
        this.forksSort = sort;
        loadRepositories(true, 1);
    }

    private ArrayList<Repository> filterByKind(ArrayList<Repository> items) {
        RepositoriesFilter.Kind kind = filter == null ? RepositoriesFilter.Kind.All : filter.getKind();
        if (RepositoriesFilter.Kind.All.equals(kind)) return items;
        ArrayList<Repository> filtered = new ArrayList<>();
        for (Repository repository : items) {
            switch (kind) {
                case Sources:
                    if (!repository.isFork()) filtered.add(repository);
                    break;
                case Forks:
                    if (repository.isFork()) filtered.add(repository);
                    break;
                case Archived:
                    if (repository.isArchived()) filtered.add(repository);
                    break;
            }
        }
        return filtered;
    }

    private Observable<Response<ArrayList<Repository>>> getObservable(boolean forceNetWork, int page) {
        switch (type) {
            case OWNED:
                return getRepoService().getUserRepos(forceNetWork, page, filter.getType(),
                        filter.getSort(), filter.getSortDirection());
            case PUBLIC:
                return getRepoService().getUserPublicRepos(forceNetWork, user, page,
                        filter.getType(), filter.getSort(), filter.getSortDirection());
            case STARRED:
                return getRepoService().getStarredRepos(forceNetWork, user, page,
                        filter.getSort(), filter.getSortDirection());
            case FORKS:
                return getRepoService().getForks(forceNetWork, user, repo, page, forksSort);
            default:
                return null;
        }
    }

    private void searchRepos(final int page) {
        mView.showLoading();

        HttpObserver<SearchResult<Repository>> httpObserver =
                new HttpObserver<SearchResult<Repository>>() {
                    @Override
                    public void onError(@NonNull Throwable error) {
                        mView.hideLoading();
                        handleError(error);
                    }

                    @Override
                    public void onSuccess(@NonNull HttpResponse<SearchResult<Repository>> response) {
                        mView.hideLoading();
                        ArrayList<Repository> items = response.body().getItems();
                        int rawCount = items.size();
                        if (ignoreListEligible && PrefUtils.isIgnoreListApplied()) {
                            items = filterIgnored(items);
                        }
                        int appendedCount;
                        if (repos == null || page == 1) {
                            repos = items;
                            appendedCount = 0;
                        } else {
                            appendedCount = items.size();
                            repos.addAll(items);
                        }
                        // GitHub's search API has no "created" sort value (it's
                        // silently ignored - confirmed against the live API,
                        // both asc/desc order came back identical), unlike
                        // stars/updated which it sorts natively. So for
                        // "recently added", re-sort what we've fetched so far
                        // client-side instead of trusting the API's order.
                        if (RepositoriesFragment.RepositoriesType.TOPICS_SEARCH.equals(type)
                                && "created".equals(sort)) {
                            sortRepos(repos, sort);
                            // a full re-sort can move existing rows, not just
                            // add new ones at the end - not safe to treat as
                            // a pure append anymore.
                            appendedCount = 0;
                        }
                        if (ignoreListEligible) {
                            // Filtering out ignored repos can shrink a full
                            // raw page down to far fewer displayed items, so
                            // ListFragment's auto-judge (itemCount % pageSize)
                            // would wrongly conclude "not a full page" and
                            // disable further loading - decide canLoadMore
                            // from the raw (pre-filter) fetch size instead.
                            // RepositoriesFragment disables the auto-judge
                            // for ignoreListEligible fragments to make this
                            // the only source of truth.
                            mView.setCanLoadMore(rawCount == SEARCH_PAGE_SIZE);
                            mView.showRepositories(repos, appendedCount);
                        } else if (rawCount == 0 && repos.size() != 0) {
                            mView.setCanLoadMore(false);
                        } else {
                            mView.showRepositories(repos, appendedCount);
                        }
                    }
                };
        generalRxHttpExecute(new IObservableCreator<SearchResult<Repository>>() {
            @Nullable
            @Override
            public Observable<Response<SearchResult<Repository>>> createObservable(boolean forceNetWork) {
                return getSearchService().searchRepos(searchModel.getQuery(), searchModel.getSort(),
                        searchModel.getOrder(), page);
            }
        }, httpObserver);
    }

    @Subscribe
    public void onSearchEvent(@NonNull Event.SearchEvent searchEvent) {
        if (!searchEvent.searchModel.getType().equals(SearchModel.SearchType.Repository)) return;
        setLoaded(false);
        this.searchModel = searchEvent.searchModel;
        prepareLoadData();
    }

    private void handleError(Throwable error) {
        if (!StringUtils.isBlankList(repos)) {
            mView.showErrorToast(getErrorTip(error));
        } else if (error instanceof HttpPageNoFoundError) {
            mView.showRepositories(new ArrayList<Repository>(), 0);
        } else {
            mView.showLoadError(getErrorTip(error));
        }
    }

    public String getUser() {
        return user;
    }

    public RepositoriesFragment.RepositoriesType getType() {
        return type;
    }

    public boolean isIgnoreListEligible() {
        return ignoreListEligible;
    }

    public RepositoriesFilter getFilter() {
        if (filter == null) {
            filter = RepositoriesFragment.RepositoriesType.STARRED.equals(type) ?
                    RepositoriesFilter.DEFAULT_STARRED_REPO : RepositoriesFilter.DEFAULT;
        }
        return filter;
    }

    private void loadTrace(final int page) {
        mView.showLoading();
        Observable.fromCallable(() -> {
            long start = System.currentTimeMillis();

            // TraceRepoDao/TraceUserDao are pre-v4-schema tables: a v3->v4
            // migration moved trace tracking into the unified Trace table (repo
            // vs user rows told apart by type) plus LocalRepo/LocalUser for the
            // actual repo/user data, and drops the old tables outright on any DB
            // that goes through that migration - querying them there crashed
            // with "no such table", and even on a fresh install (where they
            // still get created empty) nothing writes to them anymore, since
            // RepositoryPresenter.saveTrace() already writes into Trace/LocalRepo.
            List<Trace> traces = daoSession.getTraceDao().queryBuilder()
                    .where(TraceDao.Properties.Type.eq("repo"))
                    .orderDesc(TraceDao.Properties.LatestTime)
                    .offset((page - 1) * 30)
                    .limit(30)
                    .list();

            ArrayList<Repository> queryRepos = new ArrayList<>();
            for (Trace trace : traces) {
                LocalRepo localRepo = daoSession.getLocalRepoDao().load(trace.getRepoId());
                if (localRepo != null) {
                    queryRepos.add(Repository.generateFromLocalRepo(localRepo));
                }
            }
            Logger.t("loadTrace").d(System.currentTimeMillis() - start);
            return queryRepos;
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(queryRepos -> {
            if (mView == null) return;
            showQueryRepos(queryRepos, page);
        }, error -> {
            if (mView == null) return;
            mView.hideLoading();
            mView.showLoadError(getErrorTip(error));
        });
    }

    private void loadBookmarks(final int page) {
        mView.showLoading();
        Observable.fromCallable(() -> {
            // Same stale-table bug as loadTrace() (see its comment): BookMarkRepoDao
            // is a pre-v4-schema table nothing writes to anymore - bookmarking now
            // writes into the unified Bookmark table (repoId/userId + type) plus
            // LocalRepo/LocalUser for the actual repo/user data.
            List<Bookmark> bookmarks = daoSession.getBookmarkDao().queryBuilder()
                    .where(BookmarkDao.Properties.Type.eq("repo"))
                    .orderDesc(BookmarkDao.Properties.MarkTime)
                    .offset((page - 1) * 30)
                    .limit(30)
                    .list();

            ArrayList<Repository> queryRepos = new ArrayList<>();
            for (Bookmark bookmark : bookmarks) {
                LocalRepo localRepo = daoSession.getLocalRepoDao().load(bookmark.getRepoId());
                if (localRepo != null) {
                    queryRepos.add(Repository.generateFromLocalRepo(localRepo));
                }
            }
            return queryRepos;
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(queryRepos -> {
            if (mView == null) return;
            showQueryRepos(queryRepos, page);
        }, error -> {
            if (mView == null) return;
            mView.hideLoading();
            mView.showLoadError(getErrorTip(error));
        });
    }

    private void loadIgnored(final int page) {
        mView.showLoading();
        Observable.fromCallable(() -> {
            List<IgnoredRepo> ignoredRepos = daoSession.getIgnoredRepoDao().queryBuilder()
                    .orderDesc(IgnoredRepoDao.Properties.IgnoredAt)
                    .offset((page - 1) * 30)
                    .limit(30)
                    .list();

            ArrayList<Repository> queryRepos = new ArrayList<>();
            for (IgnoredRepo ignoredRepo : ignoredRepos) {
                queryRepos.add(generateFromIgnoredRepo(ignoredRepo));
            }
            return queryRepos;
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(queryRepos -> {
            if (mView == null) return;
            showQueryRepos(queryRepos, page);
        }, error -> {
            if (mView == null) return;
            mView.hideLoading();
            mView.showLoadError(getErrorTip(error));
        });
    }

    /**
     * IgnoredRepo is self-contained (denormalized display fields, no
     * LocalRepo join) since it's keyed by fullName, not a numeric repo id -
     * see IgnoredRepo's class comment for why.
     */
    private Repository generateFromIgnoredRepo(IgnoredRepo ignoredRepo) {
        Repository repository = new Repository();
        repository.setFullName(ignoredRepo.getFullName());
        repository.setName(ignoredRepo.getName());
        repository.setDescription(ignoredRepo.getDescription());
        repository.setLanguage(ignoredRepo.getLanguage());
        if (ignoredRepo.getStargazersCount() != null) {
            repository.setStargazersCount(ignoredRepo.getStargazersCount());
        }
        if (ignoredRepo.getForksCount() != null) {
            repository.setForksCount(ignoredRepo.getForksCount());
        }
        User owner = new User();
        owner.setLogin(ignoredRepo.getOwnerLogin());
        owner.setAvatarUrl(ignoredRepo.getOwnerAvatarUrl());
        repository.setOwner(owner);
        return repository;
    }

    /**
     * Applied to a freshly-fetched batch before it's merged into repos/shown
     * - only called when ignoreListEligible && PrefUtils.isIgnoreListApplied().
     */
    private ArrayList<Repository> filterIgnored(ArrayList<Repository> repositories) {
        ArrayList<Repository> filtered = new ArrayList<>();
        for (Repository repository : repositories) {
            if (!IgnoredRepoHelper.isIgnored(repository.getFullName())) {
                filtered.add(repository);
            }
        }
        return filtered;
    }

    private void showQueryRepos(ArrayList<Repository> queryRepos, int page){
        int appendedCount;
        if(repos == null || page == 1){
            repos = queryRepos;
            appendedCount = 0;
        } else {
            appendedCount = queryRepos.size();
            repos.addAll(queryRepos);
        }

        mView.showRepositories(repos, appendedCount);
        mView.hideLoading();
    }

    public void setLanguage(TrendingLanguage language) {
        this.language = language;
    }

    public void setSearchModel(SearchModel searchModel) {
        this.searchModel = searchModel;
    }

    public void setTopicsSearchParams(ArrayList<String> topicSlugs, String sort) {
        this.topicSlugs = topicSlugs;
        this.sort = sort;
    }

    /**
     * A single result of fetching one page of one topic's search, tagged
     * with which topic it came from so the subscriber can advance that
     * topic's own page counter and detect when it's exhausted.
     */
    private static class TopicPage {
        final String topicSlug;
        final ArrayList<Repository> items;
        TopicPage(String topicSlug, ArrayList<Repository> items) {
            this.topicSlug = topicSlug;
            this.items = items;
        }
    }

    /**
     * GitHub's search API rejects OR between qualifiers ("logical operators
     * only apply to text, not to qualifiers" - confirmed against the live
     * API), so there is no single query for "repos matching any of these
     * topics". Instead this fires one topic:<slug> search per selected topic
     * in parallel, merges the results (de-duped by repo id), and re-sorts the
     * merged pool by the chosen field - each source list already comes back
     * sorted from GitHub, but interleaving several sorted lists isn't sorted
     * as a whole.
     *
     * Pagination works by tracking each topic's own next-page number and
     * exhausted state independently (multiTopicNextPage/multiTopicExhausted):
     * every load-more round re-queries only the topics that haven't yet
     * returned a partial page, merges any new (not already seen) repos into
     * the accumulated list, and re-sorts the whole thing. A topic is marked
     * exhausted once it returns fewer than a full page - the same heuristic
     * ListFragment itself uses for the single-query case. Load-more overall
     * stays enabled until every topic is exhausted.
     *
     * @param freshLoad true for a first load or a reload (new topics/sort
     *                  selection) - resets all pagination state and results;
     *                  false to fetch the next page for each not-yet-
     *                  exhausted topic and append.
     */
    private void searchMultiTopics(boolean freshLoad) {
        mView.showLoading();
        if (StringUtils.isBlankList(topicSlugs)) {
            repos = new ArrayList<>();
            mView.hideLoading();
            mView.showRepositories(repos, 0);
            mView.setCanLoadMore(false);
            return;
        }
        if (freshLoad || multiTopicNextPage == null) {
            multiTopicNextPage = new HashMap<>();
            multiTopicExhausted = new HashSet<>();
            multiTopicSeenIds = new HashSet<>();
            repos = new ArrayList<>();
            for (String slug : topicSlugs) {
                multiTopicNextPage.put(slug, 1);
            }
        }
        final String sortField = StringUtils.isBlank(sort) ? "stars" : sort;

        List<String> activeTopics = new ArrayList<>();
        for (String slug : topicSlugs) {
            if (!multiTopicExhausted.contains(slug)) activeTopics.add(slug);
        }
        if (activeTopics.isEmpty()) {
            mView.hideLoading();
            mView.showRepositories(repos, 0);
            mView.setCanLoadMore(false);
            return;
        }

        List<Observable<TopicPage>> sources = new ArrayList<>();
        for (final String slug : activeTopics) {
            final int page = multiTopicNextPage.get(slug);
            sources.add(getSearchService().searchRepos("topic:" + slug, sortField, "desc", page)
                    .map(response -> {
                        ArrayList<Repository> list = new ArrayList<>();
                        if (response.isSuccessful() && response.body() != null) {
                            list.addAll(response.body().getItems());
                        }
                        return new TopicPage(slug, list);
                    })
                    .onErrorReturn(throwable -> new TopicPage(slug, new ArrayList<>())));
        }

        Observable.zip(sources, results -> {
            ArrayList<TopicPage> pages = new ArrayList<>();
            for (Object result : results) {
                pages.add((TopicPage) result);
            }
            return pages;
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(pages -> {
            if (mView == null) return;
            for (TopicPage page : pages) {
                multiTopicNextPage.put(page.topicSlug, multiTopicNextPage.get(page.topicSlug) + 1);
                // Exhaustion is decided from the raw (pre-filter) page size,
                // same reasoning as searchRepos() - filtering out ignored
                // repos can shrink a full page down to fewer displayed items.
                if (page.items.size() < SEARCH_PAGE_SIZE) {
                    multiTopicExhausted.add(page.topicSlug);
                }
                ArrayList<Repository> items = page.items;
                if (ignoreListEligible && PrefUtils.isIgnoreListApplied()) {
                    items = filterIgnored(items);
                }
                for (Repository repository : items) {
                    if (multiTopicSeenIds.add(repository.getId())) {
                        repos.add(repository);
                    }
                }
            }
            sortRepos(repos, sortField);
            mView.hideLoading();
            // every load-more round re-sorts the whole merged pool, so old
            // items can move too - never a pure append.
            mView.showRepositories(repos, 0);
            mView.setCanLoadMore(multiTopicExhausted.size() < topicSlugs.size());
        }, error -> {
            if (mView == null) return;
            mView.hideLoading();
            mView.showLoadError(getErrorTip(error));
        });
    }

    private void loadCollection(boolean isReload){
        mView.showLoading();
        HttpObserver<ResponseBody> httpObserver = new HttpObserver<ResponseBody>() {
            @Override
            public void onError(Throwable error) {
                mView.hideLoading();
                mView.showLoadError(getErrorTip(error));
            }

            @Override
            public void onSuccess(HttpResponse<ResponseBody> response) {
                try {
                    parseCollectionsPageData(response.body().string());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        generalRxHttpExecute(forceNetWork -> getGitHubWebPageService()
                        .getCollectionInfo(forceNetWork, collection.getId()),
                httpObserver, !isReload);

    }

    private void parseCollectionsPageData(String page){
        Observable.just(page)
                .map(s -> {
                    ArrayList<Repository> repos = new ArrayList<>();
                    try {
                        Document doc = Jsoup.parse(s, AppConfig.GITHUB_BASE_URL);
                        Elements elements = doc.getElementsByTag("article");
                        for (Element element : elements) {
                            //maybe a user or an org, so add catch
                            try{
                                repos.add(parseCollectionsRepositoryData(element));
                            } catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    } catch (Exception e){
                        e.printStackTrace();
                    }

                    return repos;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(results -> {
                    if(mView == null) return;
                    if(results.size() != 0){
                        repos = results;
                        mView.hideLoading();
                        mView.showRepositories(repos, 0);
                    } else {
                        String errorTip = String.format(getString(R.string.github_page_parse_error),
                                getString(R.string.repo_collections));
                        mView.showLoadError(errorTip);
                        mView.hideLoading();
                    }
                });
    }

    private Repository parseCollectionsRepositoryData(Element element) throws Exception{
        String fullName = element.select("div > h1 > a").attr("href");
        fullName = fullName.substring(1);
        String owner = fullName.substring(0, fullName.lastIndexOf("/"));
        String repoName = fullName.substring(fullName.lastIndexOf("/") + 1);
//        String ownerAvatar = element.select("div > div > a > img").attr("src");
        String ownerAvatar = "";

        Elements articleElements = element.getElementsByTag("div");
        Element descElement = articleElements.get(articleElements.size() - 2);
        StringBuilder desc = new StringBuilder();
        for(TextNode textNode : descElement.textNodes()){
            desc.append(textNode.getWholeText());
        }

        Element numElement = articleElements.last();
        String starNumStr =  numElement.select("a").get(0).textNodes().get(1).toString();
        String forkNumStr =  numElement.select("a").get(1).textNodes().get(1).toString();
        String language = "";
        Elements languageElements = numElement.select("span > span > span");
        if(languageElements.size() > 0){
            language = numElement.select("span > span > span").get(1).textNodes().get(0).toString();
        }

        Repository repo = new Repository();
        repo.setFullName(fullName);
        repo.setName(repoName);
        User user = new User();
        user.setLogin(owner);
        user.setAvatarUrl(ownerAvatar);
        repo.setOwner(user);

        repo.setDescription(desc.toString());
        repo.setStargazersCount(Integer.parseInt(starNumStr.replaceAll(" ", "")));
        repo.setForksCount(Integer.parseInt(forkNumStr.replaceAll(" ", "")));
        repo.setLanguage(language);

        return repo;
    }

    private void initSearchModelForTopic(){
        if(searchModel == null){
            searchModel = new SearchModel(SearchModel.SearchType.Repository);
            searchModel.setQuery("topic:" + topic.getId());
        }
    }

    /**
     * With exactly one topic selected, TOPICS_SEARCH is just a normal single
     * search query - same shape as TOPIC - so it can use the shared
     * searchRepos(page) path (with its own real per-request pagination)
     * instead of searchMultiTopics()'s independent per-topic page tracking.
     * Rebuilt on every call (not cached like initSearchModelForTopic()) since
     * the topic or sort can change without recreating the presenter.
     */
    private boolean isSingleTopicSearch() {
        return topicSlugs != null && topicSlugs.size() == 1;
    }

    private void initSearchModelForTopicsSearch(){
        String sortField = StringUtils.isBlank(sort) ? "stars" : sort;
        searchModel = new SearchModel(SearchModel.SearchType.Repository, "topic:" + topicSlugs.get(0))
                .setSort(sortField)
                .setDesc(true);
    }

    /**
     * GitHub's search API only natively sorts by stars/forks/help-wanted-
     * issues/updated - "created" is silently ignored (confirmed against the
     * live API: asc and desc came back identical), so "recently added" has
     * to be sorted client-side instead of trusting the API's order.
     */
    private void sortRepos(ArrayList<Repository> list, String sortField) {
        Collections.sort(list, (a, b) -> {
            if ("created".equals(sortField)) {
                Date dateA = a.getCreatedAt();
                Date dateB = b.getCreatedAt();
                if (dateA == null || dateB == null) return 0;
                return dateB.compareTo(dateA);
            } else if ("updated".equals(sortField)) {
                Date dateA = a.getUpdatedAt();
                Date dateB = b.getUpdatedAt();
                if (dateA == null || dateB == null) return 0;
                return dateB.compareTo(dateA);
            }
            return Integer.compare(b.getStargazersCount(), a.getStargazersCount());
        });
    }

    private void loadTrending(boolean isReload){
        mView.showLoading();
        HttpObserver<ResponseBody> httpObserver = new HttpObserver<ResponseBody>() {
            @Override
            public void onError(Throwable error) {
                mView.hideLoading();
                mView.showLoadError(getErrorTip(error));
            }

            @Override
            public void onSuccess(HttpResponse<ResponseBody> response) {
                try {
                    parseTrendingPageData(response.body().string());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        generalRxHttpExecute(forceNetWork -> getGitHubWebPageService()
                        .getTrendingRepos(forceNetWork, language.getSlug(), since.name()),
                httpObserver, !isReload);
    }

    private void parseTrendingPageData(String page){
        Observable.just(page)
                .map(s -> {
                    ArrayList<Repository> repos = new ArrayList<>();
                    try {
                        Document doc = Jsoup.parse(s, AppConfig.GITHUB_BASE_URL);
                        Elements elements = doc.getElementsByClass("Box-row");
                        if(elements.size() != 0){
                            for (Element element : elements) {
                                try{
                                    repos.add(parseTrendingRepositoryData(element));
                                } catch (Exception e){
                                    e.printStackTrace();
                                }
                            }
                        }
                        // The page returned something but none of it matched
                        // this scraper's expected structure (elements.size()
                        // was 0, or every per-row parse failed) - GitHub
                        // likely changed the trending page's HTML, so treat
                        // this the same as a parse exception rather than
                        // silently showing an empty list.
                        if (repos.isEmpty()) {
                            repos = null;
                        }
                    } catch (Exception e){
                        e.printStackTrace();
                        repos = null;
                    }

                    if (repos != null && ignoreListEligible && PrefUtils.isIgnoreListApplied()) {
                        repos = filterIgnored(repos);
                    }
                    return repos;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(results -> {
                    if(mView == null) return;
                    if(results != null){
                        repos = results;
                        mView.hideLoading();
                        mView.showRepositories(repos, 0);
                    } else {
                        loadTrendingViaSearchFallback();
                    }
                });
    }

    /**
     * GitHub's trending page has no REST/Search-API equivalent - "stars
     * gained in period" is computed server-side and isn't exposed anywhere
     * scrapeable-free, so this can't be a full replacement for the real
     * thing. But when the scraper above can't parse the page at all
     * (elements.size() was 0, or every per-row parse failed - almost always
     * because GitHub changed the trending page's HTML), showing a dead
     * "parse error" screen until this scraper gets updated is worse than an
     * approximation: recently-created repos sorted by stars, the same
     * search-API mechanism CreatedActivity's tabs already use. Only ever
     * called with since == Daily/Weekly/Monthly (real Trending never
     * constructs Yearly/TenYears/Max), so those are the only cases handled.
     */
    private void loadTrendingViaSearchFallback() {
        Calendar calendar = Calendar.getInstance();
        switch (since) {
            case Daily:
                calendar.add(Calendar.DAY_OF_YEAR, -1);
                break;
            case Weekly:
                calendar.add(Calendar.WEEK_OF_YEAR, -1);
                break;
            case Monthly:
            default:
                calendar.add(Calendar.MONTH, -1);
                break;
        }
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.getTime());
        StringBuilder query = new StringBuilder("created:>").append(date);
        String slug = language == null ? null : language.getSlug();
        if (slug != null && !slug.isEmpty() && !"unknown".equals(slug) && !"all".equals(slug)) {
            query.append(" language:").append(encodeLanguageSlug(slug));
        }

        HttpObserver<SearchResult<Repository>> httpObserver =
                new HttpObserver<SearchResult<Repository>>() {
                    @Override
                    public void onError(@NonNull Throwable error) {
                        mView.hideLoading();
                        String errorTip = String.format(getString(R.string.github_page_parse_error),
                                getString(R.string.trending));
                        mView.showLoadError(errorTip);
                    }

                    @Override
                    public void onSuccess(@NonNull HttpResponse<SearchResult<Repository>> response) {
                        mView.hideLoading();
                        ArrayList<Repository> items = response.body().getItems();
                        if (ignoreListEligible && PrefUtils.isIgnoreListApplied()) {
                            items = filterIgnored(items);
                        }
                        repos = items;
                        mView.showRepositories(repos, 0);
                    }
                };

        generalRxHttpExecute(new IObservableCreator<SearchResult<Repository>>() {
            @Nullable
            @Override
            public Observable<Response<SearchResult<Repository>>> createObservable(boolean forceNetWork) {
                return getSearchService().searchRepos(query.toString(), "stars", "desc", 1);
            }
        }, httpObserver);
    }

    /**
     * SearchService's "q" param is sent with @Query(encoded = true), so it is
     * never percent-encoded by Retrofit/OkHttp. Language slugs like "c++" or
     * "c#" contain characters (+, #) that are legal-but-meaningful in a URL
     * query component - a literal "+" is read back by GitHub as a space - so
     * they must be percent-encoded by hand before being appended.
     */
    private String encodeLanguageSlug(String slug) {
        try {
            return java.net.URLEncoder.encode(slug, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return slug;
        }
    }

    private Repository parseTrendingRepositoryData(Element element) throws Exception{
        String fullName = element.select("h2 > a").attr("href");
        fullName = fullName.substring(1);
        String owner = fullName.substring(0, fullName.lastIndexOf("/"));
        String repoName = fullName.substring(fullName.lastIndexOf("/") + 1);

        Element descElement = element.getElementsByClass("col-9 color-fg-muted my-1 tmp-pr-4").first();
        Element numElement = element.getElementsByClass("f6 color-fg-muted mt-2").first();
        StringBuilder desc = new StringBuilder();
        String language = "unknown";
        String starNumStr = "0";
        String forkNumStr = "0";
        String periodNumStr = "0";

        try{
            if(null != descElement){
                for(TextNode textNode : descElement.textNodes()){
                    desc.append(textNode.getWholeText());
                }
            }

            if(null != numElement){
                Elements languageElements = numElement.select("span > span");
                if(null != languageElements && languageElements.size() > 0){
                    language = numElement.select("span > span").get(1).textNodes().get(0).toString().trim();
                }

                for (Element e : numElement.select("a.Link--muted.d-inline-block")) {
                    if (e.attr("href").endsWith("stargazers")) {
                        starNumStr = e.textNodes().get(0).toString()
                                .replaceAll(" ", "").replaceAll(",", "");
                    } else if (e.attr("href").endsWith("forks")) {
                        forkNumStr = e.textNodes().get(0).toString()
                                .replaceAll(" ", "").replaceAll(",", "");
                    }
                }

                Element periodElement =  numElement.getElementsByClass("d-inline-block float-sm-right").first();
                if(periodElement != null){
                    periodNumStr = periodElement.childNodes().get(2).toString().trim();
                    periodNumStr = periodNumStr.substring(0, periodNumStr.indexOf(" "))
                            .replaceAll(",", "");
                }
            }
        }catch (Exception e){
            desc = new StringBuilder("desc parse error.");
            Logger.e("Trending repo desc or num info parse error.", e);
        }

        Repository repo = new Repository();
        repo.setFullName(fullName);
        repo.setName(repoName);
        User user = new User();
        user.setLogin(owner);
        // The trending page (scraped HTML) has no single per-repo owner avatar -
        // GitHub's own trending UI only shows small "built by" contributor faces.
        // github.com/<login>.png is GitHub's stable convention for a user/org's
        // avatar without needing an API call, so use that instead of leaving it
        // blank (which is why trending rows showed no icon at all).
        user.setAvatarUrl("https://github.com/" + owner + ".png");
        repo.setOwner(user);

        repo.setDescription(desc.toString().trim()
                .replaceAll("\n", ""));
        repo.setStargazersCount(Integer.parseInt(starNumStr));
        repo.setForksCount(Integer.parseInt(forkNumStr));
        repo.setSinceStargazersCount(Integer.parseInt(periodNumStr));
        repo.setLanguage(language);
        repo.setSince(since);

        return repo;
    }

    public TrendingLanguage getLanguage() {
        return language;
    }
}
