package com.thirtydegreesray.openhub.mvp.contract;

import com.thirtydegreesray.openhub.mvp.contract.base.IBaseContract;
import com.thirtydegreesray.openhub.mvp.model.graphql.DiscussionCategory;

import java.util.ArrayList;

public interface ICreateDiscussionContract {

    interface View extends IBaseContract.View {
        void showCategories(ArrayList<DiscussionCategory> categories);
        void onDiscussionCreated(int number);
    }

    interface Presenter extends IBaseContract.Presenter<View> {
        void submit(DiscussionCategory category, String title, String body);
    }

}
