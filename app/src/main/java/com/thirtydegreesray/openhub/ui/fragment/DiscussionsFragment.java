package com.thirtydegreesray.openhub.ui.fragment;

import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.inject.component.AppComponent;
import com.thirtydegreesray.openhub.inject.component.DaggerFragmentComponent;
import com.thirtydegreesray.openhub.inject.module.FragmentModule;
import com.thirtydegreesray.openhub.mvp.contract.IDiscussionsContract;
import com.thirtydegreesray.openhub.mvp.model.graphql.Discussion;
import com.thirtydegreesray.openhub.mvp.model.graphql.DiscussionCategory;
import com.thirtydegreesray.openhub.mvp.presenter.DiscussionsPresenter;
import com.thirtydegreesray.openhub.ui.activity.DiscussionActivity;
import com.thirtydegreesray.openhub.ui.adapter.DiscussionsAdapter;
import com.thirtydegreesray.openhub.ui.fragment.base.ListFragment;
import com.thirtydegreesray.openhub.util.BundleHelper;
import com.thirtydegreesray.openhub.util.StringUtils;

import java.util.ArrayList;

public class DiscussionsFragment extends ListFragment<DiscussionsPresenter, DiscussionsAdapter>
        implements IDiscussionsContract.View {

    public static DiscussionsFragment create(@NonNull String owner, @NonNull String repo) {
        DiscussionsFragment fragment = new DiscussionsFragment();
        fragment.setArguments(BundleHelper.builder().put("owner", owner).put("repo", repo).build());
        return fragment;
    }

    private ArrayList<DiscussionCategory> categories;
    private DiscussionCategory selectedCategory;

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
    protected void initFragment(Bundle savedInstanceState) {
        super.initFragment(savedInstanceState);
        setLoadMoreEnable(true);
        setAutoJudgeCanLoadMoreEnable(false);
        setHasOptionsMenu(true);
    }

    @Override
    protected void onReLoadData() {
        mPresenter.loadDiscussions(1, true);
    }

    @Override
    protected void onLoadMore(int page) {
        super.onLoadMore(page);
        mPresenter.loadDiscussions(page, false);
    }

    @Override
    protected String getEmptyTip() {
        return getString(R.string.no_discussions);
    }

    @Override
    public void showDiscussions(ArrayList<Discussion> discussions) {
        adapter.setData(discussions);
        postNotifyDataSetChanged();
    }

    @Override
    public void showCategories(ArrayList<DiscussionCategory> categories) {
        this.categories = categories;
    }

    @Override
    public void onItemClick(int position, @NonNull View view) {
        super.onItemClick(position, view);
        Discussion discussion = adapter.getData().get(position);
        DiscussionActivity.show(getActivity(), mPresenter.getOwner(), mPresenter.getRepo(),
                discussion.getNumber());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_discussions, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_filter_category) {
            showFilterDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void reload() {
        mPresenter.loadDiscussions(1, true);
    }

    private void showFilterDialog() {
        if (StringUtils.isBlankList(categories)) return;
        String[] labels = new String[categories.size() + 1];
        labels[0] = getString(R.string.all_categories);
        for (int i = 0; i < categories.size(); i++) {
            DiscussionCategory category = categories.get(i);
            labels[i + 1] = StringUtils.isBlank(category.getEmoji()) ?
                    category.getName() : category.getEmoji().concat(" ").concat(category.getName());
        }
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.filter_by_category)
                .setItems(labels, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        selectedCategory = which == 0 ? null : categories.get(which - 1);
                        mPresenter.filterByCategory(selectedCategory);
                    }
                })
                .show();
    }

}
