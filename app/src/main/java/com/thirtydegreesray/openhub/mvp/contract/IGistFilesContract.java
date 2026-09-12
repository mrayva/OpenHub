

package com.thirtydegreesray.openhub.mvp.contract;

import com.thirtydegreesray.openhub.mvp.contract.base.IBaseContract;
import com.thirtydegreesray.openhub.mvp.contract.base.IBasePagerContract;
import com.thirtydegreesray.openhub.mvp.model.GistFile;

import java.util.ArrayList;

public interface IGistFilesContract {

    interface View extends IBaseContract.View, IBasePagerContract.View {
        void showFiles(ArrayList<GistFile> files);
    }

    interface Presenter extends IBasePagerContract.Presenter<IGistFilesContract.View> {
    }

}
