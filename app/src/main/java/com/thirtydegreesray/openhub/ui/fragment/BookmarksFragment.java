package com.thirtydegreesray.openhub.ui.fragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.snackbar.Snackbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.inject.component.AppComponent;
import com.thirtydegreesray.openhub.inject.component.DaggerFragmentComponent;
import com.thirtydegreesray.openhub.inject.module.FragmentModule;
import com.thirtydegreesray.openhub.mvp.contract.IBookmarkContract;
import com.thirtydegreesray.openhub.mvp.model.BookmarkBackup;
import com.thirtydegreesray.openhub.mvp.model.BookmarkExt;
import com.thirtydegreesray.openhub.mvp.presenter.BookmarkPresenter;
import com.thirtydegreesray.openhub.ui.activity.ProfileActivity;
import com.thirtydegreesray.openhub.ui.activity.RepositoryActivity;
import com.thirtydegreesray.openhub.ui.adapter.BookmarksAdapter;
import com.thirtydegreesray.openhub.ui.adapter.base.ItemTouchHelperCallback;
import com.thirtydegreesray.openhub.ui.fragment.base.ListFragment;
import com.thirtydegreesray.openhub.util.BookmarkHelper;
import com.thirtydegreesray.openhub.util.PrefUtils;

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
 * Created by ThirtyDegreesRay on 2017/11/22 16:29:20
 */

public class BookmarksFragment extends ListFragment<BookmarkPresenter, BookmarksAdapter>
        implements IBookmarkContract.View, ItemTouchHelperCallback.ItemGestureListener {

    public static BookmarksFragment create(){
        return new BookmarksFragment();
    }

    private static final int EXPORT_REQUEST_CODE = 910;
    private static final int IMPORT_REQUEST_CODE = 911;

    private ItemTouchHelper itemTouchHelper;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_list;
    }

    @Override
    protected void setupFragmentComponent(AppComponent appComponent) {
        DaggerFragmentComponent.builder()
                .appComponent(appComponent)
                .fragmentModule(new FragmentModule(this))
                .build()
                .inject(this);
    }

    @Override
    protected void initFragment(Bundle savedInstanceState) {
        super.initFragment(savedInstanceState);
        setLoadMoreEnable(true);
        ItemTouchHelperCallback callback = new ItemTouchHelperCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, this);
        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_bookmarks, menu);
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
        intent.putExtra(Intent.EXTRA_TITLE, "openhub_bookmarks.json");
        startActivityForResult(intent, EXPORT_REQUEST_CODE);
    }

    private void startImport() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        startActivityForResult(intent, IMPORT_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
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
        BookmarkHelper.getAllForExport()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(backups -> {
                    if (backups.isEmpty()) {
                        showWarningToast(getString(R.string.bookmarks_empty_export));
                        return;
                    }
                    Observable.fromCallable(() -> writeJsonToUri(uri, backups))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    count -> showSuccessToast(String.format(
                                            getString(R.string.export_bookmarks_success), count)),
                                    error -> showErrorToast(getString(R.string.export_bookmarks_failed))
                            );
                }, error -> showErrorToast(getString(R.string.export_bookmarks_failed)));
    }

    private void importFrom(final Uri uri) {
        Observable.fromCallable(() -> readBackupsFromUri(uri))
                .subscribeOn(Schedulers.io())
                .flatMap(BookmarkHelper::importAll)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(count -> {
                    showSuccessToast(String.format(getString(R.string.import_bookmarks_success), count));
                    onReLoadData();
                }, error -> showErrorToast(getString(R.string.import_bookmarks_failed)));
    }

    private int writeJsonToUri(Uri uri, ArrayList<BookmarkBackup> backups) throws IOException {
        Gson gson = new Gson();
        String json = gson.toJson(backups);
        OutputStream os = getContext().getContentResolver().openOutputStream(uri);
        if (os == null) throw new IOException("Could not open output stream for " + uri);
        try {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        } finally {
            os.close();
        }
        return backups.size();
    }

    private ArrayList<BookmarkBackup> readBackupsFromUri(Uri uri) throws IOException {
        InputStream is = getContext().getContentResolver().openInputStream(uri);
        if (is == null) throw new IOException("Could not open input stream for " + uri);
        String json;
        try {
            json = readAll(is);
        } finally {
            is.close();
        }
        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<BookmarkBackup>>() {}.getType();
        ArrayList<BookmarkBackup> backups = gson.fromJson(json, type);
        if (backups == null) throw new IOException("Not a valid bookmarks export");
        return backups;
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

    @Override
    protected void onReLoadData() {
        mPresenter.loadBookmarks(1);
    }

    @Override
    protected String getEmptyTip() {
        return getString(R.string.no_bookmarks);
    }

    @Override
    public void onItemClick(int position, @NonNull View view) {
        super.onItemClick(position, view);
        BookmarkExt bookmark = adapter.getData().get(position);
        if("user".equals(bookmark.getType())){
            View userAvatar = view.findViewById(R.id.avatar);
            ProfileActivity.show(getActivity(), userAvatar, bookmark.getUser().getLogin(),
                    bookmark.getUser().getAvatarUrl());
        } else {
            RepositoryActivity.show(getActivity(), bookmark.getRepository().getOwner().getLogin(),
                    bookmark.getRepository().getName());
        }
    }

    @Override
    protected void onLoadMore(int page) {
        super.onLoadMore(page);
        mPresenter.loadBookmarks(page);
    }

    @Override
    public void showBookmarks(ArrayList<BookmarkExt> bookmarks) {
        adapter.setData(bookmarks);
        postNotifyDataSetChanged();

        if(bookmarks != null && bookmarks.size() > 0 && PrefUtils.isBookmarksTipAble()){
            showOperationTip(R.string.bookmarks_tip);
            PrefUtils.set(PrefUtils.BOOKMARKS_TIP_ABLE, false);
        }
    }

    @Override
    public void notifyItemAdded(int position) {
        if(adapter.getData().size() == 1){
            postNotifyDataSetChanged();
        } else {
            adapter.notifyItemInserted(position);
        }
    }

    @Override
    public boolean onItemMoved(int fromPosition, int toPosition) {
        return false;
    }

    @Override
    public void onItemSwiped(int position, int direction) {
        mPresenter.removeBookmark(position);
        if(adapter.getData().size() == 0){
            postNotifyDataSetChanged();
        } else {
            adapter.notifyItemRemoved(position);
        }
        Snackbar.make(recyclerView, R.string.bookmark_removed, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo, v -> mPresenter.undoRemoveBookmark() )
                .show();
    }

}
