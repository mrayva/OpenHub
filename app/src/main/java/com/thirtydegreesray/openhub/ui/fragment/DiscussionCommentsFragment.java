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
import com.thirtydegreesray.openhub.mvp.contract.IDiscussionContract;
import com.thirtydegreesray.openhub.mvp.model.graphql.DiscussionComment;
import com.thirtydegreesray.openhub.mvp.presenter.DiscussionPresenter;
import com.thirtydegreesray.openhub.ui.activity.DiscussionActivity;
import com.thirtydegreesray.openhub.ui.activity.MarkdownEditorActivity;
import com.thirtydegreesray.openhub.ui.adapter.DiscussionCommentsAdapter;
import com.thirtydegreesray.openhub.ui.fragment.base.ListFragment;
import com.thirtydegreesray.openhub.util.BundleHelper;

import java.util.ArrayList;

public class DiscussionCommentsFragment extends ListFragment<DiscussionPresenter, DiscussionCommentsAdapter>
        implements IDiscussionContract.View {

    public static DiscussionCommentsFragment create(@NonNull String owner, @NonNull String repo, int number) {
        DiscussionCommentsFragment fragment = new DiscussionCommentsFragment();
        fragment.setArguments(BundleHelper.builder()
                .put("owner", owner).put("repo", repo).put("number", number).build());
        return fragment;
    }

    @AutoAccess String editingCommentId;
    @AutoAccess String pendingReplyToId;

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
        setAutoJudgeCanLoadMoreEnable(false);
        addVerticalDivider();
        adapter.setUpvoteClickListener(position -> mPresenter.toggleUpvote(adapter.getData().get(position)));
    }

    @Override
    protected void onReLoadData() {
        mPresenter.loadDiscussion(true);
    }

    @Override
    protected void onLoadMore(int page) {
        super.onLoadMore(page);
        mPresenter.loadDiscussion(false);
    }

    @Override
    protected String getEmptyTip() {
        return getString(R.string.no_comments);
    }

    @Override
    public void showItems(ArrayList<DiscussionComment> items) {
        adapter.setData(items);
        postNotifyDataSetChanged();
    }

    @Override
    public void showEditCommentPage(String commentId, String body) {
        editingCommentId = commentId;
        MarkdownEditorActivity.show(getActivity(), R.string.comment,
                DiscussionActivity.EDIT_COMMENT_REQUEST_CODE, body);
    }

    public void addComment(String text) {
        mPresenter.addComment(text, null);
    }

    public void addReply(String text) {
        mPresenter.addComment(text, pendingReplyToId);
    }

    public void onEditComment(String text) {
        mPresenter.editComment(editingCommentId, text);
    }

    @Override
    public boolean onItemLongClick(final int position, @NonNull View view) {
        final DiscussionComment comment = adapter.getData().get(position);
        if (comment.isHeader()) {
            return true;
        }

        final boolean canEditDelete = mPresenter.isEditAndDeleteEnable(comment);
        final boolean canReply = !comment.isReply();

        ArrayList<String> actionLabels = new ArrayList<>();
        if (canReply) actionLabels.add(getString(R.string.reply));
        if (canEditDelete) {
            actionLabels.add(getString(R.string.edit));
            actionLabels.add(getString(R.string.delete));
        }
        if (actionLabels.isEmpty()) return true;
        final String[] actions = actionLabels.toArray(new String[0]);

        new AlertDialog.Builder(getActivity())
                .setItems(actions, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        String selected = actions[which];
                        if (selected.equals(getString(R.string.reply))) {
                            pendingReplyToId = comment.getId();
                            MarkdownEditorActivity.show(getActivity(), R.string.reply,
                                    DiscussionActivity.REPLY_REQUEST_CODE, null);
                        } else if (selected.equals(getString(R.string.edit))) {
                            showEditCommentPage(comment.getId(), comment.getBodyHTML());
                        } else if (selected.equals(getString(R.string.delete))) {
                            showDeleteCommentWarning(comment.getId());
                        }
                    }
                })
                .show();
        return true;
    }

    private void showDeleteCommentWarning(final String commentId) {
        new AlertDialog.Builder(getActivity())
                .setCancelable(true)
                .setTitle(R.string.warning_dialog_tile)
                .setMessage(R.string.delete_discussion_comment_warning)
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
                        mPresenter.deleteComment(commentId);
                    }
                })
                .show();
    }

}
