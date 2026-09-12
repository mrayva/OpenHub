package com.thirtydegreesray.openhub.mvp.presenter;

import com.thirtydegreesray.dataautoaccess.annotation.AutoAccess;
import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.dao.DaoSession;
import com.thirtydegreesray.openhub.http.core.HttpObserver;
import com.thirtydegreesray.openhub.http.core.HttpResponse;
import com.thirtydegreesray.openhub.mvp.contract.IDiscussionContract;
import com.thirtydegreesray.openhub.mvp.model.graphql.Discussion;
import com.thirtydegreesray.openhub.mvp.model.graphql.DiscussionComment;
import com.thirtydegreesray.openhub.mvp.model.graphql.DiscussionCommentConnection;
import com.thirtydegreesray.openhub.mvp.model.graphql.DiscussionQueryData;
import com.thirtydegreesray.openhub.mvp.model.graphql.GraphQLResponse;
import com.thirtydegreesray.openhub.mvp.model.graphql.PageInfo;
import com.thirtydegreesray.openhub.mvp.model.request.GraphQLRequest;
import com.thirtydegreesray.openhub.mvp.presenter.base.BasePresenter;
import com.thirtydegreesray.openhub.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import retrofit2.Response;
import rx.Observable;

/**
 * One GraphQL query returns the discussion itself AND a page of its
 * comments+replies together - see the flattened list this builds: item 0 is
 * a synthesized "header" row (the discussion's own body, via
 * DiscussionComment.forHeader()) followed by each top-level comment and,
 * immediately after it, its replies (marked via DiscussionComment.setReply) -
 * GitHub discussions only nest one level deep, so this flattening never loses
 * structure, it just avoids a nested RecyclerView-in-RecyclerView.
 */
public class DiscussionPresenter extends BasePresenter<IDiscussionContract.View>
        implements IDiscussionContract.Presenter {

    private static final String DISCUSSION_QUERY = """
            query($owner:String!, $repo:String!, $number:Int!, $after:String) {
              repository(owner:$owner, name:$repo) {
                discussion(number:$number) {
                  id
                  number
                  title
                  bodyHTML
                  createdAt
                  updatedAt
                  upvoteCount
                  isAnswered
                  url
                  author { login avatarUrl }
                  category { id name emoji }
                  comments(first: 20, after: $after) {
                    pageInfo { hasNextPage endCursor }
                    nodes {
                      id
                      bodyHTML
                      createdAt
                      upvoteCount
                      isAnswer
                      author { login avatarUrl }
                      replies(first: 10) {
                        nodes {
                          id
                          bodyHTML
                          createdAt
                          upvoteCount
                          isAnswer
                          author { login avatarUrl }
                        }
                      }
                    }
                  }
                }
              }
            }
            """;

    @AutoAccess String owner;
    @AutoAccess String repo;
    @AutoAccess int number;

    private ArrayList<DiscussionComment> items;
    private String endCursor;

    @Inject
    public DiscussionPresenter(DaoSession daoSession) {
        super(daoSession);
    }

    @Override
    public void onViewInitialized() {
        super.onViewInitialized();
        loadDiscussion(false);
    }

    @Override
    public void loadDiscussion(final boolean isReload) {
        final boolean freshLoad = isReload || items == null;
        if (freshLoad) {
            endCursor = null;
        }
        mView.showLoading();

        Map<String, Object> variables = new HashMap<>();
        variables.put("owner", owner);
        variables.put("repo", repo);
        variables.put("number", number);
        variables.put("after", endCursor);
        GraphQLRequest request = new GraphQLRequest(DISCUSSION_QUERY, variables);

        HttpObserver<GraphQLResponse<DiscussionQueryData>> httpObserver =
                new HttpObserver<GraphQLResponse<DiscussionQueryData>>() {
            @Override
            public void onError(Throwable error) {
                mView.hideLoading();
                if (!StringUtils.isBlankList(items)) {
                    mView.showErrorToast(getErrorTip(error));
                } else {
                    mView.showLoadError(getErrorTip(error));
                }
            }

            @Override
            public void onSuccess(HttpResponse<GraphQLResponse<DiscussionQueryData>> response) {
                mView.hideLoading();
                GraphQLResponse<DiscussionQueryData> body = response.body();
                if (body.hasErrors()) {
                    if (!StringUtils.isBlankList(items)) {
                        mView.showErrorToast(body.getErrorMessage());
                    } else {
                        mView.showLoadError(body.getErrorMessage());
                    }
                    return;
                }

                // See DiscussionsPresenter's identical guard: GitHub's edge
                // can reject a request with a plain REST-style error body
                // that has neither "data" nor "errors", so hasErrors() alone
                // isn't enough - check getData() itself before dereferencing.
                Discussion discussion = (body.getData() == null || body.getData().getRepository() == null) ?
                        null : body.getData().getRepository().getDiscussion();
                if (discussion == null) {
                    mView.showLoadError(getString(R.string.no_data));
                    return;
                }

                if (freshLoad) {
                    items = new ArrayList<>();
                    items.add(DiscussionComment.forHeader(discussion));
                }

                DiscussionCommentConnection comments = discussion.getComments();
                if (comments != null && comments.getNodes() != null) {
                    for (DiscussionComment comment : comments.getNodes()) {
                        items.add(comment);
                        if (comment.getReplies() != null && comment.getReplies().getNodes() != null) {
                            for (DiscussionComment reply : comment.getReplies().getNodes()) {
                                reply.setReply(true);
                                items.add(reply);
                            }
                        }
                    }
                }

                PageInfo pageInfo = comments == null ? null : comments.getPageInfo();
                endCursor = pageInfo == null ? null : pageInfo.getEndCursor();
                mView.setCanLoadMore(pageInfo != null && pageInfo.isHasNextPage());
                mView.showItems(items);
            }
        };

        generalRxHttpExecute(new IObservableCreator<GraphQLResponse<DiscussionQueryData>>() {
            @Override
            public Observable<Response<GraphQLResponse<DiscussionQueryData>>> createObservable(boolean forceNetWork) {
                return getGraphQLService().getDiscussion(request);
            }
        }, httpObserver);
    }

}
