package com.thirtydegreesray.openhub.ui.fragment;

import android.os.Bundle;
import androidx.annotation.NonNull;

import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.inject.component.AppComponent;
import com.thirtydegreesray.openhub.inject.component.DaggerFragmentComponent;
import com.thirtydegreesray.openhub.inject.module.FragmentModule;
import com.thirtydegreesray.openhub.mvp.contract.IDiscussionContract;
import com.thirtydegreesray.openhub.mvp.model.graphql.DiscussionComment;
import com.thirtydegreesray.openhub.mvp.presenter.DiscussionPresenter;
import com.thirtydegreesray.openhub.ui.adapter.DiscussionCommentsAdapter;
import com.thirtydegreesray.openhub.ui.fragment.base.ListFragment;
import com.thirtydegreesray.openhub.util.BundleHelper;

import java.util.ArrayList;

public class DiscussionCommentsFragment extends ListFragment<DiscussionPresenter, DiscussionCommentsAdapter>
        implements IDiscussionContract.View {

    public static DiscussionCommentsFragment create(@NonNull String owner, @NonNull String repo, int number) {
        DiscussionCommentsFragment fragment = new DiscussionCommentsFragment();
        fragment.setArguments(BundleHelper.builder()
                .put("owner", owner).put("repo", repo).put("number", number).build());
        return fragment;
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
    protected void initFragment(Bundle savedInstanceState) {
        super.initFragment(savedInstanceState);
        setLoadMoreEnable(true);
        setAutoJudgeCanLoadMoreEnable(false);
        addVerticalDivider();
    }

    @Override
    protected void onReLoadData() {
        mPresenter.loadDiscussion(true);
    }

    @Override
    protected void onLoadMore(int page) {
        super.onLoadMore(page);
        mPresenter.loadDiscussion(false);
    }

    @Override
    protected String getEmptyTip() {
        return getString(R.string.no_comments);
    }

    @Override
    public void showItems(ArrayList<DiscussionComment> items) {
        adapter.setData(items);
        postNotifyDataSetChanged();
    }

}
