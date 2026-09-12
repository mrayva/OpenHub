package com.thirtydegreesray.openhub.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;

import com.thirtydegreesray.dataautoaccess.annotation.AutoAccess;
import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.mvp.contract.base.IBaseContract;
import com.thirtydegreesray.openhub.ui.activity.base.SingleFragmentActivity;
import com.thirtydegreesray.openhub.ui.fragment.DiscussionCommentsFragment;
import com.thirtydegreesray.openhub.util.BundleHelper;

/**
 * Single screen (title/category/body header as the flattened list's row 0,
 * see DiscussionPresenter) rather than a Files/Comments pager like Gists -
 * a discussion doesn't need separate tabs.
 */
public class DiscussionActivity extends
        SingleFragmentActivity<IBaseContract.Presenter, DiscussionCommentsFragment> {

    public static void show(@NonNull Activity activity, @NonNull String owner,
                            @NonNull String repo, int number) {
        Intent intent = new Intent(activity, DiscussionActivity.class);
        intent.putExtras(BundleHelper.builder()
                .put("owner", owner).put("repo", repo).put("number", number).build());
        activity.startActivity(intent);
    }

    @AutoAccess String owner;
    @AutoAccess String repo;
    @AutoAccess int number;

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        setToolbarTitle(getString(R.string.discussion), owner.concat("/").concat(repo));
        setToolbarScrollAble(true);
    }

    @Override
    protected DiscussionCommentsFragment createFragment() {
        return DiscussionCommentsFragment.create(owner, repo, number);
    }
}
