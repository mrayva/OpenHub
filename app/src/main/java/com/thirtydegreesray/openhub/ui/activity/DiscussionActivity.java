package com.thirtydegreesray.openhub.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.View;

import com.thirtydegreesray.dataautoaccess.annotation.AutoAccess;
import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.R2;
import com.thirtydegreesray.openhub.mvp.contract.base.IBaseContract;
import com.thirtydegreesray.openhub.ui.activity.base.SingleFragmentActivity;
import com.thirtydegreesray.openhub.ui.fragment.DiscussionCommentsFragment;
import com.thirtydegreesray.openhub.ui.widget.ZoomAbleFloatingActionButton;
import com.thirtydegreesray.openhub.util.BundleHelper;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Single screen (title/category/body header as the flattened list's row 0,
 * see DiscussionPresenter) rather than a Files/Comments pager like Gists -
 * a discussion doesn't need separate tabs.
 */
public class DiscussionActivity extends
        SingleFragmentActivity<IBaseContract.Presenter, DiscussionCommentsFragment> {

    public static final int ADD_COMMENT_REQUEST_CODE = 700;
    public static final int REPLY_REQUEST_CODE = 701;
    public static final int EDIT_COMMENT_REQUEST_CODE = 702;

    public static void show(@NonNull Activity activity, @NonNull String owner,
                            @NonNull String repo, int number) {
        Intent intent = new Intent(activity, DiscussionActivity.class);
        intent.putExtras(BundleHelper.builder()
                .put("owner", owner).put("repo", repo).put("number", number).build());
        activity.startActivity(intent);
    }

    @AutoAccess String owner;
    @AutoAccess String repo;
    @AutoAccess int number;

    @BindView(R2.id.float_action_bn) ZoomAbleFloatingActionButton commentBn;

    private DiscussionCommentsFragment commentsFragment;

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        setToolbarTitle(getString(R.string.discussion), owner.concat("/").concat(repo));
        setToolbarScrollAble(true);
        commentBn.setVisibility(View.VISIBLE);
        commentBn.setImageResource(R.drawable.ic_add);
    }

    @Override
    protected DiscussionCommentsFragment createFragment() {
        return DiscussionCommentsFragment.create(owner, repo, number);
    }

    @OnClick(R2.id.float_action_bn)
    public void onCommentBnClicked() {
        MarkdownEditorActivity.show(getActivity(), R.string.comment, ADD_COMMENT_REQUEST_CODE, null);
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        if (fragment instanceof DiscussionCommentsFragment) {
            commentsFragment = (DiscussionCommentsFragment) fragment;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && data != null && commentsFragment != null) {
            String text = data.getExtras() == null ? null : data.getExtras().getString("text");
            if (requestCode == ADD_COMMENT_REQUEST_CODE) {
                commentsFragment.addComment(text);
                return;
            } else if (requestCode == REPLY_REQUEST_CODE) {
                commentsFragment.addReply(text);
                return;
            } else if (requestCode == EDIT_COMMENT_REQUEST_CODE) {
                commentsFragment.onEditComment(text);
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
