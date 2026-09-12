package com.thirtydegreesray.openhub.mvp.contract;

import com.thirtydegreesray.openhub.mvp.contract.base.IBaseContract;
import com.thirtydegreesray.openhub.mvp.contract.base.IBaseListContract;
import com.thirtydegreesray.openhub.mvp.model.graphql.DiscussionComment;

import java.util.ArrayList;

public interface IDiscussionContract {

    interface View extends IBaseContract.View, IBaseListContract.View {
        void showItems(ArrayList<DiscussionComment> items);
    }

    interface Presenter extends IBaseContract.Presenter<IDiscussionContract.View> {
        void loadDiscussion(boolean isReload);
    }

}
