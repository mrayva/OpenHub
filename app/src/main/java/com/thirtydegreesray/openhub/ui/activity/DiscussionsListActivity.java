package com.thirtydegreesray.openhub.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;

import com.thirtydegreesray.dataautoaccess.annotation.AutoAccess;
import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.mvp.contract.base.IBaseContract;
import com.thirtydegreesray.openhub.ui.activity.base.SingleFragmentActivity;
import com.thirtydegreesray.openhub.ui.fragment.DiscussionsFragment;
import com.thirtydegreesray.openhub.util.BundleHelper;

/**
 * Mirrors WikiActivity: a repo-level feature reached from RepositoryActivity's
 * overflow menu (not a ViewPager tab - discussion categories are dynamic per
 * repo, unlike the fixed Info/Files/Commits/Activity/Topics tabs).
 */
public class DiscussionsListActivity extends
        SingleFragmentActivity<IBaseContract.Presenter, DiscussionsFragment> {

    public static void show(@NonNull Activity activity, @NonNull String owner,
                            @NonNull String repo) {
        Intent intent = new Intent(activity, DiscussionsListActivity.class);
        intent.putExtras(BundleHelper.builder().put("owner", owner).put("repo", repo).build());
        activity.startActivity(intent);
    }

    @AutoAccess String owner;
    @AutoAccess String repo;

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        setToolbarTitle(getString(R.string.discussions), owner.concat("/").concat(repo));
        setToolbarScrollAble(true);
    }

    @Override
    protected DiscussionsFragment createFragment() {
        return DiscussionsFragment.create(owner, repo);
    }
}
