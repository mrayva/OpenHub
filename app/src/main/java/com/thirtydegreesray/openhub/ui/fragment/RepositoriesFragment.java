

package com.thirtydegreesray.openhub.ui.fragment;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.inject.component.AppComponent;
import com.thirtydegreesray.openhub.inject.component.DaggerFragmentComponent;
import com.thirtydegreesray.openhub.inject.module.FragmentModule;
import com.thirtydegreesray.openhub.mvp.contract.IRepositoriesContract;
import com.thirtydegreesray.openhub.mvp.model.Collection;
import com.thirtydegreesray.openhub.mvp.model.Repository;
import com.thirtydegreesray.openhub.mvp.model.SearchModel;
import com.thirtydegreesray.openhub.mvp.model.Topic;
import com.thirtydegreesray.openhub.mvp.model.TrendingLanguage;
import com.thirtydegreesray.openhub.mvp.model.filter.RepositoriesFilter;
import com.thirtydegreesray.openhub.mvp.model.filter.TrendingSince;
import com.thirtydegreesray.openhub.mvp.presenter.RepositoriesPresenter;
import com.thirtydegreesray.openhub.ui.activity.RepositoryActivity;
import com.thirtydegreesray.openhub.ui.activity.TrendingActivity;
import com.thirtydegreesray.openhub.ui.adapter.RepositoriesAdapter;
import com.thirtydegreesray.openhub.ui.adapter.base.IgnoreSwipeCallback;
import com.thirtydegreesray.openhub.ui.adapter.base.ItemTouchHelperCallback;
import com.thirtydegreesray.openhub.ui.fragment.base.ListFragment;
import com.thirtydegreesray.openhub.ui.fragment.base.OnDrawerSelectedListener;
import com.thirtydegreesray.openhub.util.BundleHelper;
import com.thirtydegreesray.openhub.util.IgnoredRepoHelper;
import com.thirtydegreesray.openhub.util.PrefUtils;

import java.util.ArrayList;

/**
 * Created on 2017/7/18.
 *
 * @author ThirtyDegreesRay
 */

