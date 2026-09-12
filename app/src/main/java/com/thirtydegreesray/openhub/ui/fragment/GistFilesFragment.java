

package com.thirtydegreesray.openhub.ui.fragment;

import android.os.Bundle;
import androidx.annotation.NonNull;
import android.view.View;

import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.inject.component.AppComponent;
import com.thirtydegreesray.openhub.inject.component.DaggerFragmentComponent;
import com.thirtydegreesray.openhub.inject.module.FragmentModule;
import com.thirtydegreesray.openhub.mvp.contract.IGistFilesContract;
import com.thirtydegreesray.openhub.mvp.model.Gist;
import com.thirtydegreesray.openhub.mvp.model.GistFile;
import com.thirtydegreesray.openhub.mvp.presenter.GistFilesPresenter;
import com.thirtydegreesray.openhub.ui.activity.ViewerActivity;
import com.thirtydegreesray.openhub.ui.adapter.GistFilesAdapter;
import com.thirtydegreesray.openhub.ui.fragment.base.ListFragment;
import com.thirtydegreesray.openhub.util.AppOpener;
import com.thirtydegreesray.openhub.util.BundleHelper;
import com.thirtydegreesray.openhub.util.GitHubHelper;
import com.thirtydegreesray.openhub.util.StringUtils;

import java.util.ArrayList;

public class GistFilesFragment extends ListFragment<GistFilesPresenter, GistFilesAdapter>
        implements IGistFilesContract.View {

    public static GistFilesFragment create(@NonNull Gist gist) {
        GistFilesFragment fragment = new GistFilesFragment();
        fragment.setArguments(BundleHelper.builder().put("gist", gist).build());
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
        setRefreshEnable(false);
        setLoadMoreEnable(false);
    }

    @Override
    protected void onReLoadData() {
    }

    @Override
    protected String getEmptyTip() {
        return getString(R.string.no_files);
    }

    @Override
    public void showFiles(ArrayList<GistFile> files) {
        adapter.setData(files);
        postNotifyDataSetChanged();
    }

    @Override
    public void onItemClick(int position, @NonNull View view) {
        super.onItemClick(position, view);
        GistFile file = adapter.getData().get(position);
        if (!StringUtils.isBlank(file.getContent())) {
            if (GitHubHelper.isMarkdown(file.getFilename())) {
                ViewerActivity.showMdSource(getActivity(), file.getFilename(), file.getContent());
            } else {
                ViewerActivity.showCode(getActivity(), file.getFilename(), file.getContent(),
                        GitHubHelper.getExtension(file.getFilename()));
            }
        } else if (!StringUtils.isBlank(file.getRawUrl())) {
            // Large/truncated files come back with no inline content - open
            // the raw file in a browser rather than building out a separate
            // fetch-then-render path for this phase.
            AppOpener.openInCustomTabsOrBrowser(getActivity(), file.getRawUrl());
        }
    }

    @Override
    public void onFragmentShowed() {
        super.onFragmentShowed();
        if (mPresenter != null) mPresenter.prepareLoadData();
    }

}
