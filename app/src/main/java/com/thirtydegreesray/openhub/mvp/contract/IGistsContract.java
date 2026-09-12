

package com.thirtydegreesray.openhub.mvp.contract;

import com.thirtydegreesray.openhub.mvp.contract.base.IBaseContract;
import com.thirtydegreesray.openhub.mvp.contract.base.IBaseListContract;
import com.thirtydegreesray.openhub.mvp.contract.base.IBasePagerContract;
import com.thirtydegreesray.openhub.mvp.model.Gist;

import java.util.ArrayList;

/**
 * Created for the Gists feature (viewing phase).
 */

public interface IGistsContract {

    interface View extends IBaseContract.View, IBasePagerContract.View, IBaseListContract.View {

        void showGists(ArrayList<Gist> gists, int appendedCount);

    }

    interface Presenter extends IBasePagerContract.Presenter<IGistsContract.View> {
        void loadGists(int page, boolean isReload);
    }

}
