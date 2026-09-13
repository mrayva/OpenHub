package com.thirtydegreesray.openhub.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.view.Menu;
import android.view.MenuItem;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.dao.IgnoredRepo;
import com.thirtydegreesray.openhub.mvp.contract.base.IBaseContract;
import com.thirtydegreesray.openhub.ui.activity.base.SingleFragmentActivity;
import com.thirtydegreesray.openhub.ui.fragment.RepositoriesFragment;
import com.thirtydegreesray.openhub.util.IgnoredRepoHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Lists every repo on the ignore list (regardless of the Trending/Created
 * toggle state), for browsing/removing from a list that may get very large -
 * swiping here always un-ignores, unlike Trending/Created's directional
 * swipe. Reuses RepositoriesFragment/RepositoriesAdapter/RepositoriesPresenter
 * with RepositoriesType.IGNORED rather than a bespoke screen.
 *
 * Export/import (this class only, IgnoredRepoHelper owns the actual DB
 * access) exists so the list survives an app data/cache clear or a device
 * change - it's otherwise only in this app's own local GreenDAO database.
 * Uses the system file picker (ACTION_CREATE_DOCUMENT/ACTION_OPEN_DOCUMENT)
 * rather than a fixed path, so it needs no storage permission at all.
 */
public class IgnoredReposActivity extends
        SingleFragmentActivity<IBaseContract.Presenter, RepositoriesFragment> {

    private static final int EXPORT_REQUEST_CODE = 900;
    private static final int IMPORT_REQUEST_CODE = 901;

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_ignored_repos, menu);
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
        intent.putExtra(Intent.EXTRA_TITLE, "openhub_ignore_list.json");
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
        if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (requestCode == EXPORT_REQUEST_CODE) {
            exportTo(uri);
        } else if (requestCode == IMPORT_REQUEST_CODE) {
            importFrom(uri);
        }
    }

    private void exportTo(final Uri uri) {
        IgnoredRepoHelper.getAllForExport()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(repos -> {
                    if (repos.isEmpty()) {
                        showWarningToast(getString(R.string.ignore_list_empty_export));
                        return;
                    }
                    Observable.fromCallable(() -> writeJsonToUri(uri, repos))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    count -> showSuccessToast(String.format(
                                            getString(R.string.export_ignore_list_success), count)),
                                    error -> showErrorToast(getString(R.string.export_ignore_list_failed))
                            );
                }, error -> showErrorToast(getString(R.string.export_ignore_list_failed)));
    }

    private void importFrom(final Uri uri) {
        Observable.fromCallable(() -> readReposFromUri(uri))
                .subscribeOn(Schedulers.io())
                .flatMap(IgnoredRepoHelper::importAll)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(count -> {
                    showSuccessToast(String.format(getString(R.string.import_ignore_list_success), count));
                    getFragment().reload();
                }, error -> showErrorToast(getString(R.string.import_ignore_list_failed)));
    }

    private int writeJsonToUri(Uri uri, ArrayList<IgnoredRepo> repos) throws IOException {
        Gson gson = new Gson();
        String json = gson.toJson(repos);
        OutputStream os = getContentResolver().openOutputStream(uri);
        if (os == null) throw new IOException("Could not open output stream for " + uri);
        try {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        } finally {
            os.close();
        }
        return repos.size();
    }

    private ArrayList<IgnoredRepo> readReposFromUri(Uri uri) throws IOException {
        InputStream is = getContentResolver().openInputStream(uri);
        if (is == null) throw new IOException("Could not open input stream for " + uri);
        String json;
        try {
            json = readAll(is);
        } finally {
            is.close();
        }
        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<IgnoredRepo>>() {}.getType();
        ArrayList<IgnoredRepo> repos = gson.fromJson(json, type);
        if (repos == null) throw new IOException("Not a valid ignore-list export");
        return repos;
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
