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
import com.thirtydegreesray.openhub.mvp.model.filter.TrendingSince;
import com.thirtydegreesray.openhub.mvp.presenter.TrendingPresenter;
import com.thirtydegreesray.openhub.ui.activity.base.PagerActivity;
import com.thirtydegreesray.openhub.ui.adapter.base.FragmentPagerModel;
import com.thirtydegreesray.openhub.ui.fragment.RepositoriesFragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Repos created in the last day/week/month/year, sorted by stars, with an
 * optional language filter. GitHub's own trending page has no such
 * "created in the last N" timeframes (only daily/weekly/monthly *trending*),
 * so this uses the real search API instead of scraping: a
 * "created:>{dateOffset}" query, same mechanism the Topic repo lists already
 * use.
 *
 * Reuses TrendingActivity's language-filter drawer wholesale (same end-drawer
 * layout/menu, same TrendingPresenter for the language list, same tab
 * structure/pager pattern) since that UI is generic and not actually
 * trending-specific.
 */
public class CreatedActivity extends PagerActivity<TrendingPresenter>
        implements ITrendingContract.View {

    public static void show(@NonNull Context context) {
        Intent intent = new Intent(context, CreatedActivity.class);
        context.startActivity(intent);
    }

    private final int SORT_LANGUAGE_REQUEST_CODE = 100;
    private TrendingLanguage selectedLanguage;
    private SearchModel dailySearchModel;
    private SearchModel weeklySearchModel;
    private SearchModel monthlySearchModel;
    private SearchModel yearlySearchModel;

    @Override
    protected void initActivity() {
        super.initActivity();
        setEndDrawerEnable(true);
        dailySearchModel = newSearchModel(TrendingSince.Daily);
        weeklySearchModel = newSearchModel(TrendingSince.Weekly);
        monthlySearchModel = newSearchModel(TrendingSince.Monthly);
        yearlySearchModel = newSearchModel(TrendingSince.Yearly);
    }

    private SearchModel newSearchModel(TrendingSince since) {
        return new SearchModel(SearchModel.SearchType.Repository, buildBaseQuery(since))
                .setSort("stars")
                .setDesc(true);
    }

    private String buildBaseQuery(TrendingSince since) {
        Calendar calendar = Calendar.getInstance();
        switch (since) {
            case Daily:
                calendar.add(Calendar.DAY_OF_YEAR, -1);
                break;
            case Weekly:
                calendar.add(Calendar.WEEK_OF_YEAR, -1);
                break;
            case Monthly:
                calendar.add(Calendar.MONTH, -1);
                break;
            case Yearly:
            default:
                calendar.add(Calendar.YEAR, -1);
                break;
        }
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.getTime());
        return "created:>" + date;
    }

    /**
     * SearchService's "q" param is sent with @Query(encoded = true), so it is
     * never percent-encoded by Retrofit/OkHttp. Language slugs like "c++" or
     * "c#" contain characters (+, #) that are legal-but-meaningful in a URL
     * query component - a literal "+" is read back by GitHub as a space - so
     * they must be percent-encoded by hand before being appended, otherwise
     * "language:c++" silently becomes "language:c".
     */
    private String encodeLanguageSlug(String slug) {
        try {
            return java.net.URLEncoder.encode(slug, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return slug;
        }
    }

    private SearchModel getSearchModel(TrendingSince since) {
        switch (since) {
            case Daily:
                return dailySearchModel;
            case Weekly:
                return weeklySearchModel;
            case Monthly:
                return monthlySearchModel;
            case Yearly:
            default:
                return yearlySearchModel;
        }
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

        com.google.android.material.appbar.AppBarLayout appBarLayout = findViewById(R.id.app_bar);
        androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams params =
                (androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams();
        params.setMargins(params.leftMargin, 0, params.rightMargin, params.bottomMargin);
        appBarLayout.setLayoutParams(params);

        setToolbarScrollAble(true);
        setToolbarBackEnable();
        pagerAdapter.setPagerList(FragmentPagerModel.createCreatedPagerList(
                getActivity(), getFragments(), dailySearchModel, weeklySearchModel,
                monthlySearchModel, yearlySearchModel));
        tabLayout.setVisibility(View.VISIBLE);
        tabLayout.setupWithViewPager(viewPager);
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
        return 4;
    }

    @Override
    protected int getFragmentPosition(Fragment fragment) {
        if (fragment instanceof RepositoriesFragment) {
            TrendingSince since = null;
            Object obj = fragment.getArguments().get("since");
            if (obj instanceof TrendingSince)
                since = (TrendingSince) obj;

            if (since == null) {
                return -1;
            } else if (since.equals(TrendingSince.Daily)) {
                return 0;
            } else if (since.equals(TrendingSince.Weekly)) {
                return 1;
            } else if (since.equals(TrendingSince.Monthly)) {
                return 2;
            } else if (since.equals(TrendingSince.Yearly)) {
                return 3;
            } else {
                return -1;
            }
        } else
            return -1;
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
        boolean hasLanguage = slug != null && !slug.isEmpty()
                && !"unknown".equals(slug) && !"all".equals(slug);
        for (FragmentPagerModel fragmentPagerModel : pagerAdapter.getPagerList()) {
            Fragment fragment = fragmentPagerModel.getFragment();
            if (fragment instanceof RepositoriesFragment) {
                TrendingSince since = null;
                Object obj = fragment.getArguments().get("since");
                if (obj instanceof TrendingSince) since = (TrendingSince) obj;
                if (since == null) continue;

                SearchModel searchModel = getSearchModel(since);
                String query = buildBaseQuery(since);
                if (hasLanguage) {
                    query += " language:" + encodeLanguageSlug(slug);
                }
                searchModel.setQuery(query);
                ((RepositoriesFragment) fragment).onSearchModelUpdate(searchModel);
            }
        }
    }

}
