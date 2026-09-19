

package com.thirtydegreesray.openhub.ui.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.thirtydegreesray.dataautoaccess.annotation.AutoAccess;
import com.thirtydegreesray.openhub.AppApplication;
import com.thirtydegreesray.openhub.AppConfig;
import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.R2;
import com.thirtydegreesray.openhub.mvp.contract.base.IBaseContract;
import com.thirtydegreesray.openhub.mvp.model.Collection;
import com.thirtydegreesray.openhub.mvp.model.Topic;
import com.thirtydegreesray.openhub.mvp.model.TrendingLanguage;
import com.thirtydegreesray.openhub.mvp.model.filter.RepositoriesFilter;
import com.thirtydegreesray.openhub.ui.activity.base.SingleFragmentActivity;
import com.thirtydegreesray.openhub.ui.fragment.RepositoriesFragment;
import com.thirtydegreesray.openhub.ui.fragment.base.OnDrawerSelectedListener;
import com.thirtydegreesray.openhub.util.AppOpener;
import com.thirtydegreesray.openhub.util.BundleHelper;
import com.thirtydegreesray.openhub.util.StringUtils;
import com.thirtydegreesray.openhub.util.TrendingLanguageHelper;

import java.util.ArrayList;

/**
 * Created by ThirtyDegreesRay on 2017/8/23 18:15:40
 */

public class RepoListActivity extends SingleFragmentActivity<IBaseContract.Presenter, RepositoriesFragment> {

    public static void show(@NonNull Context context,
                            @NonNull RepositoriesFragment.RepositoriesType type,
                            @NonNull String user){
        Intent intent = new Intent(context, RepoListActivity.class);
        intent.putExtras(BundleHelper.builder().put("type", type).put("user", user).build());
        context.startActivity(intent);
    }

    public static void showCollection(@NonNull Context context, @NonNull Collection collection){
        Intent intent = new Intent(context, RepoListActivity.class);
        intent.putExtras(BundleHelper.builder()
                .put("type", RepositoriesFragment.RepositoriesType.COLLECTION)
                .put("collection", collection)
                .build());
        context.startActivity(intent);
    }

    public static void showTopic(@NonNull Context context, @NonNull Topic topic){
        Intent intent = new Intent(context, RepoListActivity.class);
        intent.putExtras(BundleHelper.builder()
                .put("type", RepositoriesFragment.RepositoriesType.TOPIC)
                .put("topic", topic)
                .build());
        context.startActivity(intent);
    }

    public static void showForks(@NonNull Context context,
                            @NonNull String user, @NonNull String repo){
        Intent intent = new Intent(context, RepoListActivity.class);
        intent.putExtras(BundleHelper.builder()
                .put("type", RepositoriesFragment.RepositoriesType.FORKS)
                .put("user", user)
                .put("repo", repo)
                .build());
        context.startActivity(intent);
    }

    private static final int SORT_LANGUAGE_REQUEST_CODE = 100;

    @AutoAccess RepositoriesFragment.RepositoriesType type;
    @AutoAccess String user;
    @AutoAccess String repo;
    @AutoAccess Collection collection;
    @AutoAccess Topic topic;

    private OnDrawerSelectedListener listener;
    private TrendingLanguage selectedLanguage;
    private ArrayList<TrendingLanguage> topicLanguages;

