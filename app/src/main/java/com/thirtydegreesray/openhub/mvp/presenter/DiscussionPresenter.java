package com.thirtydegreesray.openhub.mvp.presenter;

import com.thirtydegreesray.dataautoaccess.annotation.AutoAccess;
import com.thirtydegreesray.openhub.AppData;
import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.dao.DaoSession;
import com.thirtydegreesray.openhub.http.core.HttpObserver;
import com.thirtydegreesray.openhub.http.core.HttpResponse;
import com.thirtydegreesray.openhub.mvp.contract.IDiscussionContract;
import com.thirtydegreesray.openhub.mvp.model.graphql.AddDiscussionCommentData;
import com.thirtydegreesray.openhub.mvp.model.graphql.Discussion;
import com.thirtydegreesray.openhub.mvp.model.graphql.DiscussionComment;
import com.thirtydegreesray.openhub.mvp.model.graphql.DiscussionCommentConnection;
import com.thirtydegreesray.openhub.mvp.model.graphql.DiscussionQueryData;
import com.thirtydegreesray.openhub.mvp.model.graphql.GraphQLResponse;
import com.thirtydegreesray.openhub.mvp.model.graphql.PageInfo;
import com.thirtydegreesray.openhub.mvp.model.graphql.UpdateDiscussionCommentData;
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
                  viewerHasUpvoted
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
                      viewerHasUpvoted
                      isAnswer
                      author { login avatarUrl }
                      replies(first: 10) {
                        nodes {
                          id
                          bodyHTML
                          createdAt
                          upvoteCount
                          viewerHasUpvoted
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

    private static final String ADD_COMMENT_MUTATION = """
            mutation($discussionId:ID!, $body:String!, $replyToId:ID) {
              addDiscussionComment(input: {discussionId:$discussionId, body:$body, replyToId:$replyToId}) {
                comment {
                  id bodyHTML createdAt upvoteCount viewerHasUpvoted isAnswer
                  author { login avatarUrl }
                }
              }
            }
            """;

    private static final String UPDATE_COMMENT_MUTATION = """
            mutation($commentId:ID!, $body:String!) {
              updateDiscussionComment(input: {commentId:$commentId, body:$body}) {
                comment { id bodyHTML }
              }
            }
            """;

    private static final String DELETE_COMMENT_MUTATION = """
            mutation($id:ID!) {
              deleteDiscussionComment(input: {id:$id}) {
                clientMutationId
              }
            }
            """;

    private static final String ADD_UPVOTE_MUTATION = """
            mutation($subjectId:ID!) {
              addUpvote(input: {subjectId:$subjectId}) {
                clientMutationId
              }
            }
            """;

    private static final String REMOVE_UPVOTE_MUTATION = """
            mutation($subjectId:ID!) {
              removeUpvote(input: {subjectId:$subjectId}) {
                clientMutationId
              }
            }
            """;

    @AutoAccess String owner;
    @AutoAccess String repo;
    @AutoAccess int number;

    private String discussionId;
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
                discussionId = discussion.getId();

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

    @Override
    public void addComment(final String body, final String replyToId) {
        if (StringUtils.isBlank(body)) {
            mView.showErrorToast(getString(R.string.comment_null_warning));
            return;
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put("discussionId", discussionId);
        variables.put("body", body);
        if (replyToId != null) variables.put("replyToId", replyToId);
        final GraphQLRequest request = new GraphQLRequest(ADD_COMMENT_MUTATION, variables);

        HttpObserver<GraphQLResponse<AddDiscussionCommentData>> httpObserver =
                new HttpObserver<GraphQLResponse<AddDiscussionCommentData>>() {
            @Override
            public void onError(Throwable error) {
                mView.showErrorToast(getErrorTip(error));
            }

            @Override
            public void onSuccess(HttpResponse<GraphQLResponse<AddDiscussionCommentData>> response) {
                GraphQLResponse<AddDiscussionCommentData> result = response.body();
                DiscussionComment newComment = (result.getData() == null || result.getData().getAddDiscussionComment() == null) ?
                        null : result.getData().getAddDiscussionComment().getComment();
                if (result.hasErrors() || newComment == null) {
                    mView.showErrorToast(result.hasErrors() ? result.getErrorMessage() : getString(R.string.no_data));
                    return;
                }
                if (replyToId != null) {
                    newComment.setReply(true);
                    insertAfterParent(replyToId, newComment);
                } else {
                    items.add(newComment);
                }
                mView.showItems(items);
                mView.showSuccessToast(getString(R.string.comment_success));
            }
        };
        generalRxHttpExecute(new IObservableCreator<GraphQLResponse<AddDiscussionCommentData>>() {
            @Override
            public Observable<Response<GraphQLResponse<AddDiscussionCommentData>>> createObservable(boolean forceNetWork) {
                return getGraphQLService().addDiscussionComment(request);
            }
        }, httpObserver, false, mView.getProgressDialog(getLoadTip()));
    }

    private void insertAfterParent(String parentId, DiscussionComment reply) {
        for (int i = 0; i < items.size(); i++) {
            if (parentId.equals(items.get(i).getId())) {
                int insertAt = i + 1;
                while (insertAt < items.size() && items.get(insertAt).isReply()) {
                    insertAt++;
                }
                items.add(insertAt, reply);
                return;
            }
        }
        items.add(reply);
    }

    @Override
    public void editComment(final String commentId, final String body) {
        if (StringUtils.isBlank(commentId) || StringUtils.isBlank(body)) {
            mView.showErrorToast(getString(R.string.comment_null_warning));
            return;
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put("commentId", commentId);
        variables.put("body", body);
        GraphQLRequest request = new GraphQLRequest(UPDATE_COMMENT_MUTATION, variables);

        HttpObserver<GraphQLResponse<UpdateDiscussionCommentData>> httpObserver =
                new HttpObserver<GraphQLResponse<UpdateDiscussionCommentData>>() {
            @Override
            public void onError(Throwable error) {
                mView.showErrorToast(getErrorTip(error));
                mView.showEditCommentPage(commentId, body);
            }

            @Override
            public void onSuccess(HttpResponse<GraphQLResponse<UpdateDiscussionCommentData>> response) {
                GraphQLResponse<UpdateDiscussionCommentData> result = response.body();
                if (result.hasErrors() || result.getData() == null || result.getData().getUpdateDiscussionComment() == null) {
                    mView.showErrorToast(result.hasErrors() ? result.getErrorMessage() : getString(R.string.no_data));
                    mView.showEditCommentPage(commentId, body);
                    return;
                }
                for (DiscussionComment item : items) {
                    if (commentId.equals(item.getId())) {
                        item.setBodyHTML(body);
                        break;
                    }
                }
                mView.showItems(items);
                mView.showSuccessToast(getString(R.string.comment_success));
            }
        };
        generalRxHttpExecute(new IObservableCreator<GraphQLResponse<UpdateDiscussionCommentData>>() {
            @Override
            public Observable<Response<GraphQLResponse<UpdateDiscussionCommentData>>> createObservable(boolean forceNetWork) {
                return getGraphQLService().updateDiscussionComment(request);
            }
        }, httpObserver, false, mView.getProgressDialog(getLoadTip()));
    }

    @Override
    public void deleteComment(final String commentId) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("id", commentId);
        GraphQLRequest request = new GraphQLRequest(DELETE_COMMENT_MUTATION, variables);

        HttpObserver<GraphQLResponse<Object>> httpObserver = new HttpObserver<GraphQLResponse<Object>>() {
            @Override
            public void onError(Throwable error) {
                mView.showErrorToast(getErrorTip(error));
            }

            @Override
            public void onSuccess(HttpResponse<GraphQLResponse<Object>> response) {
                if (response.body().hasErrors()) {
                    mView.showErrorToast(response.body().getErrorMessage());
                    return;
                }
                for (int i = 0; i < items.size(); i++) {
                    if (commentId.equals(items.get(i).getId())) {
                        items.remove(i);
                        break;
                    }
                }
                mView.showItems(items);
            }
        };
        generalRxHttpExecute(new IObservableCreator<GraphQLResponse<Object>>() {
            @Override
            public Observable<Response<GraphQLResponse<Object>>> createObservable(boolean forceNetWork) {
                return getGraphQLService().deleteDiscussionComment(request);
            }
        }, httpObserver);
    }

    @Override
    public boolean isEditAndDeleteEnable(DiscussionComment comment) {
        return !comment.isHeader() && comment.getAuthor() != null &&
                AppData.INSTANCE.getLoggedUser().getLogin().equals(comment.getAuthor().getLogin());
    }

    @Override
    public void toggleUpvote(final DiscussionComment item) {
        final boolean originalUpvoted = item.isViewerHasUpvoted();
        final int originalCount = item.getUpvoteCount();
        boolean newUpvoted = !originalUpvoted;
        item.setViewerHasUpvoted(newUpvoted);
        item.setUpvoteCount(originalCount + (newUpvoted ? 1 : -1));
        mView.showItems(items);

        Map<String, Object> variables = new HashMap<>();
        variables.put("subjectId", item.getId());
        GraphQLRequest request = new GraphQLRequest(newUpvoted ? ADD_UPVOTE_MUTATION : REMOVE_UPVOTE_MUTATION, variables);

        HttpObserver<GraphQLResponse<Object>> httpObserver = new HttpObserver<GraphQLResponse<Object>>() {
            @Override
            public void onError(Throwable error) {
                item.setViewerHasUpvoted(originalUpvoted);
                item.setUpvoteCount(originalCount);
                mView.showItems(items);
                mView.showErrorToast(getErrorTip(error));
            }

            @Override
            public void onSuccess(HttpResponse<GraphQLResponse<Object>> response) {
                if (response.body().hasErrors()) {
                    item.setViewerHasUpvoted(originalUpvoted);
                    item.setUpvoteCount(originalCount);
                    mView.showItems(items);
                    mView.showErrorToast(response.body().getErrorMessage());
                }
            }
        };
        generalRxHttpExecute(new IObservableCreator<GraphQLResponse<Object>>() {
            @Override
            public Observable<Response<GraphQLResponse<Object>>> createObservable(boolean forceNetWork) {
                return newUpvoted ? getGraphQLService().addUpvote(request) : getGraphQLService().removeUpvote(request);
            }
        }, httpObserver);
    }

}
