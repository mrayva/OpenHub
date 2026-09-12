package com.thirtydegreesray.openhub.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.view.View;

import com.thirtydegreesray.dataautoaccess.annotation.AutoAccess;
import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.R2;
import com.thirtydegreesray.openhub.mvp.contract.base.IBaseContract;
import com.thirtydegreesray.openhub.ui.activity.base.SingleFragmentActivity;
import com.thirtydegreesray.openhub.ui.fragment.DiscussionsFragment;
import com.thirtydegreesray.openhub.ui.widget.ZoomAbleFloatingActionButton;
import com.thirtydegreesray.openhub.util.BundleHelper;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Mirrors WikiActivity: a repo-level feature reached from RepositoryActivity's
 * overflow menu (not a ViewPager tab - discussion categories are dynamic per
 * repo, unlike the fixed Info/Files/Commits/Activity/Topics tabs).
 */
public class DiscussionsListActivity extends
        SingleFragmentActivity<IBaseContract.Presenter, DiscussionsFragment> {

    private static final int CREATE_DISCUSSION_REQUEST_CODE = 710;

    public static void show(@NonNull Activity activity, @NonNull String owner,
                            @NonNull String repo) {
        Intent intent = new Intent(activity, DiscussionsListActivity.class);
        intent.putExtras(BundleHelper.builder().put("owner", owner).put("repo", repo).build());
        activity.startActivity(intent);
    }

    @AutoAccess String owner;
    @AutoAccess String repo;

    @BindView(R2.id.float_action_bn) ZoomAbleFloatingActionButton floatingActionButton;

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        setToolbarTitle(getString(R.string.discussions), owner.concat("/").concat(repo));
        setToolbarScrollAble(true);
        floatingActionButton.setVisibility(View.VISIBLE);
        floatingActionButton.setImageResource(R.drawable.ic_add);
    }

    @Override
    protected DiscussionsFragment createFragment() {
        return DiscussionsFragment.create(owner, repo);
    }

    @OnClick(R2.id.float_action_bn)
    public void onAddDiscussionClick() {
        CreateDiscussionActivity.show(getActivity(), owner, repo, CREATE_DISCUSSION_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CREATE_DISCUSSION_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            getFragment().reload();
        }
    }
}
