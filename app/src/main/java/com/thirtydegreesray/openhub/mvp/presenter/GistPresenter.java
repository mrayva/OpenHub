

package com.thirtydegreesray.openhub.mvp.presenter;

import com.thirtydegreesray.dataautoaccess.annotation.AutoAccess;
import com.thirtydegreesray.openhub.dao.DaoSession;
import com.thirtydegreesray.openhub.http.core.HttpObserver;
import com.thirtydegreesray.openhub.http.core.HttpResponse;
import com.thirtydegreesray.openhub.mvp.contract.IGistContract;
import com.thirtydegreesray.openhub.mvp.model.Gist;
import com.thirtydegreesray.openhub.mvp.presenter.base.BasePresenter;

import javax.inject.Inject;

import retrofit2.Response;
import rx.Observable;

/**
 * Gist list endpoints (getMyGists/getPublicGists/etc.) never include file
 * content, only filename/size/language - only the single-gist endpoint does.
 * So GistActivity is launched with just the gist's id, and this always
 * re-fetches the full gist (with file content) rather than trusting whatever
 * summary object the list screen tapped.
 */
public class GistPresenter extends BasePresenter<IGistContract.View>
        implements IGistContract.Presenter {

    @AutoAccess String gistId;

    private Gist gist;

    @Inject
    public GistPresenter(DaoSession daoSession) {
        super(daoSession);
    }

    @Override
    public void onViewInitialized() {
        super.onViewInitialized();
        loadGist(false);
    }

    @Override
    public void loadGist(boolean isReload) {
        mView.showLoading();
        HttpObserver<Gist> httpObserver = new HttpObserver<Gist>() {
            @Override
            public void onError(Throwable error) {
                mView.hideLoading();
                mView.showErrorToast(getErrorTip(error));
            }

            @Override
            public void onSuccess(HttpResponse<Gist> response) {
                mView.hideLoading();
                gist = response.body();
                mView.showGist(gist);
            }
        };
        generalRxHttpExecute(new IObservableCreator<Gist>() {
            @Override
            public Observable<Response<Gist>> createObservable(boolean forceNetWork) {
                return getGistService().getGistInfo(forceNetWork, gistId);
            }
        }, httpObserver, !isReload);
    }

    public Gist getGist() {
        return gist;
    }

    public String getGistId() {
        return gistId;
    }

}
