

package com.thirtydegreesray.openhub.mvp.contract;

import com.thirtydegreesray.openhub.mvp.contract.base.IBaseContract;
import com.thirtydegreesray.openhub.mvp.contract.base.IBaseListContract;
import com.thirtydegreesray.openhub.mvp.contract.base.IBasePagerContract;
import com.thirtydegreesray.openhub.mvp.model.Repository;
import com.thirtydegreesray.openhub.mvp.model.filter.RepositoriesFilter;

import java.util.ArrayList;

/**
 * Created on 2017/7/18.
 *
 * @author ThirtyDegreesRay
 */

public interface IRepositoriesContract {

    interface View extends IBaseContract.View, IBasePagerContract.View, IBaseListContract.View {

        /**
         * @param appendedCount 0 means repositoryList is a fresh replacement
         *                      (full rebind); >0 means the last appendedCount
         *                      items were appended in place at the end of an
         *                      otherwise-unchanged list (safe to insert just
         *                      those rows instead of rebinding everything).
         */
        void showRepositories(ArrayList<Repository> repositoryList, int appendedCount);

    }

    interface Presenter extends IBasePagerContract.Presenter<IRepositoriesContract.View> {
        void loadRepositories(boolean isReLoad, int page);
        void loadRepositories(RepositoriesFilter filter);
    }

}
