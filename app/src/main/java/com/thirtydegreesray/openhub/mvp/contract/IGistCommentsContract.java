

package com.thirtydegreesray.openhub.mvp.contract;

import com.thirtydegreesray.openhub.mvp.contract.base.IBaseContract;
import com.thirtydegreesray.openhub.mvp.contract.base.IBaseListContract;
import com.thirtydegreesray.openhub.mvp.contract.base.IBasePagerContract;
import com.thirtydegreesray.openhub.mvp.model.GistComment;

import java.util.ArrayList;

/**
 * Read-only this phase - no add/edit/delete yet (see plan).
 */
public interface IGistCommentsContract {

    interface View extends IBaseContract.View, IBasePagerContract.View, IBaseListContract.View {
        void showComments(ArrayList<GistComment> comments);
    }

    interface Presenter extends IBasePagerContract.Presenter<IGistCommentsContract.View> {
        void loadComments(int page, boolean isReload);
    }

}
