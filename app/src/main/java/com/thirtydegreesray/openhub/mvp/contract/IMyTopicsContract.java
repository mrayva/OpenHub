package com.thirtydegreesray.openhub.mvp.contract;

import com.thirtydegreesray.openhub.mvp.contract.base.IBaseContract;

public interface IMyTopicsContract {

    interface View extends IBaseContract.View {
    }

    interface Presenter extends IBaseContract.Presenter<IMyTopicsContract.View> {
    }

}
