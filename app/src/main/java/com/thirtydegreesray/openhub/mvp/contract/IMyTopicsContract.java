package com.thirtydegreesray.openhub.mvp.contract;

import com.thirtydegreesray.openhub.mvp.contract.base.IBaseContract;
import com.thirtydegreesray.openhub.mvp.model.TrendingLanguage;

import java.util.ArrayList;

public interface IMyTopicsContract {

    interface View extends IBaseContract.View {
    }

    interface Presenter extends IBaseContract.Presenter<IMyTopicsContract.View> {
        ArrayList<TrendingLanguage> getLanguagesFromLocal();
    }

}
