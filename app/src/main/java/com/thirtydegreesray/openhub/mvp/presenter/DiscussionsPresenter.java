package com.thirtydegreesray.openhub.mvp.presenter;

import com.thirtydegreesray.dataautoaccess.annotation.AutoAccess;
import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.dao.DaoSession;
import com.thirtydegreesray.openhub.http.core.HttpObserver;
import com.thirtydegreesray.openhub.http.core.HttpResponse;
import com.thirtydegreesray.openhub.mvp.contract.IDiscussionsContract;
import com.thirtydegreesray.openhub.mvp.model.graphql.Discussion;
import com.thirtydegreesray.openhub.mvp.model.graphql.DiscussionCategory;
import com.thirtydegreesray.openhub.mvp.model.graphql.DiscussionConnection;
import com.thirtydegreesray.openhub.mvp.model.graphql.DiscussionsQueryData;
import com.thirtydegreesray.openhub.mvp.model.graphql.GraphQLResponse;
import com.thirtydegreesray.openhub.mvp.model.graphql.RepositoryDiscussions;
import com.thirtydegreesray.openhub.mvp.model.request.GraphQLRequest;
import com.thirtydegreesray.openhub.mvp.presenter.base.BasePresenter;
import com.thirtydegreesray.openhub.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import retrofit2.Response;
import rx.Observable;

public class DiscussionsPresenter extends BasePresenter<IDiscussionsContract.View>
        implements IDiscussionsContract.Presenter {

    // Discussions/categories only exist on GitHub's GraphQL API, not REST -
    // see GraphQLResponse for why every onSuccess() here has to check
    // hasErrors() itself rather than relying on generalRxHttpExecute's
    // HTTP-status-based error mapping.
    private static final String DISCUSSIONS_QUERY = """
            query($owner:String!, $repo:String!, $after:String, $categoryId:ID) {
              repository(owner:$owner, name:$repo) {
                discussionCategories(first: 25) {
                  nodes { id name emoji }
                }
                discussions(first: 20, after: $after, categoryId: $categoryId, orderBy: {field: UPDATED_AT, direction: DESC}) {
                  pageInfo { hasNextPage endCursor }
                  nodes {
                    id
                    number
                    title
                    createdAt
                    updatedAt
                    upvoteCount
                    viewerHasUpvoted
                    isAnswered
                    author { login avatarUrl }
                    category { id name emoji }
                    comments { totalCount }
                  }
                }
              }
            }
            """;

    @AutoAccess String owner;
    @AutoAccess String repo;

    private ArrayList<Discussion> discussions;
    private ArrayList<DiscussionCategory> categories;
    private DiscussionCategory selectedCategory;
    private String endCursor;

    @Inject
    public DiscussionsPresenter(DaoSession daoSession) {
        super(daoSession);
    }

    @Override
    public void onViewInitialized() {
        super.onViewInitialized();
        loadDiscussions(1, false);
    }

    @Override
    public void loadDiscussions(final int page, final boolean isReload) {
        if (page == 1) {
            endCursor = null;
        }
        mView.showLoading();

        Map<String, Object> variables = new HashMap<>();
        variables.put("owner", owner);
        variables.put("repo", repo);
        variables.put("after", endCursor);
        if (selectedCategory != null) {
            variables.put("categoryId", selectedCategory.getId());
        }
        GraphQLRequest request = new GraphQLRequest(DISCUSSIONS_QUERY, variables);

        HttpObserver<GraphQLResponse<DiscussionsQueryData>> httpObserver =
                new HttpObserver<GraphQLResponse<DiscussionsQueryData>>() {
            @Override
            public void onError(Throwable error) {
                mView.hideLoading();
                if (!StringUtils.isBlankList(discussions)) {
                    mView.showErrorToast(getErrorTip(error));
                } else {
                    mView.showLoadError(getErrorTip(error));
                }
            }

            @Override
            public void onSuccess(HttpResponse<GraphQLResponse<DiscussionsQueryData>> response) {
                mView.hideLoading();
                GraphQLResponse<DiscussionsQueryData> body = response.body();
                if (body.hasErrors()) {
                    if (!StringUtils.isBlankList(discussions)) {
                        mView.showErrorToast(body.getErrorMessage());
                    } else {
                        mView.showLoadError(body.getErrorMessage());
                    }
                    return;
                }
                // GitHub's edge/gateway can reject a request (rate limiting,
                // an org's OAuth app restrictions, etc.) with a plain REST-
                // style error body that has neither "data" nor "errors" -
                // hasErrors() alone won't catch that, so guard here too
                // rather than let a null "data" NPE with a blank message.
                RepositoryDiscussions repository = body.getData() == null ? null : body.getData().getRepository();
                if (repository == null) {
                    mView.showLoadError(getString(R.string.no_data));
                    return;
                }

                if (categories == null && repository.getDiscussionCategories() != null) {
                    categories = repository.getDiscussionCategories().getNodes();
                    mView.showCategories(categories);
                }

                DiscussionConnection connection = repository.getDiscussions();
                ArrayList<Discussion> newDiscussions = connection.getNodes() == null ?
                        new ArrayList<>() : connection.getNodes();
                if (page == 1 || isReload || discussions == null) {
                    discussions = newDiscussions;
                } else {
                    discussions.addAll(newDiscussions);
                }
                endCursor = connection.getPageInfo().getEndCursor();
                mView.setCanLoadMore(connection.getPageInfo().isHasNextPage());
                mView.showDiscussions(discussions);
            }
        };

        generalRxHttpExecute(new IObservableCreator<GraphQLResponse<DiscussionsQueryData>>() {
            @Override
            public Observable<Response<GraphQLResponse<DiscussionsQueryData>>> createObservable(boolean forceNetWork) {
                return getGraphQLService().getDiscussions(request);
            }
        }, httpObserver);
    }

    @Override
    public void filterByCategory(DiscussionCategory category) {
        selectedCategory = category;
        loadDiscussions(1, true);
    }

    @Override
    public ArrayList<DiscussionCategory> getCategories() {
        return categories;
    }

    public String getOwner() {
        return owner;
    }

    public String getRepo() {
        return repo;
    }

}
