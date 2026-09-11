package com.thirtydegreesray.openhub.mvp.contract;

import com.thirtydegreesray.openhub.dao.MyTopic;
import com.thirtydegreesray.openhub.mvp.contract.base.IBaseContract;
import com.thirtydegreesray.openhub.mvp.contract.base.IBaseListContract;

import java.util.ArrayList;

/**
 * "My Topics" editor: the user's persisted, selectable list of topics used by
 * MyTopicsActivity's multi-topic search.
 */
public interface ITopicsEditorContract {

    interface View extends IBaseContract.View, IBaseListContract.View{
        void showTopics(ArrayList<MyTopic> topics);
        void notifyItemInserted(int position);
    }

    interface Presenter extends IBaseContract.Presenter<ITopicsEditorContract.View>{
        void loadTopics();
        boolean addTopic(String slug);
        MyTopic removeTopic(int position);
        void undoRemoveTopic();
        void toggleSelected(int position);
        int getSelectedCount();
    }

}
