package com.thirtydegreesray.openhub.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.inject.component.AppComponent;
import com.thirtydegreesray.openhub.inject.component.DaggerActivityComponent;
import com.thirtydegreesray.openhub.inject.module.ActivityModule;
import com.thirtydegreesray.openhub.mvp.contract.IMyTopicsContract;
import com.thirtydegreesray.openhub.mvp.model.TrendingLanguage;
import com.thirtydegreesray.openhub.mvp.presenter.MyTopicsPresenter;
import com.thirtydegreesray.openhub.ui.activity.base.PagerActivity;
import com.thirtydegreesray.openhub.ui.adapter.base.FragmentPagerModel;
import com.thirtydegreesray.openhub.ui.fragment.RepositoriesFragment;
import com.thirtydegreesray.openhub.util.PrefUtils;

import java.util.ArrayList;

/**
 * Repos matching any of the user's selected topics (an OR/union, not GitHub's
 * native topic:a topic:b AND), sorted by stars or last-updated. Multi-topic
 * search fires one query per topic and merges client-side - see
 * RepositoriesPresenter.searchMultiTopics() for why (GitHub's search API
 * rejects OR between qualifiers). Topic selection/management lives in
 * TopicsEditorActivity; this screen just shows the results and lets the user
 * jump there or change the sort field.
 */
