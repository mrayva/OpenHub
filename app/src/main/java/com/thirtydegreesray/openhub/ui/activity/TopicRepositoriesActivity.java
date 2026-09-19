package com.thirtydegreesray.openhub.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.thirtydegreesray.dataautoaccess.annotation.AutoAccess;
import com.thirtydegreesray.openhub.AppApplication;
import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.inject.component.AppComponent;
import com.thirtydegreesray.openhub.mvp.model.TrendingLanguage;
import com.thirtydegreesray.openhub.ui.activity.base.PagerActivity;
import com.thirtydegreesray.openhub.ui.adapter.base.FragmentPagerModel;
import com.thirtydegreesray.openhub.ui.adapter.base.FragmentViewPagerAdapter;
import com.thirtydegreesray.openhub.ui.fragment.RepositoriesFragment;
import com.thirtydegreesray.openhub.util.BundleHelper;
import com.thirtydegreesray.openhub.util.TrendingLanguageHelper;

import java.util.ArrayList;

/**
 * Repos matching a single tapped topic slug (e.g. from a repo's Topics tab),
 * reusing the same topic-search pager as MyTopicsActivity but for one ad-hoc
 * topic instead of the user's persisted "my topics" selection.
 */
public class TopicRepositoriesActivity extends PagerActivity {

    public static void show(@NonNull Context context, @NonNull String topicSlug) {
        Intent intent = new Intent(context, TopicRepositoriesActivity.class);
        intent.putExtras(BundleHelper.builder().put("topicSlug", topicSlug).build());
        context.startActivity(intent);
    }

    private static final int SORT_LANGUAGE_REQUEST_CODE = 100;

    @AutoAccess String topicSlug;

    private ArrayList<String> topicSlugs;
    private TrendingLanguage selectedLanguage;
    private ArrayList<TrendingLanguage> languages;

    @Override
    protected void initActivity() {
        super.initActivity();
        pagerAdapter = new FragmentViewPagerAdapter(getSupportFragmentManager());
        setEndDrawerEnable(true);
    }

    @Override
    protected void setupActivityComponent(AppComponent appComponent) {

    }

    @Override
    protected int getContentView() {
        return R.layout.activity_view_pager_with_drawer;
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);

        // activity_view_pager_with_drawer.xml hardcodes a 200dp top margin on
        // the included appbar - every current user of this layout resets it
        // to 0 right after super.initView(), otherwise the toolbar renders
        // 200dp down the screen behind a dead black gap.
        com.google.android.material.appbar.AppBarLayout appBarLayout = findViewById(R.id.app_bar);
        androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams params =
                (androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams();
        params.setMargins(params.leftMargin, 0, params.rightMargin, params.bottomMargin);
        appBarLayout.setLayoutParams(params);

        setToolbarScrollAble(true);
        setToolbarBackEnable();
        setToolbarTitle(topicSlug);
        topicSlugs = new ArrayList<>();
        topicSlugs.add(topicSlug);
        pagerAdapter.setPagerList(FragmentPagerModel.createTopicsSearchPagerList(
                getActivity(), getFragments(), topicSlugs, "stars"));
        tabLayout.setVisibility(View.GONE);
        viewPager.setAdapter(pagerAdapter);
        showFirstPager();
        initLanguagesDrawer();
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
        TrendingLanguage curSelectedLanguage = languages.get(item.getOrder() - 1);
        if (!curSelectedLanguage.equals(selectedLanguage)) {
            selectedLanguage = curSelectedLanguage;
            notifyLanguageUpdate();
        }
    }

    @Override
    protected int getEndDrawerToggleMenuItemId() {
        return R.id.nav_languages;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_topic_language, menu);
        return true;
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
        languages = TrendingLanguageHelper.getLanguagesFromLocal(
                AppApplication.get().getAppComponent().getDaoSession(), getActivity());
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
        }
        menu.findItem(selectedLanguage.getOrder()).setChecked(true);
    }

    private void notifyLanguageUpdate() {
        for (FragmentPagerModel fragmentPagerModel : pagerAdapter.getPagerList()) {
            Fragment fragment = fragmentPagerModel.getFragment();
            if (fragment instanceof RepositoriesFragment) {
                ((RepositoriesFragment) fragment).onTopicsSearchUpdate(topicSlugs, "stars", selectedLanguage);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SORT_LANGUAGE_REQUEST_CODE && resultCode == RESULT_OK) {
            updateLanguagesDrawer();
        }
    }

}
