package com.thirtydegreesray.openhub.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.View;

import com.thirtydegreesray.dataautoaccess.annotation.AutoAccess;
import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.inject.component.AppComponent;
import com.thirtydegreesray.openhub.ui.activity.base.PagerActivity;
import com.thirtydegreesray.openhub.ui.adapter.base.FragmentPagerModel;
import com.thirtydegreesray.openhub.ui.adapter.base.FragmentViewPagerAdapter;
import com.thirtydegreesray.openhub.ui.fragment.RepositoriesFragment;
import com.thirtydegreesray.openhub.util.BundleHelper;

import java.util.ArrayList;

/**
 * Repos matching a single tapped topic slug (e.g. from a repo's Topics tab),
 * reusing the same topic-search pager as MyTopicsActivity but for one ad-hoc
 * topic instead of the user's persisted "my topics" selection.
 */
public class TopicRepositoriesActivity extends PagerActivity {

    public static void show(@NonNull Context context, @NonNull String topicSlug) {
        Intent intent = new Intent(context, TopicRepositoriesActivity.class);
        intent.putExtras(BundleHelper.builder().put("topicSlug", topicSlug).build());
        context.startActivity(intent);
    }

    @AutoAccess String topicSlug;

    @Override
    protected void initActivity() {
        super.initActivity();
        pagerAdapter = new FragmentViewPagerAdapter(getSupportFragmentManager());
    }

    @Override
    protected void setupActivityComponent(AppComponent appComponent) {

    }

    @Override
    protected int getContentView() {
        return R.layout.activity_view_pager;
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        setToolbarScrollAble(true);
        setToolbarBackEnable();
        setToolbarTitle(topicSlug);
        ArrayList<String> topicSlugs = new ArrayList<>();
        topicSlugs.add(topicSlug);
        pagerAdapter.setPagerList(FragmentPagerModel.createTopicsSearchPagerList(
                getActivity(), getFragments(), topicSlugs, "stars"));
        tabLayout.setVisibility(View.GONE);
        viewPager.setAdapter(pagerAdapter);
        showFirstPager();
    }

    @Override
    public int getPagerSize() {
        return 1;
    }

    @Override
    protected int getFragmentPosition(Fragment fragment) {
        return fragment instanceof RepositoriesFragment ? 0 : -1;
    }

}