public class MyTopicsActivity extends PagerActivity<MyTopicsPresenter>
        implements IMyTopicsContract.View {

    public static void show(@NonNull Context context) {
        Intent intent = new Intent(context, MyTopicsActivity.class);
        context.startActivity(intent);
    }

    private static final int MANAGE_TOPICS_REQUEST_CODE = 100;
    private static final int SORT_LANGUAGE_REQUEST_CODE = 101;

    private ArrayList<String> topicSlugs;
    private String sort;
    private TrendingLanguage selectedLanguage;

    @Override
    protected void initActivity() {
        super.initActivity();
        setEndDrawerEnable(true);
        sort = PrefUtils.getTopicsSearchSort();
        mPresenter.seedDefaultTopicsIfEmpty();
    }

    @Override
    protected void setupActivityComponent(AppComponent appComponent) {
        DaggerActivityComponent.builder()
                .appComponent(appComponent)
                .activityModule(new ActivityModule(this))
                .build()
                .inject(this);
    }

    @Nullable
    @Override
    protected int getContentView() {
        return R.layout.activity_view_pager_with_drawer;
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        setToolbarScrollAble(true);
        setToolbarBackEnable();
        topicSlugs = mPresenter.getSelectedTopicSlugs();
        pagerAdapter.setPagerList(FragmentPagerModel.createTopicsSearchPagerList(
                getActivity(), getFragments(), topicSlugs, sort));
        tabLayout.setVisibility(View.GONE);
        viewPager.setAdapter(pagerAdapter);
        showFirstPager();
        initLanguagesDrawer();
        updateTitle();
    }

    private void updateTitle() {
        String subtitle;
        if (topicSlugs.isEmpty()) {
            subtitle = getString(R.string.no_topics_selected);
        } else if (topicSlugs.size() <= 3) {
            subtitle = android.text.TextUtils.join(", ", topicSlugs);
        } else {
            subtitle = String.format(getString(R.string.topics_selected_count), topicSlugs.size());
        }
        setToolbarTitle(getString(R.string.my_topics), subtitle);
    }

    @Override
    public int getPagerSize() {
        return 1;
    }

    @Override
    protected int getFragmentPosition(Fragment fragment) {
        return fragment instanceof RepositoriesFragment ? 0 : -1;
    }

    @Override
    protected void onNavItemSelected(@NonNull MenuItem item, boolean isStartDrawer) {
        super.onNavItemSelected(item, isStartDrawer);
        TrendingLanguage curSelectedLanguage = mPresenter.getLanguages().get(item.getOrder() - 1);
        if (!curSelectedLanguage.equals(selectedLanguage)) {
            selectedLanguage = curSelectedLanguage;
            notifyLanguageUpdate();
        }
    }

    @Override
    protected int getEndDrawerToggleMenuItemId() {
        return R.id.nav_languages;
    }

    private void initLanguagesDrawer() {
        if (navViewEnd == null) return;
        updateLanguagesDrawer();
        View view = getLayoutInflater().inflate(R.layout.layout_trending_drawer_bottom, null);
        navViewEnd.addHeaderView(view);
        View editView = view.findViewById(R.id.language_edit_bn);
        editView.setOnClickListener(v -> LanguagesEditorActivity.show(getActivity(),
                LanguagesEditorActivity.LanguageEditorMode.Sort, SORT_LANGUAGE_REQUEST_CODE));
    }

    private void updateLanguagesDrawer() {
        if (navViewEnd == null) return;
        updateEndDrawerContent(R.menu.drawer_menu_trending);
        ArrayList<TrendingLanguage> languages = mPresenter.getLanguagesFromLocal();
        Menu menu = navViewEnd.getMenu();
        for (TrendingLanguage language : languages) {
            menu.add(R.id.group_languages, language.getOrder(), language.getOrder(), language.getName());
        }
        menu.setGroupCheckable(R.id.group_languages, true, true);
        if (languages.contains(selectedLanguage)) {
            //maybe list size changed, and order changed too
            selectedLanguage = languages.get(languages.indexOf(selectedLanguage));
        } else {
            selectedLanguage = languages.get(0);
            notifyLanguageUpdate();
        }
        menu.findItem(selectedLanguage.getOrder()).setChecked(true);
    }

    private void notifyLanguageUpdate() {
        for (FragmentPagerModel fragmentPagerModel : pagerAdapter.getPagerList()) {
            Fragment fragment = fragmentPagerModel.getFragment();
            if (fragment instanceof RepositoriesFragment) {
                ((RepositoriesFragment) fragment).onTopicsSearchUpdate(topicSlugs, sort, selectedLanguage);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_topics_search, menu);
        int sortItemId;
        if ("updated".equals(sort)) {
            sortItemId = R.id.action_sort_updated;
        } else if ("created".equals(sort)) {
            sortItemId = R.id.action_sort_created;
        } else {
            sortItemId = R.id.action_sort_stars;
        }
        menu.findItem(sortItemId).setChecked(true);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_apply_ignore_list).setChecked(PrefUtils.isIgnoreListApplied());
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_manage_topics) {
            TopicsEditorActivity.show(getActivity(), MANAGE_TOPICS_REQUEST_CODE);
            return true;
        } else if (id == R.id.action_sort_stars || id == R.id.action_sort_updated
                || id == R.id.action_sort_created) {
            item.setChecked(true);
            if (id == R.id.action_sort_updated) {
                sort = "updated";
            } else if (id == R.id.action_sort_created) {
                sort = "created";
            } else {
                sort = "stars";
            }
            PrefUtils.setTopicsSearchSort(sort);
            reloadTopicsSearch();
            return true;
        } else if (id == R.id.action_apply_ignore_list) {
            boolean applied = !item.isChecked();
            item.setChecked(applied);
            PrefUtils.set(PrefUtils.IGNORE_LIST_APPLIED, applied);
            notifyIgnoreListToggle();
            return true;
        } else if (id == R.id.action_manage_ignore_list) {
            IgnoredReposActivity.show(getActivity());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void notifyIgnoreListToggle() {
        for (FragmentPagerModel fragmentPagerModel : pagerAdapter.getPagerList()) {
            Fragment fragment = fragmentPagerModel.getFragment();
            if (fragment instanceof RepositoriesFragment) {
                ((RepositoriesFragment) fragment).onIgnoreListToggle();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MANAGE_TOPICS_REQUEST_CODE && resultCode == RESULT_OK) {
            reloadTopicsSearch();
        } else if (requestCode == SORT_LANGUAGE_REQUEST_CODE && resultCode == RESULT_OK) {
            updateLanguagesDrawer();
        }
    }

    private void reloadTopicsSearch() {
        topicSlugs = mPresenter.getSelectedTopicSlugs();
        updateTitle();
        for (FragmentPagerModel fragmentPagerModel : pagerAdapter.getPagerList()) {
            Fragment fragment = fragmentPagerModel.getFragment();
            if (fragment instanceof RepositoriesFragment) {
                ((RepositoriesFragment) fragment).onTopicsSearchUpdate(topicSlugs, sort, selectedLanguage);
            }
        }
    }

}
