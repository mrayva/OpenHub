

package com.thirtydegreesray.openhub.mvp.contract;

import androidx.annotation.NonNull;

import com.thirtydegreesray.openhub.mvp.contract.base.IBaseContract;
import com.thirtydegreesray.openhub.mvp.contract.base.IBaseListContract;
import com.thirtydegreesray.openhub.mvp.contract.base.IBasePagerContract;
import com.thirtydegreesray.openhub.mvp.model.RepoCommit;

import java.util.ArrayList;

/**
 * Created by ThirtyDegreesRay on 2017/10/17 14:26:15
 */

public interface ICommitsContract {

    interface View extends IBaseContract.View, IBasePagerContract.View, IBaseListContract.View {
        /**
         * @param appendedCount 0 means commits is a fresh replacement (full
         *                      rebind); >0 means the last appendedCount items
         *                      were appended in place at the end of an
         *                      otherwise-unchanged list.
         */
        void showCommits(ArrayList<RepoCommit> commits, int appendedCount);
    }

    interface Presenter extends IBasePagerContract.Presenter<ICommitsContract.View>{
        void loadCommits(boolean isReload, int page);
    }

}
