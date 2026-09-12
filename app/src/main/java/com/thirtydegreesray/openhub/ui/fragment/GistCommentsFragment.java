

package com.thirtydegreesray.openhub.ui.fragment;

import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import android.view.View;

import com.thirtydegreesray.dataautoaccess.annotation.AutoAccess;
import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.inject.component.AppComponent;
import com.thirtydegreesray.openhub.inject.component.DaggerFragmentComponent;
import com.thirtydegreesray.openhub.inject.module.FragmentModule;
import com.thirtydegreesray.openhub.mvp.contract.IGistCommentsContract;
import com.thirtydegreesray.openhub.mvp.model.Gist;
import com.thirtydegreesray.openhub.mvp.model.GistComment;
import com.thirtydegreesray.openhub.mvp.presenter.GistCommentsPresenter;
import com.thirtydegreesray.openhub.ui.activity.GistActivity;
import com.thirtydegreesray.openhub.ui.adapter.GistCommentsAdapter;
import com.thirtydegreesray.openhub.ui.fragment.base.ListFragment;
import com.thirtydegreesray.openhub.util.AppOpener;
import com.thirtydegreesray.openhub.util.BundleHelper;

import java.util.ArrayList;

public class GistCommentsFragment extends ListFragment<GistCommentsPresenter, GistCommentsAdapter>
        implements IGistCommentsContract.View {

    public static GistCommentsFragment create(@NonNull Gist gist) {
        GistCommentsFragment fragment = new GistCommentsFragment();
        fragment.setArguments(BundleHelper.builder().put("gist", gist).build());
        return fragment;
    }

    @AutoAccess String editingCommentId;

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
    }

    @Override
    protected void onReLoadData() {
        mPresenter.loadComments(1, true);
    }

    @Override
    protected void onLoadMore(int page) {
        super.onLoadMore(page);
        mPresenter.loadComments(page, false);
    }

    @Override
    protected String getEmptyTip() {
        return getString(R.string.no_comments);
    }

    @Override
    public void showComments(ArrayList<GistComment> comments) {
        adapter.setData(comments);
        postNotifyDataSetChanged();
    }

    @Override
    public void showAddedComment(GistComment comment) {
        adapter.getData().add(comment);
        postNotifyDataSetChanged();
        recyclerView.smoothScrollToPosition(adapter.getItemCount() - 1);
    }

    @Override
    public void showEditCommentPage(String commentId, String body) {
        com.thirtydegreesray.openhub.ui.activity.MarkdownEditorActivity.show(getActivity(),
                R.string.comment, GistActivity.EDIT_GIST_COMMENT_REQUEST_CODE, body);
    }

    public void addComment(String text) {
        mPresenter.addComment(text);
    }

    public void onEditComment(String body) {
        mPresenter.editComment(editingCommentId, body);
    }

    @Override
    public boolean onItemLongClick(final int position, @NonNull View view) {
        final GistComment comment = adapter.getData().get(position);
        String[] actions;
        if (mPresenter.isEditAndDeleteEnable(position)) {
            actions = new String[]{getString(R.string.edit), getString(R.string.delete)};
        } else {
            return true;
        }
        new AlertDialog.Builder(getActivity())
                .setItems(actions, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        switch (which) {
                            case 0:
                                editingCommentId = String.valueOf(comment.getId());
                                showEditCommentPage(editingCommentId, comment.getBody());
                                break;
                            case 1:
                                showDeleteCommentWarning(position, String.valueOf(comment.getId()));
                                break;
                        }
                    }
                })
                .show();
        return true;
    }

    private void showDeleteCommentWarning(final int position, final String commentId) {
        new AlertDialog.Builder(getActivity())
                .setCancelable(true)
                .setTitle(R.string.warning_dialog_tile)
                .setMessage(R.string.delete_gist_comment_warning)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        adapter.getData().remove(position);
                        adapter.notifyItemRemoved(position);
                        mPresenter.deleteComment(commentId);
                    }
                })
                .show();
    }

    @Override
    public void onFragmentShowed() {
        super.onFragmentShowed();
        if (mPresenter != null) mPresenter.prepareLoadData();
    }

}
