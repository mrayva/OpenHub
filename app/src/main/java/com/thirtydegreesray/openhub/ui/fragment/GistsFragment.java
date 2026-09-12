

package com.thirtydegreesray.openhub.ui.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.view.View;

import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.inject.component.AppComponent;
import com.thirtydegreesray.openhub.inject.component.DaggerFragmentComponent;
import com.thirtydegreesray.openhub.inject.module.FragmentModule;
import com.thirtydegreesray.openhub.mvp.contract.IGistsContract;
import com.thirtydegreesray.openhub.mvp.model.Gist;
import com.thirtydegreesray.openhub.mvp.presenter.GistsPresenter;
import com.thirtydegreesray.openhub.ui.activity.GistActivity;
import com.thirtydegreesray.openhub.ui.adapter.GistsAdapter;
import com.thirtydegreesray.openhub.ui.fragment.base.ListFragment;
import com.thirtydegreesray.openhub.util.BundleHelper;

import java.util.ArrayList;

/**
 * One shared fragment for every gists list variant - mirrors
 * RepositoriesFragment's type-enum-driven design (used both standalone, e.g.
 * a single user's public gists, and as ViewPager tabs, e.g. My/Starred/Public).
 */
public class GistsFragment extends ListFragment<GistsPresenter, GistsAdapter>
        implements IGistsContract.View {

    public enum GistsType {
        MY, STARRED, PUBLIC, USER
    }

    public static GistsFragment create(@NonNull GistsType type, @NonNull String user) {
        GistsFragment fragment = new GistsFragment();
        fragment.setArguments(BundleHelper.builder().put("type", type).put("user", user).build());
        return fragment;
    }

    public static GistsFragment create(@NonNull GistsType type) {
        return create(type, "");
    }

    private final int GIST_DETAIL_REQUEST_CODE = 300;

    public GistsType getGistsType() {
        return (GistsType) getArguments().getSerializable("type");
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
        mPresenter.loadGists(1, true);
    }

    @Override
    protected void onLoadMore(int page) {
        super.onLoadMore(page);
        mPresenter.loadGists(page, false);
    }

    @Override
    protected String getEmptyTip() {
        return getString(R.string.no_gists);
    }

    @Override
    public void showGists(ArrayList<Gist> gists, int appendedCount) {
        adapter.setData(gists);
        if (appendedCount > 0) {
            postNotifyItemRangeInserted(gists.size() - appendedCount, appendedCount);
        } else {
            postNotifyDataSetChanged();
        }
    }

    @Override
    public void onItemClick(int position, @NonNull View view) {
        super.onItemClick(position, view);
        Intent intent = GistActivity.createIntent(getActivity(), adapter.getData().get(position).getId());
        startActivityForResult(intent, GIST_DETAIL_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GIST_DETAIL_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            mPresenter.loadGists(1, true);
        }
    }

    public void reload() {
        mPresenter.loadGists(1, true);
    }

    @Override
    public void onFragmentShowed() {
        super.onFragmentShowed();
        if (mPresenter != null) mPresenter.prepareLoadData();
    }

}
