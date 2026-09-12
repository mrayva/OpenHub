package com.thirtydegreesray.openhub.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;

import com.thirtydegreesray.dataautoaccess.annotation.AutoAccess;
import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.R2;
import com.thirtydegreesray.openhub.inject.component.AppComponent;
import com.thirtydegreesray.openhub.inject.component.DaggerActivityComponent;
import com.thirtydegreesray.openhub.inject.module.ActivityModule;
import com.thirtydegreesray.openhub.mvp.contract.ICreateDiscussionContract;
import com.thirtydegreesray.openhub.mvp.model.graphql.DiscussionCategory;
import com.thirtydegreesray.openhub.mvp.presenter.CreateDiscussionPresenter;
import com.thirtydegreesray.openhub.ui.activity.base.BaseActivity;
import com.thirtydegreesray.openhub.util.BundleHelper;
import com.thirtydegreesray.openhub.util.StringUtils;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.OnClick;

public class CreateDiscussionActivity extends BaseActivity<CreateDiscussionPresenter>
        implements ICreateDiscussionContract.View {

    public static void show(@NonNull Activity activity, @NonNull String owner,
                            @NonNull String repo, int requestCode) {
        Intent intent = new Intent(activity, CreateDiscussionActivity.class);
        intent.putExtras(BundleHelper.builder().put("owner", owner).put("repo", repo).build());
        activity.startActivityForResult(intent, requestCode);
    }

    @AutoAccess String owner;
    @AutoAccess String repo;

    @BindView(R2.id.et_title) EditText etTitle;
    @BindView(R2.id.tv_category) TextView tvCategory;
    @BindView(R2.id.et_body) EditText etBody;

    private ArrayList<DiscussionCategory> categories;
    private DiscussionCategory selectedCategory;

    @Override
    protected void setupActivityComponent(AppComponent appComponent) {
        DaggerActivityComponent.builder()
                .appComponent(appComponent)
                .activityModule(new ActivityModule(getActivity()))
                .build()
                .inject(this);
    }

    @Override
    protected int getContentView() {
        return R.layout.activity_create_discussion;
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        setToolbarBackEnable();
        setToolbarTitle(getString(R.string.create_discussion));
    }

    @OnClick(R2.id.tv_category)
    public void onCategoryClick() {
        showCategoryDialog();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_confirm, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_commit) {
            mPresenter.submit(selectedCategory, etTitle.getText().toString().trim(),
                    etBody.getText().toString());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void showCategories(ArrayList<DiscussionCategory> categories) {
        this.categories = categories;
        if (!StringUtils.isBlankList(categories) && selectedCategory == null) {
            selectedCategory = categories.get(0);
            updateCategoryLabel();
        }
    }

    private void showCategoryDialog() {
        if (StringUtils.isBlankList(categories)) return;
        final String[] labels = new String[categories.size()];
        for (int i = 0; i < categories.size(); i++) {
            DiscussionCategory category = categories.get(i);
            labels[i] = StringUtils.isBlank(category.getEmoji()) ?
                    category.getName() : category.getEmoji().concat(" ").concat(category.getName());
        }
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.choose_category)
                .setItems(labels, (dialog, which) -> {
                    selectedCategory = categories.get(which);
                    updateCategoryLabel();
                })
                .show();
    }

    private void updateCategoryLabel() {
        String emoji = StringUtils.isBlank(selectedCategory.getEmoji()) ? "" : selectedCategory.getEmoji().concat(" ");
        tvCategory.setText(emoji.concat(selectedCategory.getName()));
    }

    @Override
    public void onDiscussionCreated(int number) {
        showSuccessToast(getString(R.string.create_discussion_success));
        DiscussionActivity.show(getActivity(), owner, repo, number);
        setResult(RESULT_OK);
        finish();
    }
}
