

package com.thirtydegreesray.openhub.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.View;

import com.thirtydegreesray.dataautoaccess.annotation.AutoAccess;
import com.thirtydegreesray.openhub.AppData;
import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.R2;
import com.thirtydegreesray.openhub.inject.component.AppComponent;
import com.thirtydegreesray.openhub.ui.activity.base.PagerActivity;
import com.thirtydegreesray.openhub.ui.adapter.base.FragmentPagerModel;
import com.thirtydegreesray.openhub.ui.adapter.base.FragmentViewPagerAdapter;
import com.thirtydegreesray.openhub.ui.fragment.GistsFragment;
import com.thirtydegreesray.openhub.ui.widget.ZoomAbleFloatingActionButton;
import com.thirtydegreesray.openhub.util.BundleHelper;

import java.util.Collections;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Two launch modes sharing one Activity+layout rather than two near-identical
 * classes:
 * - showMine(): My Gists / Starred / Public as three tabs - the nav drawer
 *   entry point, and also used from your own profile screen's gists count.
 * - showForUser(): a single, tab-less list of one other user's public gists -
 *   used from that user's profile screen's gists count.
 */
public class GistsListActivity extends PagerActivity {

    enum Mode { MINE, USER }

    public static void showMine(@NonNull Activity activity) {
        Intent intent = new Intent(activity, GistsListActivity.class);
        intent.putExtras(BundleHelper.builder().put("mode", Mode.MINE).build());
        activity.startActivity(intent);
    }

    public static void showForUser(@NonNull Activity activity, @NonNull String user) {
        Intent intent = new Intent(activity, GistsListActivity.class);
        intent.putExtras(BundleHelper.builder().put("mode", Mode.USER).put("user", user).build());
        activity.startActivity(intent);
    }

    private final int CREATE_GIST_REQUEST_CODE = 400;

    @AutoAccess Mode mode;
    @AutoAccess String user;

    @BindView(R2.id.float_action_bn) ZoomAbleFloatingActionButton floatingActionButton;

    @Override
    protected void initActivity() {
        super.initActivity();
        pagerAdapter = new FragmentViewPagerAdapter(getSupportFragmentManager());
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        setToolbarScrollAble(true);
        setToolbarBackEnable();
        setToolbarTitle(getString(R.string.gists));
        if (Mode.MINE.equals(mode)) {
            pagerAdapter.setPagerList(FragmentPagerModel.createGistsPagerList(
                    getActivity(), getFragments(), AppData.INSTANCE.getLoggedUser().getLogin()));
            tabLayout.setVisibility(View.VISIBLE);
            floatingActionButton.setVisibility(View.VISIBLE);
            floatingActionButton.setImageResource(R.drawable.ic_add);
        } else {
            pagerAdapter.setPagerList(Collections.singletonList(
                    new FragmentPagerModel(getString(R.string.gists),
                            GistsFragment.create(GistsFragment.GistsType.USER, user))));
            tabLayout.setVisibility(View.GONE);
        }
        tabLayout.setupWithViewPager(viewPager);
        viewPager.setAdapter(pagerAdapter);
        showFirstPager();
    }

    @OnClick(R2.id.float_action_bn)
    public void onAddGistClick() {
        CreateGistActivity.showForCreate(getActivity(), CREATE_GIST_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CREATE_GIST_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            for (Object fragment : getFragments()) {
                if (fragment instanceof GistsFragment &&
                        GistsFragment.GistsType.MY.equals(((GistsFragment) fragment).getGistsType())) {
                    ((GistsFragment) fragment).reload();
                }
            }
        }
    }

    @Override
    protected void setupActivityComponent(AppComponent appComponent) {

    }

    @Override
    protected int getContentView() {
        return R.layout.activity_view_pager;
    }

    @Override
    public int getPagerSize() {
        return Mode.MINE.equals(mode) ? 3 : 1;
    }

    @Override
    protected int getFragmentPosition(Fragment fragment) {
        if (!(fragment instanceof GistsFragment)) return -1;
        if (!Mode.MINE.equals(mode)) return 0;
        switch (((GistsFragment) fragment).getGistsType()) {
            case MY: return 0;
            case STARRED: return 1;
            case PUBLIC: return 2;
            default: return -1;
        }
    }

}
