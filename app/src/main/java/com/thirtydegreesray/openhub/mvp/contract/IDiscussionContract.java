package com.thirtydegreesray.openhub.mvp.contract;

import com.thirtydegreesray.openhub.mvp.contract.base.IBaseContract;
import com.thirtydegreesray.openhub.mvp.contract.base.IBaseListContract;
import com.thirtydegreesray.openhub.mvp.model.graphql.DiscussionComment;

import java.util.ArrayList;

public interface IDiscussionContract {

    interface View extends IBaseContract.View, IBaseListContract.View {
        // Also reused after addComment()/editComment()/deleteComment()/
        // toggleUpvote() succeed - simpler than tracking exactly where a
        // reply landed in the flattened list, and cheap enough for a
        // comment-thread-sized list.
        void showItems(ArrayList<DiscussionComment> items);
        void showEditCommentPage(String commentId, String body);
    }

    interface Presenter extends IBaseContract.Presenter<IDiscussionContract.View> {
        void loadDiscussion(boolean isReload);
        void addComment(String body, String replyToId);
        void editComment(String commentId, String body);
        void deleteComment(String commentId);
        boolean isEditAndDeleteEnable(DiscussionComment comment);
        void toggleUpvote(DiscussionComment item);
    }

}
