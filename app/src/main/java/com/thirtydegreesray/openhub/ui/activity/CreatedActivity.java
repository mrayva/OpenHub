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
import com.thirtydegreesray.openhub.mvp.contract.ITrendingContract;
import com.thirtydegreesray.openhub.mvp.model.SearchModel;
import com.thirtydegreesray.openhub.mvp.model.TrendingLanguage;
import com.thirtydegreesray.openhub.mvp.presenter.TrendingPresenter;
import com.thirtydegreesray.openhub.ui.activity.base.PagerActivity;
import com.thirtydegreesray.openhub.ui.adapter.base.FragmentPagerModel;
import com.thirtydegreesray.openhub.ui.fragment.RepositoriesFragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Repos created in the last year, sorted by stars, with an optional language
 * filter. GitHub's own trending page has no yearly timeframe (only
 * daily/weekly/monthly), so this uses the real search API instead of
 * scraping: a "created:>{oneYearAgo}" query, same mechanism the Topic repo
 * lists already use.
 *
 * Reuses TrendingActivity's language-filter drawer wholesale (same end-drawer
 * layout/menu, same TrendingPresenter for the language list, same
 * RepositoriesFragment.LanguageUpdateListener hookup) since that UI is
 * generic and not actually trending-specific.
 */
public class CreatedActivity extends PagerActivity<TrendingPresenter>
        implements ITrendingContract.View {

    public static void show(@NonNull Context context) {
        Intent intent = new Intent(context, CreatedActivity.class);
        context.startActivity(intent);
    }

    private final int SORT_LANGUAGE_REQUEST_CODE = 100;
    private TrendingLanguage selectedLanguage;
    private SearchModel searchModel;

    @Override
    protected void initActivity() {
        super.initActivity();
        setEndDrawerEnable(true);
        searchModel = new SearchModel(SearchModel.SearchType.Repository, buildBaseQuery())
                .setSort("stars")
                .setDesc(true);
    }

    private String buildBaseQuery() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, -1);
        String oneYearAgo = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.getTime());
        return "created:>" + oneYearAgo;
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
        pagerAdapter.setPagerList(FragmentPagerModel.createCreatedPagerList(
                getActivity(), getFragments(), searchModel));
        tabLayout.setVisibility(View.GONE);
        viewPager.setAdapter(pagerAdapter);
        showFirstPager();
        initLanguagesDrawer();
        updateTitle();
    }

    private void updateTitle() {
        setToolbarTitle(getString(R.string.created), selectedLanguage.getName());
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
            updateTitle();
        }
    }

    @Override
    protected int getEndDrawerToggleMenuItemId() {
        return R.id.nav_languages;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_trending, menu);
        return true;
    }

    private void initLanguagesDrawer() {
        if (navViewEnd == null) return;
        updateLanguagesDrawer();
        View view = getLayoutInflater().inflate(R.layout.layout_trending_drawer_bottom, null);
        navViewEnd.addHeaderView(view);
        View editView = view.findViewById(R.id.language_edit_bn);
        editView.setOnClickListener(v -> {
            LanguagesEditorActivity.show(getActivity(),
                    LanguagesEditorActivity.LanguageEditorMode.Sort, SORT_LANGUAGE_REQUEST_CODE);
        });
    }

    private void updateLanguagesDrawer() {
        if (navViewEnd == null) return;
        updateEndDrawerContent(R.menu.drawer_menu_trending);
        java.util.ArrayList<TrendingLanguage> languages = mPresenter.getLanguagesFromLocal();
        Menu menu = navViewEnd.getMenu();
        for (TrendingLanguage language : languages) {
            menu.add(R.id.group_languages, language.getOrder(), language.getOrder(), language.getName());
        }
        menu.setGroupCheckable(R.id.group_languages, true, true);
        if (languages.contains(selectedLanguage)) {
            selectedLanguage = languages.get(languages.indexOf(selectedLanguage));
        } else {
            selectedLanguage = languages.get(0);
            notifyLanguageUpdate();
            updateTitle();
        }
        menu.findItem(selectedLanguage.getOrder()).setChecked(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == SORT_LANGUAGE_REQUEST_CODE) {
            updateLanguagesDrawer();
        }
    }

    private void notifyLanguageUpdate() {
        String slug = selectedLanguage.getSlug();
        String query = buildBaseQuery();
        if (slug != null && !slug.isEmpty() && !"unknown".equals(slug)) {
            query += " language:" + slug;
        }
        searchModel.setQuery(query);
        for (FragmentPagerModel fragmentPagerModel : pagerAdapter.getPagerList()) {
            Fragment fragment = fragmentPagerModel.getFragment();
            if (fragment instanceof RepositoriesFragment) {
                ((RepositoriesFragment) fragment).onSearchModelUpdate(searchModel);
            }
        }
    }

}