public class RepositoriesFragment extends ListFragment<RepositoriesPresenter, RepositoriesAdapter>
            implements IRepositoriesContract.View, OnDrawerSelectedListener,
        TrendingActivity.LanguageUpdateListener,
        ItemTouchHelperCallback.ItemGestureListener,
        IgnoreSwipeCallback.Listener{

    public enum RepositoriesType{
        OWNED, PUBLIC, STARRED, TRENDING, SEARCH, FORKS, TRACE, BOOKMARK, IGNORED, COLLECTION, TOPIC, TOPICS_SEARCH
    }

    public static RepositoriesFragment create(@NonNull RepositoriesType type,
                                              @NonNull String user){
        RepositoriesFragment fragment = new RepositoriesFragment();
        fragment.setArguments(BundleHelper.builder().put("type", type)
                .put("user", user).build());
        return fragment;
    }

    public static RepositoriesFragment createForCollection(@NonNull Collection collection){
        RepositoriesFragment fragment = new RepositoriesFragment();
        fragment.setArguments(BundleHelper.builder()
                .put("type", RepositoriesType.COLLECTION)
                .put("collection", collection)
                .build());
        return fragment;
    }

    public static RepositoriesFragment createForTopic(@NonNull Topic topic){
        RepositoriesFragment fragment = new RepositoriesFragment();
        fragment.setArguments(BundleHelper.builder()
                .put("type", RepositoriesType.TOPIC)
                .put("topic", topic)
                .build());
        return fragment;
    }

    public static RepositoriesFragment createForForks(@NonNull String user, @NonNull String repo){
        RepositoriesFragment fragment = new RepositoriesFragment();
        fragment.setArguments(BundleHelper.builder()
                .put("type", RepositoriesType.FORKS)
                .put("user", user)
                .put("repo", repo)
                .build());
        return fragment;
    }

    public static RepositoriesFragment createForSearch(@NonNull SearchModel searchModel){
        RepositoriesFragment fragment = new RepositoriesFragment();
        fragment.setArguments(
                BundleHelper.builder()
                        .put("type", RepositoriesType.SEARCH)
                        .put("searchModel", searchModel)
                        .build()
        );
        return fragment;
    }

    /**
     * Same as createForSearch, but tags the fragment's arguments with a
     * TrendingSince value purely so the hosting pager Activity (CreatedActivity)
     * can identify which tab a re-attached fragment instance belongs to after
     * process/config restoration - mirrors createForTrending's "since" tag.
     */
    public static RepositoriesFragment createForSearch(@NonNull SearchModel searchModel,
                                                        @NonNull TrendingSince since){
        RepositoriesFragment fragment = new RepositoriesFragment();
        fragment.setArguments(
                BundleHelper.builder()
                        .put("type", RepositoriesType.SEARCH)
                        .put("searchModel", searchModel)
                        .put("since", since)
                        .put("ignoreListEligible", true)
                        .build()
        );
        return fragment;
    }

    public static RepositoriesFragment createForTrending(@NonNull TrendingSince since){
        RepositoriesFragment fragment = new RepositoriesFragment();
        fragment.setArguments(
                BundleHelper.builder()
                        .put("type", RepositoriesType.TRENDING)
                        .put("since", since)
                        .put("ignoreListEligible", true)
                        .build()
        );
        return fragment;
    }

    /**
     * Repos matching ANY of the given topics ("OR", not GitHub search's native
     * topic:a topic:b AND), sorted by stars or last-updated. GitHub's search
     * API rejects boolean OR between qualifiers ("logical operators only apply
     * to text, not to qualifiers" - verified against the live API), so
     * RepositoriesPresenter fires one topic: query per topic and merges/re-sorts
     * the results client-side instead of a single OR query.
     */
    public static RepositoriesFragment createForTopicsSearch(@NonNull ArrayList<String> topicSlugs,
                                                               @NonNull String sort){
        RepositoriesFragment fragment = new RepositoriesFragment();
        fragment.setArguments(
                BundleHelper.builder()
                        .put("type", RepositoriesType.TOPICS_SEARCH)
                        .putStringList("topicSlugs", topicSlugs)
                        .put("sort", sort)
                        .build()
        );
        return fragment;
    }

    public static RepositoriesFragment createForTrace(){
        RepositoriesFragment fragment = new RepositoriesFragment();
        fragment.setArguments(BundleHelper.builder().put("type", RepositoriesType.TRACE).build());
        return fragment;
    }

    public static RepositoriesFragment createForBookmark(){
        RepositoriesFragment fragment = new RepositoriesFragment();
        fragment.setArguments(BundleHelper.builder().put("type", RepositoriesType.BOOKMARK).build());
        return fragment;
    }

    public static RepositoriesFragment createForIgnored(){
        RepositoriesFragment fragment = new RepositoriesFragment();
        fragment.setArguments(BundleHelper.builder().put("type", RepositoriesType.IGNORED).build());
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void showRepositories(ArrayList<Repository> repositoryList, int appendedCount) {
        adapter.setData(repositoryList);
        if (appendedCount > 0) {
            postNotifyItemRangeInserted(repositoryList.size() - appendedCount, appendedCount);
        } else {
            postNotifyDataSetChanged();
        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_list;
    }

    @Override
    protected void setupFragmentComponent(AppComponent appComponent) {
        DaggerFragmentComponent.builder()
                .appComponent(appComponent)
                .fragmentModule(new FragmentModule(this))
                .build()
                .inject(this);
    }

    @Override
    protected void initFragment(Bundle savedInstanceState){
        super.initFragment(savedInstanceState);
        setLoadMoreEnable(!RepositoriesType.COLLECTION.equals(mPresenter.getType()));
        if (mPresenter.isIgnoreListEligible()) {
            adapter.setShowIgnoredState(true);
            // Filtering can shrink a full raw page down to far fewer visible
            // items - RepositoriesPresenter decides canLoadMore explicitly
            // from the raw fetch size instead (see searchRepos()'s comment).
            setAutoJudgeCanLoadMoreEnable(false);
            // Default change-animation cross-fades old/new holder alpha and
            // resets itemView's alpha to 1 once it "finishes" (instant, since
            // it's the same holder) - stomping the setAlpha(0.5f) dimming
            // RepositoriesAdapter just applied in the very onBindViewHolder
            // call this notifyItemChanged() triggered.
            if (recyclerView.getItemAnimator() instanceof SimpleItemAnimator) {
                ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
            }
            IgnoreSwipeCallback callback = new IgnoreSwipeCallback(getContext(), adapter, this);
            new ItemTouchHelper(callback).attachToRecyclerView(recyclerView);
            // Trending/Created host their DAILY/WEEKLY/MONTHLY tabs in a
            // ViewPager, whose onInterceptTouchEvent runs before the child
            // RecyclerView's and claims a horizontal drag for paging as soon
            // as it exceeds touch slop - racing (and often beating)
            // ItemTouchHelper for the same gesture, so a swipe meant to
            // ignore a row would instead flip tabs. Telling the ViewPager
            // not to intercept for the duration of any touch that starts on
            // this list hands every horizontal drag here to ItemTouchHelper
            // instead; tab switching still works via the tab bar itself.
            recyclerView.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
                @Override
                public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                    if (e.getActionMasked() == MotionEvent.ACTION_DOWN && rv.getParent() != null) {
                        rv.getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    return false;
                }
            });
        } else if (RepositoriesType.IGNORED.equals(mPresenter.getType())) {
            ItemTouchHelperCallback callback = new ItemTouchHelperCallback(
                    0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, this);
            new ItemTouchHelper(callback).attachToRecyclerView(recyclerView);
        }
    }

    @Override
    protected void onReLoadData() {
        mPresenter.loadRepositories(true, 1);
    }

    @Override
    protected String getEmptyTip() {
        if(RepositoriesType.TRENDING.equals(mPresenter.getType())){
            return String.format(getString(R.string.no_trending_repos), mPresenter.getLanguage().getName());
        }
        if(RepositoriesType.IGNORED.equals(mPresenter.getType())){
            return getString(R.string.no_ignored_repos);
        }
        return getString(R.string.no_repository);
    }

    @Override
    public void onItemClick(int position, @NonNull View view) {
        super.onItemClick(position, view);
        if(RepositoriesType.TRENDING.equals(mPresenter.getType())
                || RepositoriesType.TRACE.equals(mPresenter.getType())
                || RepositoriesType.BOOKMARK.equals(mPresenter.getType())
                || RepositoriesType.IGNORED.equals(mPresenter.getType())
                || RepositoriesType.COLLECTION.equals(mPresenter.getType())){
            RepositoryActivity.show(getActivity(), adapter.getData().get(position).getOwner().getLogin(),
                    adapter.getData().get(position).getName());
        } else {
            RepositoryActivity.show(getActivity(), adapter.getData().get(position));
        }
    }

    @Override
    protected void onLoadMore(int page) {
        super.onLoadMore(page);
        mPresenter.loadRepositories(false, page);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
//        if(mPresenter.getType().equals(RepositoriesType.OWNED)){
//            inflater.inflate(R.menu.menu_owned_repo, menu);
//            if(!mPresenter.getUser().equals(AppData.INSTANCE.getLoggedUser().getLogin())){
//                menu.findItem(R.id.action_filter_public).setVisible(false);
//                menu.findItem(R.id.action_filter_private).setVisible(false);
//            }
//        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFragmentShowed() {
        super.onFragmentShowed();
        if(mPresenter != null) mPresenter.prepareLoadData();
    }

    @Override
    public void onDrawerSelected(@NonNull NavigationView navView, @NonNull MenuItem item) {
        RepositoriesFilter filter = RepositoriesFilter.generateFromDrawer(navView);
        mPresenter.loadRepositories(filter);
    }

    public void setForksSort(String sort) {
        mPresenter.setForksSort(sort);
    }

    @Override
    public void onLanguageUpdate(TrendingLanguage language) {
        if(mPresenter != null){
            mPresenter.setLanguage(language);
            mPresenter.setLoaded(false);
            mPresenter.prepareLoadData();
        } else {
            getArguments().putParcelable("language", language);
        }
    }

    /**
     * For SEARCH-type fragments (e.g. CreatedActivity) where a filter change
     * (like language) needs to be baked into the query itself, rather than
     * applied by the presenter the way TRENDING's scraped fetch does.
     */
    public void onSearchModelUpdate(SearchModel searchModel) {
        if(mPresenter != null){
            mPresenter.setSearchModel(searchModel);
            mPresenter.setLoaded(false);
            mPresenter.prepareLoadData();
        } else {
            getArguments().putParcelable("searchModel", searchModel);
        }
    }

    /**
     * For the TOPICS_SEARCH type (MyTopicsActivity), pushed when the user's
     * selected topics or sort field change - mirrors onSearchModelUpdate.
     */
    public void onTopicsSearchUpdate(ArrayList<String> topicSlugs, String sort) {
        if(mPresenter != null){
            mPresenter.setTopicsSearchParams(topicSlugs, sort);
            mPresenter.setLoaded(false);
            // Use onRefresh() rather than prepareLoadData() directly so the
            // fragment's own page counter resets to 1 too - otherwise a user
            // who'd already scrolled to page 3, then switched topics, would
            // have the next load-more request page 4 instead of 2, skipping
            // data (the presenter also independently resets its own
            // per-topic page tracking on this path - see
            // RepositoriesPresenter.searchMultiTopics()'s freshLoad param).
            onRefresh();
        } else {
            getArguments().putStringArrayList("topicSlugs", topicSlugs);
            getArguments().putString("sort", sort);
        }
    }

    /**
     * Pushed by TrendingActivity/CreatedActivity when the shared ignore-list
     * toggle flips - only Trending/Created-tab fragments react (ignoreListEligible).
     */
    public void onIgnoreListToggle() {
        if(mPresenter != null && mPresenter.isIgnoreListEligible()){
            mPresenter.setLoaded(false);
            onRefresh();
        }
    }

    @Override
    public boolean onItemMoved(int fromPosition, int toPosition) {
        return false;
    }

    /**
     * Only reachable for RepositoriesType.IGNORED (the Manage Ignore List
     * screen) - Trending/Created use IgnoreSwipeCallback.Listener instead,
     * which distinguishes swipe direction.
     */
    @Override
    public void onItemSwiped(int position, int direction) {
        Repository repository = adapter.getData().get(position);
        IgnoredRepoHelper.unignore(repository.getFullName());
        adapter.getData().remove(position);
        if (adapter.getData().size() == 0) {
            postNotifyDataSetChanged();
        } else {
            adapter.notifyItemRemoved(position);
        }
        Snackbar.make(recyclerView, String.format(getString(R.string.repo_unignored), repository.getFullName()),
                Snackbar.LENGTH_LONG)
                .setAction(R.string.undo, v -> {
                    IgnoredRepoHelper.ignore(repository);
                    int insertPos = Math.min(position, adapter.getData().size());
                    adapter.getData().add(insertPos, repository);
                    adapter.notifyItemInserted(insertPos);
                })
                .show();
    }

    @Override
    public void onSwipeToIgnore(int position) {
        Repository repository = adapter.getData().get(position);
        IgnoredRepoHelper.ignore(repository);
        if (PrefUtils.isIgnoreListApplied()) {
            adapter.getData().remove(position);
            adapter.notifyItemRemoved(position);
        } else {
            adapter.notifyItemChanged(position);
        }
        Snackbar.make(recyclerView, String.format(getString(R.string.repo_ignored), repository.getFullName()),
                Snackbar.LENGTH_LONG)
                .setAction(R.string.undo, v -> {
                    IgnoredRepoHelper.unignore(repository.getFullName());
                    if (PrefUtils.isIgnoreListApplied()) {
                        int insertPos = Math.min(position, adapter.getData().size());
                        adapter.getData().add(insertPos, repository);
                        adapter.notifyItemInserted(insertPos);
                    } else {
                        adapter.notifyItemChanged(position);
                    }
                })
                .show();
    }

    @Override
    public void onSwipeToUnignore(int position) {
        Repository repository = adapter.getData().get(position);
        IgnoredRepoHelper.unignore(repository.getFullName());
        adapter.notifyItemChanged(position);
    }

}
