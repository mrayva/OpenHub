

package com.thirtydegreesray.openhub.mvp.presenter;

import com.thirtydegreesray.dataautoaccess.annotation.AutoAccess;
import com.thirtydegreesray.openhub.AppData;
import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.dao.DaoSession;
import com.thirtydegreesray.openhub.http.core.HttpObserver;
import com.thirtydegreesray.openhub.http.core.HttpResponse;
import com.thirtydegreesray.openhub.http.error.HttpPageNoFoundError;
import com.thirtydegreesray.openhub.mvp.contract.IGistCommentsContract;
import com.thirtydegreesray.openhub.mvp.model.Gist;
import com.thirtydegreesray.openhub.mvp.model.GistComment;
import com.thirtydegreesray.openhub.mvp.model.request.CommentRequestModel;
import com.thirtydegreesray.openhub.mvp.presenter.base.BasePagerPresenter;
import com.thirtydegreesray.openhub.util.StringUtils;

import java.util.ArrayList;

import javax.inject.Inject;

import retrofit2.Response;
import rx.Observable;

public class GistCommentsPresenter extends BasePagerPresenter<IGistCommentsContract.View>
        implements IGistCommentsContract.Presenter {

    @AutoAccess Gist gist;

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
                return getGistService().getGistComments(forceNetWork, gist.getId(), page);
            }
        }, httpObserver, readCacheFirst);
    }

    @Override
    public void addComment(final String text) {
        if (StringUtils.isBlank(text)) {
            mView.showErrorToast(getString(R.string.comment_null_warning));
            return;
        }
        HttpObserver<GistComment> httpObserver = new HttpObserver<GistComment>() {
            @Override
            public void onError(Throwable error) {
                mView.showErrorToast(getErrorTip(error));
            }

            @Override
            public void onSuccess(HttpResponse<GistComment> response) {
                if (comments == null) comments = new ArrayList<>();
                comments.add(response.body());
                mView.showAddedComment(response.body());
                mView.showSuccessToast(getString(R.string.comment_success));
            }
        };
        generalRxHttpExecute(new IObservableCreator<GistComment>() {
            @Override
            public Observable<Response<GistComment>> createObservable(boolean forceNetWork) {
                return getGistService().createGistComment(gist.getId(), new CommentRequestModel(text));
            }
        }, httpObserver, false, mView.getProgressDialog(getLoadTip()));
    }

    @Override
    public void editComment(final String commentId, final String body) {
        if (StringUtils.isBlank(commentId)) {
            mView.showErrorToast(getString(R.string.comment_null_warning));
            return;
        }
        HttpObserver<GistComment> httpObserver = new HttpObserver<GistComment>() {
            @Override
            public void onError(Throwable error) {
                mView.showErrorToast(getErrorTip(error));
                mView.showEditCommentPage(commentId, body);
            }

            @Override
            public void onSuccess(HttpResponse<GistComment> response) {
                updateComment(response.body());
                mView.showComments(comments);
                mView.showSuccessToast(getString(R.string.comment_success));
            }
        };
        generalRxHttpExecute(new IObservableCreator<GistComment>() {
            @Override
            public Observable<Response<GistComment>> createObservable(boolean forceNetWork) {
                return getGistService().editGistComment(gist.getId(), commentId, new CommentRequestModel(body));
            }
        }, httpObserver, false, mView.getProgressDialog(getLoadTip()));
    }

    @Override
    public void deleteComment(String commentId) {
        executeSimpleRequest(getGistService().deleteGistComment(gist.getId(), commentId));
    }

    @Override
    public boolean isEditAndDeleteEnable(int position) {
        String loggedUser = AppData.INSTANCE.getLoggedUser().getLogin();
        return loggedUser.equals(gist.getOwner().getLogin()) ||
                loggedUser.equals(comments.get(position).getUser().getLogin());
    }

    private void updateComment(GistComment editedComment) {
        for (GistComment comment : comments) {
            if (editedComment.getId() == comment.getId()) {
                comment.setBody(editedComment.getBody());
            }
        }
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
