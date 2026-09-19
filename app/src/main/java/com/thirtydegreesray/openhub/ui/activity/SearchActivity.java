

package com.thirtydegreesray.openhub.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.view.MenuItemCompat;
import androidx.appcompat.widget.SearchView;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import androidx.appcompat.app.AlertDialog;

import com.thirtydegreesray.dataautoaccess.annotation.AutoAccess;
import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.common.AppEventBus;
import com.thirtydegreesray.openhub.common.Event;
import com.thirtydegreesray.openhub.inject.component.AppComponent;
import com.thirtydegreesray.openhub.inject.component.DaggerActivityComponent;
import com.thirtydegreesray.openhub.inject.module.ActivityModule;
import com.thirtydegreesray.openhub.mvp.contract.ISearchContract;
import com.thirtydegreesray.openhub.mvp.model.SearchModel;
import com.thirtydegreesray.openhub.mvp.model.TrendingLanguage;
import com.thirtydegreesray.openhub.mvp.presenter.SearchPresenter;
import com.thirtydegreesray.openhub.ui.activity.base.PagerActivity;
import com.thirtydegreesray.openhub.ui.adapter.base.FragmentPagerModel;
import com.thirtydegreesray.openhub.ui.fragment.RepositoriesFragment;
import com.thirtydegreesray.openhub.ui.fragment.UserListFragment;
import com.thirtydegreesray.openhub.util.StringUtils;
import com.thirtydegreesray.openhub.util.ViewUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ThirtyDegreesRay on 2017/8/25 10:54:04
 */

