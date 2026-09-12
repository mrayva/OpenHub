

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
import com.thirtydegreesray.openhub.mvp.contract.IGistContract;
import com.thirtydegreesray.openhub.mvp.model.Gist;
import com.thirtydegreesray.openhub.mvp.presenter.GistPresenter;
import com.thirtydegreesray.openhub.ui.activity.base.PagerActivity;
import com.thirtydegreesray.openhub.ui.adapter.base.FragmentPagerModel;
import com.thirtydegreesray.openhub.ui.fragment.GistCommentsFragment;
import com.thirtydegreesray.openhub.ui.fragment.GistFilesFragment;
import com.thirtydegreesray.openhub.util.AppOpener;
import com.thirtydegreesray.openhub.util.BundleHelper;

/**
 * Gist detail screen (Files + Comments tabs), viewing-only for now - see
 * plan for star/fork/edit/delete, deferred to a later phase.
 */
public class GistActivity extends PagerActivity<GistPresenter>
        implements IGistContract.View {

    public static void show(@NonNull Context context, @NonNull Gist gist) {
        show(context, gist.getId());
    }

    public static void show(@NonNull Context context, @NonNull String gistId) {
        Intent intent = new Intent(context, GistActivity.class);
        intent.putExtras(BundleHelper.builder().put("gistId", gistId).build());
        context.startActivity(intent);
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
        return R.layout.activity_view_pager;
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        setToolbarScrollAble(true);
        setToolbarBackEnable();
        setToolbarTitle(getString(R.string.gist));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mPresenter.getGist() != null) {
            getMenuInflater().inflate(R.menu.menu_open_in_browser, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_open_in_browser) {
            AppOpener.openInCustomTabsOrBrowser(getActivity(), mPresenter.getGist().getHtmlUrl());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void showGist(Gist gist) {
        String subtitle = gist.getOwner() != null ? gist.getOwner().getLogin() : null;
        setToolbarTitle(gist.getDisplayTitle(), subtitle);

        if (pagerAdapter.getCount() == 0) {
            pagerAdapter.setPagerList(FragmentPagerModel.createGistPagerList(getActivity(), gist, getFragments()));
            tabLayout.setVisibility(View.VISIBLE);
            tabLayout.setupWithViewPager(viewPager);
            viewPager.setAdapter(pagerAdapter);
            showFirstPager();
        }
        invalidateOptionsMenu();
    }

    @Override
    public int getPagerSize() {
        return 2;
    }

    @Override
    protected int getFragmentPosition(Fragment fragment) {
        if (fragment instanceof GistFilesFragment) {
            return 0;
        } else if (fragment instanceof GistCommentsFragment) {
            return 1;
        } else {
            return -1;
        }
    }

}