    @Override
    protected int getContentView() {
        return R.layout.activity_single_fragment_with_drawer;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.nav_sort && RepositoriesFragment.RepositoriesType.FORKS.equals(type)){
            showForksSortDialog();
            return true;
        }
        if(item.getItemId() == R.id.action_open_in_browser){
            String url = null;
            if(RepositoriesFragment.RepositoriesType.COLLECTION.equals(type)){
                url = AppConfig.GITHUB_BASE_URL.concat("collections/").concat(collection.getId());
            } else if(RepositoriesFragment.RepositoriesType.TOPIC.equals(type)){
                url = AppConfig.GITHUB_BASE_URL.concat("topics/").concat(topic.getId());
            }
            if(url != null){
                AppOpener.openInCustomTabsOrBrowser(getActivity(), url);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected RepositoriesFragment getFragment() {
        return super.getFragment();
    }

    @Override
    protected void onNavItemSelected(@NonNull MenuItem item, boolean isStartDrawer) {
        super.onNavItemSelected(item, isStartDrawer);
        if (isTopic()) {
            TrendingLanguage curSelectedLanguage = topicLanguages.get(item.getOrder() - 1);
            if (!curSelectedLanguage.equals(selectedLanguage)) {
                selectedLanguage = curSelectedLanguage;
                getFragment().onLanguageUpdate(selectedLanguage);
            }
            return;
        }
        listener.onDrawerSelected(navViewEnd, item);
    }

    @Override
    protected boolean isEndDrawerMultiSelect() {
        // Owned/Public's drawer has several independent checkable groups
        // (Type/Kind/Sort/Language) open at once, so each selection must be
        // applied without closing the drawer or auto-unchecking siblings.
        // Topic's language drawer is a single flat group - plain single-select
        // behavior (auto-check + close), same as Trending/Created/My Topics/
        // Search's language drawers.
        return isFilterEnable();
    }

    @Override
    protected int getEndDrawerToggleMenuItemId() {
        return isTopic() ? R.id.nav_languages : R.id.nav_sort;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(isFilterEnable()){
            getMenuInflater().inflate(R.menu.menu_sort, menu);
        } else if(RepositoriesFragment.RepositoriesType.FORKS.equals(type)){
            // Not the drawer-based filter used by Owned/Public - GitHub's
            // forks endpoint only supports 4 fixed sort values, so a plain
            // dialog (see showForksSortDialog()) is enough; the end drawer
            // stays disabled for this type (see isFilterEnable()).
            getMenuInflater().inflate(R.menu.menu_sort, menu);
        } else if(RepositoriesFragment.RepositoriesType.TOPIC.equals(type)){
            getMenuInflater().inflate(R.menu.menu_topic_repos, menu);
        } else if(RepositoriesFragment.RepositoriesType.COLLECTION.equals(type)){
            getMenuInflater().inflate(R.menu.menu_open_in_browser, menu);
        }
        return true;
    }



    @Override
    protected void initActivity() {
        super.initActivity();
        setEndDrawerEnable(isFilterEnable() || isTopic());
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        String title = getListTitle();
        String subTitle = StringUtils.isBlank(repo) ? user : user.concat("/").concat(repo);
        setToolbarTitle(title, subTitle);
        intiFilter();
        setToolbarScrollAble(true);
    }

    @Override
    protected RepositoriesFragment createFragment() {
        RepositoriesFragment fragment;
        if(RepositoriesFragment.RepositoriesType.COLLECTION.equals(type)){
            fragment = RepositoriesFragment.createForCollection(collection);
        } else if(RepositoriesFragment.RepositoriesType.TOPIC.equals(type)){
            fragment = RepositoriesFragment.createForTopic(topic);
        } else {
            fragment = RepositoriesFragment.RepositoriesType.FORKS.equals(type) ?
                    RepositoriesFragment.createForForks(user, repo) :
                    RepositoriesFragment.create(type, user);
            listener = fragment;
        }
        return fragment;
    }

    private String getListTitle(){
        if(type.equals(RepositoriesFragment.RepositoriesType.PUBLIC)){
            return getString(R.string.public_repositories);
        }else if(type.equals(RepositoriesFragment.RepositoriesType.STARRED)){
            return getString(R.string.starred_repositories);
        }else if(type.equals(RepositoriesFragment.RepositoriesType.FORKS)){
            return getString(R.string.forks);
        }else if(type.equals(RepositoriesFragment.RepositoriesType.COLLECTION)){
            return collection.getName();
        }else if(type.equals(RepositoriesFragment.RepositoriesType.TOPIC)){
            return topic.getName();
        }
        return getString(R.string.repositories);
    }

    private boolean isFilterEnable(){
        return RepositoriesFragment.RepositoriesType.OWNED.equals(type) ||
                RepositoriesFragment.RepositoriesType.PUBLIC.equals(type);
    }

    private boolean isTopic(){
        return RepositoriesFragment.RepositoriesType.TOPIC.equals(type);
    }

    private void intiFilter(){
        if(isFilterEnable()){
            updateEndDrawerContent(R.menu.menu_repositories_filter);
            RepositoriesFilter.initDrawer(navViewEnd, type);
            populateLanguageChooser();
        } else if(isTopic()){
            initTopicLanguageDrawer();
        }
    }

    /**
     * Fills in the "Language" submenu of menu_repositories_filter.xml with
     * the user's curated language list, same list/source Trending/Created/My
     * Topics/Search already share - just nested inside this drawer's
     * existing Type/Kind/Sort choosers instead of a standalone flat drawer,
     * since Owned/Public only have the one end-drawer to work with.
     * Client-side only (see RepositoriesPresenter.filterByLanguage()) -
     * GitHub's plain list-repos REST endpoints have no language param.
     */
    private void populateLanguageChooser(){
        if(navViewEnd == null) return;
        MenuItem languageChooserItem = navViewEnd.getMenu().findItem(R.id.nav_language_chooser);
        if(languageChooserItem == null || languageChooserItem.getSubMenu() == null) return;
        Menu subMenu = languageChooserItem.getSubMenu();
        ArrayList<TrendingLanguage> languages = TrendingLanguageHelper.getLanguagesFromLocal(
                AppApplication.get().getAppComponent().getDaoSession(), getActivity());
        for(TrendingLanguage language : languages){
            String slug = language.getSlug();
            // "All languages" is already nav_language_all; "Unknown languages"
            // has no equivalent here (a repo with no detected language just
            // won't match any specific entry, same net effect).
            if(StringUtils.isBlank(slug) || "unknown".equals(slug)) continue;
            subMenu.add(R.id.group_language_chooser, language.getOrder(), language.getOrder(), language.getName());
        }
    }

    private void initTopicLanguageDrawer(){
        if(navViewEnd == null) return;
        updateTopicLanguageDrawer();
        View view = getLayoutInflater().inflate(R.layout.layout_trending_drawer_bottom, null);
        navViewEnd.addHeaderView(view);
        View editView = view.findViewById(R.id.language_edit_bn);
        editView.setOnClickListener(v -> LanguagesEditorActivity.show(getActivity(),
                LanguagesEditorActivity.LanguageEditorMode.Sort, SORT_LANGUAGE_REQUEST_CODE));
    }

    private void updateTopicLanguageDrawer(){
        if(navViewEnd == null) return;
        updateEndDrawerContent(R.menu.drawer_menu_trending);
        topicLanguages = TrendingLanguageHelper.getLanguagesFromLocal(
                AppApplication.get().getAppComponent().getDaoSession(), getActivity());
        Menu menu = navViewEnd.getMenu();
        for(TrendingLanguage language : topicLanguages){
            menu.add(R.id.group_languages, language.getOrder(), language.getOrder(), language.getName());
        }
        menu.setGroupCheckable(R.id.group_languages, true, true);
        if(topicLanguages.contains(selectedLanguage)){
            //maybe list size changed, and order changed too
            selectedLanguage = topicLanguages.get(topicLanguages.indexOf(selectedLanguage));
        } else {
            selectedLanguage = topicLanguages.get(0);
        }
        menu.findItem(selectedLanguage.getOrder()).setChecked(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == SORT_LANGUAGE_REQUEST_CODE && resultCode == RESULT_OK){
            updateTopicLanguageDrawer();
        }
    }

    private void showForksSortDialog(){
        final String[] labels = {
                getString(R.string.recently_created), getString(R.string.previously_created),
                getString(R.string.most_stars), getString(R.string.most_watchers)
        };
        final String[] values = {"newest", "oldest", "stargazers", "watchers"};
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.sort)
                .setItems(labels, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        getFragment().setForksSort(values[which]);
                    }
                })
                .show();
    }

}