public class SearchActivity extends PagerActivity<SearchPresenter>
        implements ISearchContract.View,
        MenuItemCompat.OnActionExpandListener,
        SearchView.OnQueryTextListener {

    public static void show(@NonNull Context context) {
        Intent intent = new Intent(context, SearchActivity.class);
        context.startActivity(intent);
    }

    private final Map<Integer, List<Integer>> MENU_ID_MAP = new HashMap<>();
    private static final int SORT_LANGUAGE_REQUEST_CODE = 100;

    @AutoAccess boolean isInputMode = true;
    @AutoAccess String[] sortInfos;
    private TrendingLanguage selectedLanguage;

    @Override
    protected void initActivity() {
        super.initActivity();
        setEndDrawerEnable(true);
        MENU_ID_MAP.put(0, SearchModel.REPO_SORT_ID_LIST);
        MENU_ID_MAP.put(1, SearchModel.USER_SORT_ID_LIST);
    }

    @Override
    protected void setupActivityComponent(AppComponent appComponent) {
        DaggerActivityComponent.builder()
                .appComponent(appComponent)
                .activityModule(new ActivityModule(getActivity()))
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

        // activity_view_pager_with_drawer.xml hardcodes a 200dp top margin on
        // the included appbar - every current user of this layout
        // (Trending/Created/Issues/My Topics) resets it to 0 right after
        // super.initView(), otherwise the toolbar renders 200dp down the
        // screen behind a dead black gap.
        com.google.android.material.appbar.AppBarLayout appBarLayout = findViewById(R.id.app_bar);
        androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams params =
                (androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams();
        params.setMargins(params.leftMargin, 0, params.rightMargin, params.bottomMargin);
        appBarLayout.setLayoutParams(params);

        setToolbarScrollAble(true);
        setToolbarBackEnable();
        setToolbarTitle(getString(R.string.search));
        initLanguagesDrawer();
        if(sortInfos == null) {
            sortInfos = new String[]{
                    getString(R.string.best_match), getString(R.string.best_match)
            };
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView =
                (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnQueryTextListener(this);
        searchView.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        searchView.setQuery(mPresenter.getSearchModels().get(0).getQuery(), false);
        if (isInputMode) {
            MenuItemCompat.expandActionView(searchItem);
        } else {
            MenuItemCompat.collapseActionView(searchItem);
        }
        MenuItemCompat.setOnActionExpandListener(searchItem, this);

        AutoCompleteTextView autoCompleteTextView = searchView
                .findViewById(androidx.appcompat.R.id.search_src_text);
        autoCompleteTextView.setThreshold(0);
        autoCompleteTextView.setAdapter(new SearchRecordAdapter(this,
                R.layout.layout_item_simple_list, mPresenter.getSearchRecordList()));
        autoCompleteTextView.setDropDownBackgroundDrawable(new ColorDrawable(ViewUtils.getWindowBackground(getActivity())));
        autoCompleteTextView.setOnItemClickListener((parent, view, position, id) -> {
            onQueryTextSubmit(parent.getAdapter().getItem(position).toString());
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_sort) {
            filterSortMenu(item);
        } else if (SearchModel.SORT_ID_LIST.contains(item.getItemId())) {
            int page = viewPager.getCurrentItem();
            postSearchEvent(mPresenter.getSortModel(page, item.getItemId()));
            sortInfos[page] = item.getTitle().toString();
            setSubTitle(page);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (isInputMode) {
            menu.findItem(R.id.action_info).setVisible(false);
            menu.findItem(R.id.action_sort).setVisible(false);
            menu.findItem(R.id.nav_languages).setVisible(false);
            SearchView searchView = (SearchView) MenuItemCompat.
                    getActionView(menu.findItem(R.id.action_search));
            searchView.setQuery(mPresenter.getSearchModels().get(0).getQuery(), false);
        } else {
            menu.findItem(R.id.action_info).setVisible(false);
            menu.findItem(R.id.action_sort).setVisible(pagerAdapter.getCount() != 0);
            // Language only makes sense for the Repositories tab (page 0).
            menu.findItem(R.id.nav_languages).setVisible(
                    pagerAdapter.getCount() != 0 && viewPager.getCurrentItem() == 0);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        isInputMode = true;
        invalidateOptionsMenu();
        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        isInputMode = false;
        invalidateOptionsMenu();
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if (StringUtils.isBlank(query)) {
            showWarningToast(getString(R.string.invalid_query));
            return true;
        }
        isInputMode = false;
        invalidateOptionsMenu();
        search(query);
        setSubTitle(viewPager.getCurrentItem());
        mPresenter.addSearchRecord(query);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    private void filterSortMenu(MenuItem item) {
        int index = viewPager.getCurrentItem();
        List<Integer> idList = MENU_ID_MAP.get(index);
        for (Integer id : SearchModel.SORT_ID_LIST) {
            item.getSubMenu().findItem(id).setVisible(idList.contains(id));
        }
    }

    private void search(String query) {
        if (pagerAdapter.getCount() == 0) {
            ArrayList<SearchModel> models = mPresenter.getQueryModels(query);
            ArrayList<SearchModel> initialModels = new ArrayList<>();
            for (SearchModel model : models) initialModels.add(withLanguageFilter(model));
            pagerAdapter.setPagerList(FragmentPagerModel.
                    createSearchPagerList(getActivity(), initialModels, getFragments()));
            tabLayout.setVisibility(View.VISIBLE);
            tabLayout.setupWithViewPager(viewPager);
            viewPager.setAdapter(pagerAdapter);
            showFirstPager();

            // Set margins on tabs to increase spacing
            for (int i = 0; i < tabLayout.getTabCount(); i++) {
                View tab = ((ViewGroup) tabLayout.getChildAt(0)).getChildAt(i);
                ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) tab.getLayoutParams();
                p.setMargins(12, 0, 12, 0);
                tab.setLayoutParams(p);
            }
        } else {
            for (SearchModel searchModel : mPresenter.getQueryModels(query)) {
                postSearchEvent(searchModel);
            }
        }
    }

    /**
     * searchModel here (and everywhere else this fires from - sort changes,
     * a resubmitted query) always carries the RAW query text
     * (mPresenter.getSearchModels() is never itself mutated with the
     * language suffix - see withLanguageFilter()), so wrapping every post
     * through here is the one place that needs to remember to reapply the
     * active language filter.
     */
    private void postSearchEvent(SearchModel searchModel) {
        AppEventBus.INSTANCE.getEventBus().post(new Event.SearchEvent(withLanguageFilter(searchModel)));
    }

    private void setSubTitle(int page) {
        setToolbarSubTitle(mPresenter.getSearchModels().get(0).getQuery() + "/" + sortInfos[page]);
    }

    @Override
    public void onPageSelected(int position) {
        super.onPageSelected(position);
        setSubTitle(position);
        invalidateOptionsMenu();
    }

    @Override
    public int getPagerSize() {
        return 2;
    }

    @Override
    public void showSearches(ArrayList<SearchModel> searchModels) {
        search(searchModels.get(0).getQuery());
        setSubTitle(0);
    }

    @Override
    protected int getFragmentPosition(Fragment fragment) {
        if(fragment instanceof RepositoriesFragment){
            return 0;
        }else if(fragment instanceof UserListFragment){
            return 1;
        }else
            return -1;
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
        }
        menu.findItem(selectedLanguage.getOrder()).setChecked(true);
    }

    /**
     * Only the Repositories tab (page 0) has anything to re-fetch - the
     * Users tab's results are untouched by a language change, so no event is
     * posted for it.
     */
    private void notifyLanguageUpdate() {
        if (pagerAdapter.getCount() == 0) return;
        SearchModel repoModel = mPresenter.getSearchModels().get(0);
        if (StringUtils.isBlank(repoModel.getQuery())) return;
        postSearchEvent(repoModel);
    }

    /**
     * Never mutates the passed-in model - mPresenter.getSearchModels() must
     * keep holding the RAW, user-typed query (it backs the SearchView
     * prefill, the subtitle, and search history), so the language-augmented
     * query is built fresh into a separate SearchModel each time it's
     * needed, same as CreatedActivity's language filter does for its own
     * query building.
     */
    private SearchModel withLanguageFilter(SearchModel model) {
        if (!SearchModel.SearchType.Repository.equals(model.getType())) return model;
        String langSlug = selectedLanguage == null ? null : selectedLanguage.getSlug();
        boolean hasLanguage = langSlug != null && !langSlug.isEmpty()
                && !"unknown".equals(langSlug) && !"all".equals(langSlug);
        if (!hasLanguage) return model;
        String query = model.getQuery() + " language:" + encodeLanguageSlug(langSlug);
        return new SearchModel(model.getType(), query)
                .setSort(model.getSort()).setDesc(model.isDesc());
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SORT_LANGUAGE_REQUEST_CODE && resultCode == RESULT_OK) {
            updateLanguagesDrawer();
        }
    }

    private class SearchRecordAdapter extends ArrayAdapter<String> {

        public SearchRecordAdapter(@NonNull Context context, int resource, @NonNull List<String> objects) {
            super(context, resource, objects);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            view.setOnLongClickListener(v -> {
                String record = getItem(position);
                if (record != null) {
                    new AlertDialog.Builder(getContext())
                            .setTitle(R.string.warning_dialog_tile)
                            .setMessage(R.string.delete_search_record_confirm)
                            .setPositiveButton(R.string.ok, (dialog, which) -> {
                                mPresenter.removeSearchRecord(record);
                                clear();
                                addAll(mPresenter.getSearchRecordList());
                                notifyDataSetChanged();
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .show();
                }
                return true;
            });
            return view;
        }
    }

}
