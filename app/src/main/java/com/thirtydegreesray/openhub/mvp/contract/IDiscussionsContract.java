package com.thirtydegreesray.openhub.mvp.contract;

import com.thirtydegreesray.openhub.mvp.contract.base.IBaseContract;
import com.thirtydegreesray.openhub.mvp.contract.base.IBaseListContract;
import com.thirtydegreesray.openhub.mvp.model.graphql.Discussion;
import com.thirtydegreesray.openhub.mvp.model.graphql.DiscussionCategory;

import java.util.ArrayList;

public interface IDiscussionsContract {

    interface View extends IBaseContract.View, IBaseListContract.View {
        void showDiscussions(ArrayList<Discussion> discussions);
        void showCategories(ArrayList<DiscussionCategory> categories);
    }

    interface Presenter extends IBaseContract.Presenter<IDiscussionsContract.View> {
        void loadDiscussions(int page, boolean isReload);
        void filterByCategory(DiscussionCategory category);
        ArrayList<DiscussionCategory> getCategories();
    }

}
