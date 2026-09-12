

package com.thirtydegreesray.openhub.mvp.presenter;

import com.thirtydegreesray.dataautoaccess.annotation.AutoAccess;
import com.thirtydegreesray.openhub.dao.DaoSession;
import com.thirtydegreesray.openhub.http.core.HttpObserver;
import com.thirtydegreesray.openhub.http.core.HttpResponse;
import com.thirtydegreesray.openhub.http.error.HttpPageNoFoundError;
import com.thirtydegreesray.openhub.mvp.contract.IGistCommentsContract;
import com.thirtydegreesray.openhub.mvp.model.GistComment;
import com.thirtydegreesray.openhub.mvp.presenter.base.BasePagerPresenter;
import com.thirtydegreesray.openhub.util.StringUtils;

import java.util.ArrayList;

import javax.inject.Inject;

import retrofit2.Response;
import rx.Observable;

/**
 * Read-only this phase - no add/edit/delete yet (see plan).
 */
public class GistCommentsPresenter extends BasePagerPresenter<IGistCommentsContract.View>
        implements IGistCommentsContract.Presenter {

    @AutoAccess String gistId;

    private ArrayList<GistComment> comments;

    @Inject
    public GistCommentsPresenter(DaoSession daoSession) {
        super(daoSession);
    }

    @Override
    protected void loadData() {
        loadComments(1, false);
    }

    @Override
    public void loadComments(final int page, final boolean isReload) {
        mView.showLoading();
        final boolean readCacheFirst = !isReload && page == 1;

        HttpObserver<ArrayList<GistComment>> httpObserver = new HttpObserver<ArrayList<GistComment>>() {
            @Override
            public void onError(Throwable error) {
                mView.hideLoading();
                handleError(error);
            }

            @Override
            public void onSuccess(HttpResponse<ArrayList<GistComment>> response) {
                mView.hideLoading();
                if (isReload || readCacheFirst || comments == null || page == 1) {
                    comments = response.body();
                } else {
                    comments.addAll(response.body());
                }
                if (response.body().size() == 0 && comments.size() != 0) {
                    mView.setCanLoadMore(false);
                } else {
                    mView.showComments(comments);
                }
            }
        };

        generalRxHttpExecute(new IObservableCreator<ArrayList<GistComment>>() {
            @Override
            public Observable<Response<ArrayList<GistComment>>> createObservable(boolean forceNetWork) {
                return getGistService().getGistComments(forceNetWork, gistId, page);
            }
        }, httpObserver, readCacheFirst);
    }

    private void handleError(Throwable error) {
        if (!StringUtils.isBlankList(comments)) {
            mView.showErrorToast(getErrorTip(error));
        } else if (error instanceof HttpPageNoFoundError) {
            mView.showComments(new ArrayList<GistComment>());
        } else {
            mView.showLoadError(getErrorTip(error));
        }
    }

}
