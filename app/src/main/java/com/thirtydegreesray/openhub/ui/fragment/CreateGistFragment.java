package com.thirtydegreesray.openhub.ui.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.snackbar.Snackbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.view.View;
import android.widget.EditText;

import com.thirtydegreesray.dataautoaccess.annotation.AutoAccess;
import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.R2;
import com.thirtydegreesray.openhub.inject.component.AppComponent;
import com.thirtydegreesray.openhub.inject.component.DaggerFragmentComponent;
import com.thirtydegreesray.openhub.inject.module.FragmentModule;
import com.thirtydegreesray.openhub.mvp.contract.ICreateGistContract;
import com.thirtydegreesray.openhub.mvp.model.Gist;
import com.thirtydegreesray.openhub.mvp.model.GistFileEntry;
import com.thirtydegreesray.openhub.mvp.presenter.CreateGistPresenter;
import com.thirtydegreesray.openhub.ui.activity.GistFileEditorActivity;
import com.thirtydegreesray.openhub.ui.adapter.CreateGistFilesAdapter;
import com.thirtydegreesray.openhub.ui.adapter.base.ItemTouchHelperCallback;
import com.thirtydegreesray.openhub.ui.fragment.base.ListFragment;
import com.thirtydegreesray.openhub.util.BundleHelper;
import com.thirtydegreesray.openhub.util.StringUtils;

import java.util.ArrayList;

import butterknife.BindView;

public class CreateGistFragment extends ListFragment<CreateGistPresenter, CreateGistFilesAdapter>
        implements ICreateGistContract.View, ItemTouchHelperCallback.ItemGestureListener {

    public static CreateGistFragment create() {
        return new CreateGistFragment();
    }

    public static CreateGistFragment createForEdit(@NonNull Gist gist) {
        CreateGistFragment fragment = new CreateGistFragment();
        fragment.setArguments(BundleHelper.builder().put("editingGist", gist).build());
        return fragment;
    }

    @BindView(R2.id.et_description) EditText etDescription;

    private final int ADD_FILE_REQUEST_CODE = 500;
    private final int EDIT_FILE_REQUEST_CODE = 501;

    @AutoAccess int editingFilePosition = -1;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_create_gist;
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
        setCanLoadMore(false);
        setRefreshEnable(false);
        ItemTouchHelperCallback callback = new ItemTouchHelperCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, this);
        new ItemTouchHelper(callback).attachToRecyclerView(recyclerView);
        addVerticalDivider();
        String initialDescription = mPresenter.getInitialDescription();
        if (!StringUtils.isBlank(initialDescription)) {
            etDescription.setText(initialDescription);
        }
    }

    @Override
    protected void onReLoadData() {
        mPresenter.loadFiles();
    }

    @Override
    protected String getEmptyTip() {
        return getString(R.string.no_files_add_one);
    }

    @Override
    public void showFiles(ArrayList<GistFileEntry> files) {
        adapter.setData(files);
        postNotifyDataSetChanged();
    }

    @Override
    public void notifyItemInserted(int position) {
        adapter.notifyItemInserted(position);
        recyclerView.scrollToPosition(position);
    }

    @Override
    public void onGistSaved(Gist gist) {
        Intent data = new Intent();
        data.putExtra("gist", gist);
        getActivity().setResult(Activity.RESULT_OK, data);
        getActivity().finish();
    }

    @Override
    public void onItemClick(int position, @NonNull View view) {
        super.onItemClick(position, view);
        editingFilePosition = position;
        GistFileEntry entry = adapter.getData().get(position);
        GistFileEditorActivity.showForResult(getActivity(), entry.getFilename(),
                entry.getContent(), EDIT_FILE_REQUEST_CODE);
    }

    public void addFile() {
        GistFileEditorActivity.showForResult(getActivity(), ADD_FILE_REQUEST_CODE);
    }

    @Override
    public boolean onItemMoved(int fromPosition, int toPosition) {
        return false;
    }

    @Override
    public void onItemSwiped(int position, int direction) {
        GistFileEntry removed = mPresenter.removeFile(position);
        if (adapter.getData().size() == 0) {
            postNotifyDataSetChanged();
        } else {
            adapter.notifyItemRemoved(position);
        }
        String tip = String.format(getString(R.string.file_removed), removed.getFilename());
        Snackbar.make(recyclerView, tip, Snackbar.LENGTH_SHORT)
                .setAction(R.string.undo, v -> mPresenter.undoRemoveFile())
                .show();
    }

    public void onFilePicked(int requestCode, String filename, String content) {
        if (requestCode == ADD_FILE_REQUEST_CODE) {
            if (!mPresenter.addFile(filename, content)) {
                showWarningToast(getString(R.string.file_name_already_used));
            }
        } else if (requestCode == EDIT_FILE_REQUEST_CODE && editingFilePosition >= 0) {
            if (!mPresenter.updateFile(editingFilePosition, filename, content)) {
                showWarningToast(getString(R.string.file_name_already_used));
            }
        }
    }

    public String getDescription() {
        return etDescription.getText().toString().trim();
    }

    public boolean isEditMode() {
        return mPresenter.isEditMode();
    }

    public void submitCreate(boolean isPublic) {
        mPresenter.submitCreate(getDescription(), isPublic);
    }

    public void submitEdit() {
        mPresenter.submitEdit(getDescription());
    }

}
