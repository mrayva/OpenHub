

package com.thirtydegreesray.openhub.ui.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.R2;
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
import com.thirtydegreesray.openhub.ui.widget.ZoomAbleFloatingActionButton;
import com.thirtydegreesray.openhub.util.AppOpener;
import com.thirtydegreesray.openhub.util.BundleHelper;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Gist detail screen (Files + Comments tabs): view, star/unstar, fork,
 * comment write/edit/delete, and edit/delete the gist itself (owner only).
 */
public class GistActivity extends PagerActivity<GistPresenter>
        implements IGistContract.View {

    public static final int ADD_GIST_COMMENT_REQUEST_CODE = 200;
    public static final int EDIT_GIST_COMMENT_REQUEST_CODE = 201;
    public static final int EDIT_GIST_REQUEST_CODE = 202;

    public static void show(@NonNull Context context, @NonNull Gist gist) {
        show(context, gist.getId());
    }

    public static void show(@NonNull Context context, @NonNull String gistId) {
        context.startActivity(createIntent(context, gistId));
    }

    public static Intent createIntent(@NonNull Context context, @NonNull String gistId) {
        Intent intent = new Intent(context, GistActivity.class);
        intent.putExtras(BundleHelper.builder().put("gistId", gistId).build());
        return intent;
    }

    @BindView(R2.id.float_action_bn) ZoomAbleFloatingActionButton commentBn;

    private GistCommentsFragment gistCommentsFragment;

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
            getMenuInflater().inflate(R.menu.menu_gist, menu);
            MenuItem starItem = menu.findItem(R.id.action_star);
            starItem.setTitle(mPresenter.isStarred() ? R.string.unstar : R.string.star);
            starItem.setIcon(mPresenter.isStarred() ?
                    R.drawable.ic_star_title : R.drawable.ic_un_star_title);
            menu.findItem(R.id.action_fork).setVisible(!mPresenter.isMine());
            menu.findItem(R.id.action_edit).setVisible(mPresenter.isMine());
            menu.findItem(R.id.action_delete).setVisible(mPresenter.isMine());
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_star:
                mPresenter.starGist(!mPresenter.isStarred());
                return true;
            case R.id.action_fork:
                forkGist();
                return true;
            case R.id.action_edit:
                CreateGistActivity.showForEdit(getActivity(), mPresenter.getGist(), EDIT_GIST_REQUEST_CODE);
                return true;
            case R.id.action_delete:
                deleteGist();
                return true;
            case R.id.action_open_in_browser:
                AppOpener.openInCustomTabsOrBrowser(getActivity(), mPresenter.getGist().getHtmlUrl());
                return true;
            case R.id.action_share:
                AppOpener.shareText(getActivity(), mPresenter.getGist().getHtmlUrl());
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void forkGist() {
        new AlertDialog.Builder(getActivity())
                .setCancelable(true)
                .setTitle(R.string.warning_dialog_tile)
                .setMessage(R.string.fork_gist_warning_msg)
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel())
                .setPositiveButton(R.string.fork, (dialog, which) -> mPresenter.createFork())
                .show();
    }

    private void deleteGist() {
        new AlertDialog.Builder(getActivity())
                .setCancelable(true)
                .setTitle(R.string.warning_dialog_tile)
                .setMessage(R.string.delete_gist_warning)
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    dialog.dismiss();
                    mPresenter.deleteGist();
                })
                .show();
    }

    @Override
    public void onGistDeleted() {
        setResult(RESULT_OK);
        finish();
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
    public void onPageSelected(int position) {
        super.onPageSelected(position);
        commentBn.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
    }

    @OnClick(R2.id.float_action_bn)
    public void onCommentBnClicked() {
        MarkdownEditorActivity.show(getActivity(), R.string.comment, ADD_GIST_COMMENT_REQUEST_CODE, null);
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        if (fragment instanceof GistCommentsFragment) {
            gistCommentsFragment = (GistCommentsFragment) fragment;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == ADD_GIST_COMMENT_REQUEST_CODE) {
            String text = data.getExtras().getString("text");
            if (gistCommentsFragment != null) gistCommentsFragment.addComment(text);
            return;
        } else if (resultCode == RESULT_OK && requestCode == EDIT_GIST_COMMENT_REQUEST_CODE) {
            String text = data.getExtras().getString("text");
            if (gistCommentsFragment != null) gistCommentsFragment.onEditComment(text);
            return;
        } else if (resultCode == RESULT_OK && requestCode == EDIT_GIST_REQUEST_CODE) {
            setResult(RESULT_OK);
            mPresenter.loadGist(true);
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
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
