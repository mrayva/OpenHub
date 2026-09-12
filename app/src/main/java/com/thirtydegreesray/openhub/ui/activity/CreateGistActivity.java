package com.thirtydegreesray.openhub.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.thirtydegreesray.dataautoaccess.annotation.AutoAccess;
import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.R2;
import com.thirtydegreesray.openhub.mvp.contract.base.IBaseContract;
import com.thirtydegreesray.openhub.mvp.model.Gist;
import com.thirtydegreesray.openhub.ui.activity.base.SingleFragmentActivity;
import com.thirtydegreesray.openhub.ui.fragment.CreateGistFragment;
import com.thirtydegreesray.openhub.ui.widget.ZoomAbleFloatingActionButton;
import com.thirtydegreesray.openhub.util.BundleHelper;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Create mode (no editingGist): the toolbar overflow offers "Create public
 * gist"/"Create secret gist" - two separate actions rather than a single
 * submit, since GitHub doesn't allow changing a gist's visibility later.
 * Edit mode (editingGist present): the usual menu_confirm checkmark commits.
 */
public class CreateGistActivity extends
        SingleFragmentActivity<IBaseContract.Presenter, CreateGistFragment> {

    public static void showForCreate(@NonNull Activity activity, int requestCode) {
        Intent intent = new Intent(activity, CreateGistActivity.class);
        activity.startActivityForResult(intent, requestCode);
    }

    public static void showForEdit(@NonNull Activity activity, @NonNull Gist gist, int requestCode) {
        Intent intent = new Intent(activity, CreateGistActivity.class);
        intent.putExtras(BundleHelper.builder().put("editingGist", gist).build());
        activity.startActivityForResult(intent, requestCode);
    }

    @AutoAccess Gist editingGist;

    @BindView(R2.id.float_action_bn) ZoomAbleFloatingActionButton floatingActionButton;

    @Override
    protected CreateGistFragment createFragment() {
        return editingGist == null ?
                CreateGistFragment.create() : CreateGistFragment.createForEdit(editingGist);
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        setToolbarTitle(getString(editingGist == null ? R.string.create_gist : R.string.edit_gist));
        setToolbarScrollAble(true);
        floatingActionButton.setVisibility(View.VISIBLE);
        floatingActionButton.setImageResource(R.drawable.ic_add);
    }

    @OnClick(R2.id.float_action_bn)
    public void onAddFileClick() {
        getFragment().addFile();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (editingGist == null) {
            getMenuInflater().inflate(R.menu.menu_create_gist, menu);
        } else {
            getMenuInflater().inflate(R.menu.menu_confirm, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_create_public) {
            getFragment().submitCreate(true);
            return true;
        } else if (id == R.id.action_create_secret) {
            getFragment().submitCreate(false);
            return true;
        } else if (id == R.id.action_commit) {
            getFragment().submitEdit();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null && data.hasExtra("filename")) {
            String filename = data.getStringExtra("filename");
            String content = data.getStringExtra("content");
            getFragment().onFilePicked(requestCode, filename, content);
        }
    }

}
