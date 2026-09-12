

package com.thirtydegreesray.openhub.mvp.presenter;

import com.thirtydegreesray.dataautoaccess.annotation.AutoAccess;
import com.thirtydegreesray.openhub.AppData;
import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.dao.DaoSession;
import com.thirtydegreesray.openhub.http.core.HttpObserver;
import com.thirtydegreesray.openhub.http.core.HttpResponse;
import com.thirtydegreesray.openhub.mvp.contract.IGistContract;
import com.thirtydegreesray.openhub.mvp.model.Gist;
import com.thirtydegreesray.openhub.mvp.presenter.base.BasePresenter;

import javax.inject.Inject;

import okhttp3.ResponseBody;
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
    private boolean starred;
    private boolean isStatusChecked = false;

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
                checkStarred();
            }
        };
        generalRxHttpExecute(new IObservableCreator<Gist>() {
            @Override
            public Observable<Response<Gist>> createObservable(boolean forceNetWork) {
                return getGistService().getGistInfo(forceNetWork, gistId);
            }
        }, httpObserver, !isReload);
    }

    private void checkStarred() {
        if (isStatusChecked) return;
        isStatusChecked = true;
        checkStatus(
                getGistService().checkGistStarred(gistId),
                new CheckStatusCallback() {
                    @Override
                    public void onChecked(boolean status) {
                        starred = status;
                        mView.invalidateOptionsMenu();
                    }
                }
        );
    }

    @Override
    public boolean isStarred() {
        return starred;
    }

    @Override
    public void starGist(boolean star) {
        boolean originalStarred = starred;
        starred = star;
        mView.invalidateOptionsMenu();
        Observable<Response<ResponseBody>> observable = starred ?
                getGistService().starGist(gistId) :
                getGistService().unstarGist(gistId);
        HttpObserver<ResponseBody> httpObserver = new HttpObserver<ResponseBody>() {
            @Override
            public void onError(Throwable error) {
                starred = originalStarred;
                mView.invalidateOptionsMenu();
                mView.showErrorToast(getErrorTip(error));
            }

            @Override
            public void onSuccess(HttpResponse<ResponseBody> response) {
                mView.showSuccessToast(starred ?
                        getString(R.string.starred) : getString(R.string.unstarred));
            }
        };
        generalRxHttpExecute(new IObservableCreator<ResponseBody>() {
            @Override
            public Observable<Response<ResponseBody>> createObservable(boolean forceNetWork) {
                return observable;
            }
        }, httpObserver);
    }

    @Override
    public boolean isMine() {
        return gist != null && gist.getOwner() != null &&
                AppData.INSTANCE.getLoggedUser().getLogin().equals(gist.getOwner().getLogin());
    }

    @Override
    public void createFork() {
        mView.getProgressDialog(getLoadTip()).show();
        HttpObserver<Gist> httpObserver = new HttpObserver<Gist>() {
            @Override
            public void onError(Throwable error) {
                mView.showErrorToast(getErrorTip(error));
                mView.getProgressDialog(getLoadTip()).dismiss();
            }

            @Override
            public void onSuccess(HttpResponse<Gist> response) {
                mView.getProgressDialog(getLoadTip()).dismiss();
                if (response.body() != null) {
                    mView.showSuccessToast(getString(R.string.forked));
                    com.thirtydegreesray.openhub.ui.activity.GistActivity
                            .show(getContext(), response.body());
                } else {
                    mView.showErrorToast(getString(R.string.fork_failed));
                }
            }
        };
        generalRxHttpExecute(new IObservableCreator<Gist>() {
            @Override
            public Observable<Response<Gist>> createObservable(boolean forceNetWork) {
                return getGistService().forkGist(gistId);
            }
        }, httpObserver);
    }

    @Override
    public void deleteGist() {
        HttpObserver<ResponseBody> httpObserver = new HttpObserver<ResponseBody>() {
            @Override
            public void onError(Throwable error) {
                mView.showErrorToast(getErrorTip(error));
            }

            @Override
            public void onSuccess(HttpResponse<ResponseBody> response) {
                mView.onGistDeleted();
            }
        };
        generalRxHttpExecute(new IObservableCreator<ResponseBody>() {
            @Override
            public Observable<Response<ResponseBody>> createObservable(boolean forceNetWork) {
                return getGistService().deleteGist(gistId);
            }
        }, httpObserver, false, mView.getProgressDialog(getLoadTip()));
    }

    public Gist getGist() {
        return gist;
    }

    public String getGistId() {
        return gistId;
    }

}
