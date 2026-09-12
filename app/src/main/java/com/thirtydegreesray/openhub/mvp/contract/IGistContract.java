

package com.thirtydegreesray.openhub.mvp.contract;

import com.thirtydegreesray.openhub.mvp.contract.base.IBaseContract;
import com.thirtydegreesray.openhub.mvp.model.Gist;

public interface IGistContract {

    interface View extends IBaseContract.View {
        void showGist(Gist gist);
    }

    interface Presenter extends IBaseContract.Presenter<IGistContract.View> {
        void loadGist(boolean isReload);
    }

}
