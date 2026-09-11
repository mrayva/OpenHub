package com.thirtydegreesray.openhub.mvp.contract;

import com.thirtydegreesray.openhub.mvp.contract.base.IBaseContract;
import com.thirtydegreesray.openhub.mvp.contract.base.IBaseListContract;
import com.thirtydegreesray.openhub.mvp.contract.base.IBasePagerContract;
import com.thirtydegreesray.openhub.mvp.model.Issue;
import com.thirtydegreesray.openhub.mvp.model.filter.IssuesFilter;

import java.util.ArrayList;

/**
 * Created by ThirtyDegreesRay on 2017/9/20 14:55:29
 */

public interface IIssuesContract {

    interface View extends IBaseContract.View, IBasePagerContract.View, IBaseListContract.View{
        /**
         * @param appendedCount 0 means issues is a fresh replacement (full
         *                      rebind); >0 means the last appendedCount items
         *                      were appended in place at the end of an
         *                      otherwise-unchanged list.
         */
        void showIssues(ArrayList<Issue> issues, int appendedCount);
    }

    interface Presenter extends IBasePagerContract.Presenter<IIssuesContract.View>{
        void loadIssues(int page, boolean isReload);
        void loadIssues(IssuesFilter issuesFilter, int page, boolean isReload);
    }

}
