package com.thirtydegreesray.openhub.mvp.presenter;

import com.thirtydegreesray.openhub.dao.DaoSession;
import com.thirtydegreesray.openhub.dao.MyTopic;
import com.thirtydegreesray.openhub.dao.MyTopicDao;
import com.thirtydegreesray.openhub.mvp.contract.IMyTopicsContract;
import com.thirtydegreesray.openhub.mvp.model.TrendingLanguage;
import com.thirtydegreesray.openhub.mvp.presenter.base.BasePresenter;
import com.thirtydegreesray.openhub.util.TrendingLanguageHelper;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class MyTopicsPresenter extends BasePresenter<IMyTopicsContract.View>
        implements IMyTopicsContract.Presenter {

    private static final String[] DEFAULT_TOPICS = {
            "android", "machine-learning", "python", "javascript",
            "react", "docker", "kubernetes", "api", "cli", "game"
    };

    private ArrayList<TrendingLanguage> languages;

    @Inject
    public MyTopicsPresenter(DaoSession daoSession) {
        super(daoSession);
    }

    @Override
    public ArrayList<TrendingLanguage> getLanguagesFromLocal() {
        languages = TrendingLanguageHelper.getLanguagesFromLocal(daoSession, getContext());
        return languages;
    }

    public ArrayList<TrendingLanguage> getLanguages() {
        return languages;
    }

    /**
     * Must be called from initActivity(), not onViewInitialized() - the
     * activity's initView() (which reads getSelectedTopicSlugs() to build the
     * initial pager) runs before onViewInitialized() in BaseActivity's
     * lifecycle, so seeding there would be too late for the first render.
     */
    public void seedDefaultTopicsIfEmpty() {
        if (daoSession.getMyTopicDao().count() > 0) return;
        ArrayList<MyTopic> defaults = new ArrayList<>();
        int order = 0;
        for (String slug : DEFAULT_TOPICS) {
            defaults.add(new MyTopic(slug, ++order, true));
        }
        daoSession.getMyTopicDao().insertInTx(defaults);
    }

    public ArrayList<String> getSelectedTopicSlugs() {
        List<MyTopic> myTopics = daoSession.getMyTopicDao().queryBuilder()
                .where(MyTopicDao.Properties.Selected.eq(true))
                .orderAsc(MyTopicDao.Properties.Order)
                .list();
        ArrayList<String> slugs = new ArrayList<>();
        for (MyTopic topic : myTopics) {
            slugs.add(topic.getSlug());
        }
        return slugs;
    }

}
