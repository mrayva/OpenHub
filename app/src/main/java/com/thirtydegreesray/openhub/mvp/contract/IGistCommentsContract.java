

package com.thirtydegreesray.openhub.mvp.contract;

import com.thirtydegreesray.openhub.mvp.contract.base.IBaseContract;
import com.thirtydegreesray.openhub.mvp.contract.base.IBaseListContract;
import com.thirtydegreesray.openhub.mvp.contract.base.IBasePagerContract;
import com.thirtydegreesray.openhub.mvp.model.GistComment;

import java.util.ArrayList;

public interface IGistCommentsContract {

    interface View extends IBaseContract.View, IBasePagerContract.View, IBaseListContract.View {
        void showComments(ArrayList<GistComment> comments);
        void showAddedComment(GistComment comment);
        void showEditCommentPage(String commentId, String body);
    }

    interface Presenter extends IBasePagerContract.Presenter<IGistCommentsContract.View> {
        void loadComments(int page, boolean isReload);
        void addComment(String text);
        void editComment(String commentId, String body);
        void deleteComment(String commentId);
        boolean isEditAndDeleteEnable(int position);
    }

}
