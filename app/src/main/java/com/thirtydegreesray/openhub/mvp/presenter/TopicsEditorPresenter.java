package com.thirtydegreesray.openhub.mvp.presenter;

import com.thirtydegreesray.openhub.dao.DaoSession;
import com.thirtydegreesray.openhub.dao.MyTopic;
import com.thirtydegreesray.openhub.dao.MyTopicDao;
import com.thirtydegreesray.openhub.mvp.contract.ITopicsEditorContract;
import com.thirtydegreesray.openhub.mvp.presenter.base.BasePresenter;
import com.thirtydegreesray.openhub.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * Manages the user's persisted "My Topics" list: add/remove/toggle-selected,
 * each change written straight to the DB (no separate commit step - simpler,
 * and survives the activity being killed mid-edit).
 */
public class TopicsEditorPresenter extends BasePresenter<ITopicsEditorContract.View>
        implements ITopicsEditorContract.Presenter {

    private ArrayList<MyTopic> topics;
    private MyTopic removedTopic;
    private int removedPosition;

    @Inject
    public TopicsEditorPresenter(DaoSession daoSession) {
        super(daoSession);
    }

    @Override
    public void onViewInitialized() {
        super.onViewInitialized();
        loadTopics();
    }

    @Override
    public void loadTopics() {
        List<MyTopic> myTopics = daoSession.getMyTopicDao().queryBuilder()
                .orderAsc(MyTopicDao.Properties.Order)
                .list();
        topics = new ArrayList<>(myTopics);
        mView.showTopics(topics);
        mView.hideLoading();
    }

    @Override
    public boolean addTopic(String slug) {
        if (StringUtils.isBlank(slug)) return false;
        final String normalized = slug.trim().toLowerCase();
        for (MyTopic topic : topics) {
            if (topic.getSlug().equals(normalized)) return false;
        }
        int maxOrder = 0;
        for (MyTopic topic : topics) {
            if (topic.getOrder() > maxOrder) maxOrder = topic.getOrder();
        }
        MyTopic newTopic = new MyTopic(normalized, maxOrder + 1, true);
        daoSession.getMyTopicDao().insertOrReplace(newTopic);
        topics.add(newTopic);
        mView.notifyItemInserted(topics.size() - 1);
        return true;
    }

    @Override
    public MyTopic removeTopic(int position) {
        removedTopic = topics.remove(position);
        removedPosition = position;
        daoSession.getMyTopicDao().deleteByKey(removedTopic.getSlug());
        return removedTopic;
    }

    @Override
    public void undoRemoveTopic() {
        topics.add(removedPosition, removedTopic);
        daoSession.getMyTopicDao().insertOrReplace(removedTopic);
        mView.notifyItemInserted(removedPosition);
    }

    @Override
    public void toggleSelected(int position) {
        MyTopic topic = topics.get(position);
        topic.setSelected(!topic.getSelected());
        daoSession.getMyTopicDao().insertOrReplace(topic);
    }

    @Override
    public int getSelectedCount() {
        int count = 0;
        for (MyTopic topic : topics) {
            if (topic.getSelected()) count++;
        }
        return count;
    }

}
