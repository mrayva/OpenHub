package com.thirtydegreesray.openhub.ui.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.thirtydegreesray.openhub.AppApplication;
import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.R2;
import com.thirtydegreesray.openhub.dao.MyTopic;
import com.thirtydegreesray.openhub.mvp.contract.base.IBaseContract;
import com.thirtydegreesray.openhub.ui.activity.base.SingleFragmentActivity;
import com.thirtydegreesray.openhub.ui.fragment.TopicsEditorFragment;
import com.thirtydegreesray.openhub.ui.widget.ZoomAbleFloatingActionButton;
import com.thirtydegreesray.openhub.util.ViewUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.OnClick;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Manage the "My Topics" list used by MyTopicsActivity: add a topic by name,
 * swipe to remove one, tap to include/exclude it from the multi-topic search.
 *
 * Export/import mirrors IgnoredReposActivity's: the system file picker
 * (ACTION_CREATE_DOCUMENT/ACTION_OPEN_DOCUMENT) rather than a fixed path, so
 * it needs no storage permission, and it's here (rather than a shared
 * helper) since MyTopic has no other caller needing DB access outside
 * TopicsEditorPresenter's own CRUD.
 */
public class TopicsEditorActivity extends
        SingleFragmentActivity<IBaseContract.Presenter, TopicsEditorFragment> {

    private static final int EXPORT_REQUEST_CODE = 900;
    private static final int IMPORT_REQUEST_CODE = 901;

    public static void show(@NonNull Activity activity, int requestCode) {
        Intent intent = new Intent(activity, TopicsEditorActivity.class);
        activity.startActivityForResult(intent, requestCode);
    }

    @BindView(R2.id.float_action_bn) ZoomAbleFloatingActionButton floatingActionButton;

    @Override
    protected TopicsEditorFragment createFragment() {
        return TopicsEditorFragment.create();
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        setToolbarTitle(getString(R.string.my_topics));
        setToolbarScrollAble(true);
        floatingActionButton.setVisibility(View.VISIBLE);
        floatingActionButton.setImageResource(R.drawable.ic_add);
        // Every change (add/remove/toggle) is persisted immediately, so the
        // caller should always reload on return - regardless of whether the
        // user exits via the hardware back button, the toolbar's up arrow
        // (which bypasses onBackPressed(), going through finishActivity()
        // instead), or a system back gesture.
        setResult(Activity.RESULT_OK);
    }

    @OnClick(R2.id.float_action_bn)
    public void onAddClick() {
        final EditText editText = new EditText(this);
        editText.setInputType(InputType.TYPE_CLASS_TEXT);
        editText.setHint(getString(R.string.add_topic_hint));
        editText.setTextColor(ViewUtils.getTitleColor(this));
        editText.setHintTextColor(ViewUtils.getSubTitleColor(this));
        new AlertDialog.Builder(this)
                .setTitle(R.string.add_topic)
                .setView(editText)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String slug = editText.getText().toString();
                    if (!getFragment().addTopic(slug)) {
                        showWarningToast(getString(R.string.topic_already_added));
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_topics_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_export) {
            startExport();
            return true;
        } else if (item.getItemId() == R.id.action_import) {
            startImport();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void startExport() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "openhub_topics.json");
        startActivityForResult(intent, EXPORT_REQUEST_CODE);
    }

    private void startImport() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        startActivityForResult(intent, IMPORT_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != EXPORT_REQUEST_CODE && requestCode != IMPORT_REQUEST_CODE) return;
        if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (requestCode == EXPORT_REQUEST_CODE) {
            exportTo(uri);
        } else {
            importFrom(uri);
        }
    }

    private void exportTo(final Uri uri) {
        Observable.fromCallable(() -> new ArrayList<>(AppApplication.get().getAppComponent()
                        .getDaoSession().getMyTopicDao().loadAll()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(topics -> {
                    if (topics.isEmpty()) {
                        showWarningToast(getString(R.string.topics_empty_export));
                        return;
                    }
                    Observable.fromCallable(() -> writeJsonToUri(uri, topics))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    count -> showSuccessToast(String.format(
                                            getString(R.string.export_topics_success), count)),
                                    error -> showErrorToast(getString(R.string.export_topics_failed))
                            );
                }, error -> showErrorToast(getString(R.string.export_topics_failed)));
    }

    private void importFrom(final Uri uri) {
        Observable.fromCallable(() -> readTopicsFromUri(uri))
                .subscribeOn(Schedulers.io())
                .map(topics -> {
                    AppApplication.get().getAppComponent().getDaoSession()
                            .getMyTopicDao().insertOrReplaceInTx(topics);
                    return topics.size();
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(count -> {
                    showSuccessToast(String.format(getString(R.string.import_topics_success), count));
                    getFragment().onRefresh();
                }, error -> showErrorToast(getString(R.string.import_topics_failed)));
    }

    private int writeJsonToUri(Uri uri, ArrayList<MyTopic> topics) throws IOException {
        Gson gson = new Gson();
        String json = gson.toJson(topics);
        OutputStream os = getContentResolver().openOutputStream(uri);
        if (os == null) throw new IOException("Could not open output stream for " + uri);
        try {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        } finally {
            os.close();
        }
        return topics.size();
    }

    private ArrayList<MyTopic> readTopicsFromUri(Uri uri) throws IOException {
        InputStream is = getContentResolver().openInputStream(uri);
        if (is == null) throw new IOException("Could not open input stream for " + uri);
        String json;
        try {
            json = readAll(is);
        } finally {
            is.close();
        }
        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<MyTopic>>() {}.getType();
        ArrayList<MyTopic> topics = gson.fromJson(json, type);
        if (topics == null) throw new IOException("Not a valid topics export");
        return topics;
    }

    /**
     * Manual read loop rather than InputStream.readAllBytes() - that's only
     * available from API 33, and this app's minSdk is 21.
     */
    private static String readAll(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int bytesRead;
        while ((bytesRead = is.read(chunk)) != -1) {
            buffer.write(chunk, 0, bytesRead);
        }
        return buffer.toString("UTF-8");
    }
}
