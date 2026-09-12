package com.thirtydegreesray.openhub.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import com.thirtydegreesray.dataautoaccess.annotation.AutoAccess;
import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.R2;
import com.thirtydegreesray.openhub.inject.component.AppComponent;
import com.thirtydegreesray.openhub.mvp.contract.base.IBaseContract;
import com.thirtydegreesray.openhub.ui.activity.base.BaseActivity;
import com.thirtydegreesray.openhub.util.BundleHelper;
import com.thirtydegreesray.openhub.util.StringUtils;

import butterknife.BindView;

/**
 * A minimal, plain-text-only editor for one gist file's name+content -
 * deliberately NOT MarkdownEditorActivity, whose toolbar injects literal
 * Markdown syntax (would corrupt arbitrary code) and whose preview tab
 * renders everything as Markdown (meaningless for e.g. a .py file).
 */
public class GistFileEditorActivity extends BaseActivity<IBaseContract.Presenter> {

    public static void showForResult(@NonNull Activity activity, int requestCode) {
        activity.startActivityForResult(new Intent(activity, GistFileEditorActivity.class), requestCode);
    }

    public static void showForResult(@NonNull Activity activity, @NonNull String filename,
                                     @NonNull String content, int requestCode) {
        Intent intent = new Intent(activity, GistFileEditorActivity.class);
        intent.putExtras(BundleHelper.builder()
                .put("filename", filename)
                .put("content", content)
                .build());
        activity.startActivityForResult(intent, requestCode);
    }

    @AutoAccess String filename;
    @AutoAccess String content;

    @BindView(R2.id.et_file_name) EditText etFileName;
    @BindView(R2.id.et_file_content) EditText etFileContent;

    @Override
    protected void setupActivityComponent(AppComponent appComponent) {
    }

    @Override
    protected int getContentView() {
        return R.layout.activity_gist_file_editor;
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        setToolbarBackEnable();
        setToolbarTitle(getString(R.string.gist_file));
        if (!StringUtils.isBlank(filename)) etFileName.setText(filename);
        if (!StringUtils.isBlank(content)) etFileContent.setText(content);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_confirm, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_commit) {
            commit();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void commit() {
        String name = etFileName.getText().toString().trim();
        if (StringUtils.isBlank(name)) {
            showWarningToast(getString(R.string.file_name_hint));
            return;
        }
        Intent data = new Intent();
        data.putExtra("filename", name);
        data.putExtra("content", etFileContent.getText().toString());
        setResult(RESULT_OK, data);
        finish();
    }

}
