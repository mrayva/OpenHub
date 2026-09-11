package com.thirtydegreesray.openhub.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;

import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.mvp.contract.base.IBaseContract;
import com.thirtydegreesray.openhub.ui.activity.base.SingleFragmentActivity;
import com.thirtydegreesray.openhub.ui.fragment.RepositoriesFragment;

/**
 * Lists every repo on the ignore list (regardless of the Trending/Created
 * toggle state), for browsing/removing from a list that may get very large -
 * swiping here always un-ignores, unlike Trending/Created's directional
 * swipe. Reuses RepositoriesFragment/RepositoriesAdapter/RepositoriesPresenter
 * with RepositoriesType.IGNORED rather than a bespoke screen.
 */
public class IgnoredReposActivity extends
        SingleFragmentActivity<IBaseContract.Presenter, RepositoriesFragment> {

    public static void show(@NonNull Activity activity) {
        Intent intent = new Intent(activity, IgnoredReposActivity.class);
        activity.startActivity(intent);
    }

    @Override
    protected RepositoriesFragment createFragment() {
        return RepositoriesFragment.createForIgnored();
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        setToolbarTitle(getString(R.string.manage_ignore_list));
    }

}
