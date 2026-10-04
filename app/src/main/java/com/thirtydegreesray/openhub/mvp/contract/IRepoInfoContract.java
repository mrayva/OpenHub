

package com.thirtydegreesray.openhub.mvp.contract;

import com.thirtydegreesray.openhub.mvp.contract.base.IBaseContract;
import com.thirtydegreesray.openhub.mvp.contract.base.IBasePagerContract;
import com.thirtydegreesray.openhub.mvp.model.Repository;

/**
 * Created by ThirtyDegreesRay on 2017/8/11 11:33:00
 */

public interface IRepoInfoContract {

    interface View extends IBaseContract.View, IBasePagerContract.View{
        void showRepoInfo(Repository repository);
        void showReadMe(String content, String baseUrl);
        /** Unlike showReadMe(), always re-renders - for toggling translated/original. */
        void updateReadMe(String content, String baseUrl);
        void showReadMeLoader();
        void showNoReadMe();
        void showTranslateButton(boolean showingTranslation);
        void hideTranslateButton();
        void setTranslateButtonBusy(boolean busy);
        void showTranslateError(String message);
    }

    interface Presenter extends IBasePagerContract.Presenter<IRepoInfoContract.View>{
        void loadReadMe();
        void toggleReadmeTranslation();
    }

}
