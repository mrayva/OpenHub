

package com.thirtydegreesray.openhub.mvp.presenter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.thirtydegreesray.dataautoaccess.annotation.AutoAccess;
import com.thirtydegreesray.openhub.dao.DaoSession;
import com.thirtydegreesray.openhub.http.core.HttpObserver;
import com.thirtydegreesray.openhub.http.core.HttpResponse;
import com.thirtydegreesray.openhub.http.error.HttpPageNoFoundError;
import com.thirtydegreesray.openhub.mvp.contract.IGistsContract;
import com.thirtydegreesray.openhub.mvp.model.Gist;
import com.thirtydegreesray.openhub.mvp.presenter.base.BasePagerPresenter;
import com.thirtydegreesray.openhub.ui.fragment.GistsFragment;
import com.thirtydegreesray.openhub.util.StringUtils;

import java.util.ArrayList;

import javax.inject.Inject;

import retrofit2.Response;
import rx.Observable;

/**
 * One shared presenter for every gists list variant (MY/STARRED/PUBLIC/USER) -
 * mirrors RepositoriesPresenter's type-enum-driven design rather than
 * FastHub-RE's three separate near-duplicate presenters for the same idea.
 */
public class GistsPresenter extends BasePagerPresenter<IGistsContract.View>
        implements IGistsContract.Presenter {

    @AutoAccess GistsFragment.GistsType type;
    @AutoAccess String user;

    private ArrayList<Gist> gists;

    @Inject
    public GistsPresenter(DaoSession daoSession) {
        super(daoSession);
    }

    @Override
    protected void loadData() {
        loadGists(1, false);
    }

    @Override
    public void loadGists(final int page, final boolean isReload) {
        mView.showLoading();
        final boolean readCacheFirst = !isReload && page == 1;

        HttpObserver<ArrayList<Gist>> httpObserver = new HttpObserver<ArrayList<Gist>>() {
            @Override
            public void onError(@NonNull Throwable error) {
                mView.hideLoading();
                handleError(error);
            }

            @Override
            public void onSuccess(@NonNull HttpResponse<ArrayList<Gist>> response) {
                mView.hideLoading();
                int appendedCount;
                if (isReload || readCacheFirst || gists == null || page == 1) {
                    gists = response.body();
                    appendedCount = 0;
                } else {
                    appendedCount = response.body().size();
                    gists.addAll(response.body());
                }
                if (response.body().size() == 0 && gists.size() != 0) {
                    mView.setCanLoadMore(false);
                } else {
                    mView.showGists(gists, appendedCount);
                }
            }
        };

        generalRxHttpExecute(new IObservableCreator<ArrayList<Gist>>() {
            @Nullable
            @Override
            public Observable<Response<ArrayList<Gist>>> createObservable(boolean forceNetWork) {
                return getObservable(forceNetWork, page);
            }
        }, httpObserver, readCacheFirst);
    }

    private Observable<Response<ArrayList<Gist>>> getObservable(boolean forceNetWork, int page) {
        switch (type) {
            case MY:
                return getGistService().getMyGists(forceNetWork, page);
            case STARRED:
                return getGistService().getStarredGists(forceNetWork, page);
            case PUBLIC:
                return getGistService().getPublicGists(forceNetWork, page);
            case USER:
                return getGistService().getUserGists(forceNetWork, user, page);
            default:
                return null;
        }
    }

    private void handleError(Throwable error) {
        if (!StringUtils.isBlankList(gists)) {
            mView.showErrorToast(getErrorTip(error));
        } else if (error instanceof HttpPageNoFoundError) {
            mView.showGists(new ArrayList<Gist>(), 0);
        } else {
            mView.showLoadError(getErrorTip(error));
        }
    }

}
