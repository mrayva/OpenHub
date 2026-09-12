

package com.thirtydegreesray.openhub.ui.fragment;

import android.os.Bundle;
import androidx.annotation.NonNull;

import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.inject.component.AppComponent;
import com.thirtydegreesray.openhub.inject.component.DaggerFragmentComponent;
import com.thirtydegreesray.openhub.inject.module.FragmentModule;
import com.thirtydegreesray.openhub.mvp.contract.IGistCommentsContract;
import com.thirtydegreesray.openhub.mvp.model.GistComment;
import com.thirtydegreesray.openhub.mvp.presenter.GistCommentsPresenter;
import com.thirtydegreesray.openhub.ui.adapter.GistCommentsAdapter;
import com.thirtydegreesray.openhub.ui.fragment.base.ListFragment;
import com.thirtydegreesray.openhub.util.BundleHelper;

import java.util.ArrayList;

/**
 * Read-only this phase - no add/edit/delete yet (see plan).
 */
public class GistCommentsFragment extends ListFragment<GistCommentsPresenter, GistCommentsAdapter>
        implements IGistCommentsContract.View {

    public static GistCommentsFragment create(@NonNull String gistId) {
        GistCommentsFragment fragment = new GistCommentsFragment();
        fragment.setArguments(BundleHelper.builder().put("gistId", gistId).build());
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
    }

    @Override
    protected void onReLoadData() {
        mPresenter.loadComments(1, true);
    }

    @Override
    protected void onLoadMore(int page) {
        super.onLoadMore(page);
        mPresenter.loadComments(page, false);
    }

    @Override
    protected String getEmptyTip() {
        return getString(R.string.no_comments);
    }

    @Override
    public void showComments(ArrayList<GistComment> comments) {
        adapter.setData(comments);
        postNotifyDataSetChanged();
    }

    @Override
    public void onFragmentShowed() {
        super.onFragmentShowed();
        if (mPresenter != null) mPresenter.prepareLoadData();
    }

}
