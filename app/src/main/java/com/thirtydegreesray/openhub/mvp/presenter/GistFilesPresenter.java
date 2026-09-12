

package com.thirtydegreesray.openhub.mvp.presenter;

import com.thirtydegreesray.dataautoaccess.annotation.AutoAccess;
import com.thirtydegreesray.openhub.dao.DaoSession;
import com.thirtydegreesray.openhub.mvp.contract.IGistFilesContract;
import com.thirtydegreesray.openhub.mvp.model.Gist;
import com.thirtydegreesray.openhub.mvp.presenter.base.BasePagerPresenter;

import javax.inject.Inject;

/**
 * A gist's file list comes fully loaded with the parent Gist object (see
 * GistPresenter's comment on why that object is always fetched fresh via
 * getGistInfo) - no separate network call needed here, unlike RepoFilesFragment.
 */
public class GistFilesPresenter extends BasePagerPresenter<IGistFilesContract.View>
        implements IGistFilesContract.Presenter {

    @AutoAccess Gist gist;

    @Inject
    public GistFilesPresenter(DaoSession daoSession) {
        super(daoSession);
    }

    @Override
    protected void loadData() {
        mView.showFiles(gist.getFileList());
    }

    public Gist getGist() {
        return gist;
    }

}
