package com.thirtydegreesray.openhub.mvp.presenter;

import com.thirtydegreesray.dataautoaccess.annotation.AutoAccess;
import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.dao.DaoSession;
import com.thirtydegreesray.openhub.http.core.HttpObserver;
import com.thirtydegreesray.openhub.http.core.HttpResponse;
import com.thirtydegreesray.openhub.mvp.contract.ICreateDiscussionContract;
import com.thirtydegreesray.openhub.mvp.model.graphql.AddDiscussionData;
import com.thirtydegreesray.openhub.mvp.model.graphql.DiscussionCategory;
import com.thirtydegreesray.openhub.mvp.model.graphql.GraphQLResponse;
import com.thirtydegreesray.openhub.mvp.model.graphql.RepositoryIdAndCategoriesData;
import com.thirtydegreesray.openhub.mvp.model.graphql.RepositoryWithCategories;
import com.thirtydegreesray.openhub.mvp.model.request.GraphQLRequest;
import com.thirtydegreesray.openhub.mvp.presenter.base.BasePresenter;
import com.thirtydegreesray.openhub.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import retrofit2.Response;
import rx.Observable;

/**
 * Fetches the repository's node id and its discussion categories together
 * (one round trip) - the id is what addDiscussion's repositoryId argument
 * needs, and this app has no other reason to know a repo's GraphQL node id
 * (the REST-fetched Repository model this app otherwise uses doesn't carry
 * one).
 */
public class CreateDiscussionPresenter extends BasePresenter<ICreateDiscussionContract.View>
        implements ICreateDiscussionContract.Presenter {

    private static final String REPO_AND_CATEGORIES_QUERY = """
            query($owner:String!, $repo:String!) {
              repository(owner:$owner, name:$repo) {
                id
                discussionCategories(first: 25) {
                  nodes { id name emoji }
                }
              }
            }
            """;

    private static final String ADD_DISCUSSION_MUTATION = """
            mutation($repositoryId:ID!, $categoryId:ID!, $title:String!, $body:String!) {
              addDiscussion(input: {repositoryId:$repositoryId, categoryId:$categoryId, title:$title, body:$body}) {
                discussion { number }
              }
            }
            """;

    @AutoAccess String owner;
    @AutoAccess String repo;

    private String repositoryId;

    @Inject
    public CreateDiscussionPresenter(DaoSession daoSession) {
        super(daoSession);
    }

    @Override
    public void onViewInitialized() {
        super.onViewInitialized();
        loadCategories();
    }

    private void loadCategories() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("owner", owner);
        variables.put("repo", repo);
        GraphQLRequest request = new GraphQLRequest(REPO_AND_CATEGORIES_QUERY, variables);

        HttpObserver<GraphQLResponse<RepositoryIdAndCategoriesData>> httpObserver =
                new HttpObserver<GraphQLResponse<RepositoryIdAndCategoriesData>>() {
            @Override
            public void onError(Throwable error) {
                mView.showErrorToast(getErrorTip(error));
            }

            @Override
            public void onSuccess(HttpResponse<GraphQLResponse<RepositoryIdAndCategoriesData>> response) {
                GraphQLResponse<RepositoryIdAndCategoriesData> body = response.body();
                RepositoryWithCategories repository = (body.hasErrors() || body.getData() == null) ?
                        null : body.getData().getRepository();
                if (repository == null) {
                    mView.showErrorToast(body.hasErrors() ? body.getErrorMessage() : getString(R.string.no_data));
                    return;
                }
                repositoryId = repository.getId();
                if (repository.getDiscussionCategories() != null) {
                    mView.showCategories(repository.getDiscussionCategories().getNodes());
                }
            }
        };
        generalRxHttpExecute(new IObservableCreator<GraphQLResponse<RepositoryIdAndCategoriesData>>() {
            @Override
            public Observable<Response<GraphQLResponse<RepositoryIdAndCategoriesData>>> createObservable(boolean forceNetWork) {
                return getGraphQLService().getRepositoryIdAndCategories(request);
            }
        }, httpObserver);
    }

    @Override
    public void submit(DiscussionCategory category, final String title, final String body) {
        if (StringUtils.isBlank(title) || StringUtils.isBlank(body)) {
            mView.showWarningToast(getString(R.string.comment_null_warning));
            return;
        }
        if (category == null || StringUtils.isBlank(repositoryId)) {
            mView.showWarningToast(getString(R.string.no_data));
            return;
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("repositoryId", repositoryId);
        variables.put("categoryId", category.getId());
        variables.put("title", title);
        variables.put("body", body);
        GraphQLRequest request = new GraphQLRequest(ADD_DISCUSSION_MUTATION, variables);

        HttpObserver<GraphQLResponse<AddDiscussionData>> httpObserver =
                new HttpObserver<GraphQLResponse<AddDiscussionData>>() {
            @Override
            public void onError(Throwable error) {
                mView.showErrorToast(getErrorTip(error));
            }

            @Override
            public void onSuccess(HttpResponse<GraphQLResponse<AddDiscussionData>> response) {
                GraphQLResponse<AddDiscussionData> result = response.body();
                boolean failed = result.hasErrors() || result.getData() == null
                        || result.getData().getAddDiscussion() == null
                        || result.getData().getAddDiscussion().getDiscussion() == null;
                if (failed) {
                    mView.showErrorToast(result.hasErrors() ? result.getErrorMessage() : getString(R.string.no_data));
                    return;
                }
                mView.onDiscussionCreated(result.getData().getAddDiscussion().getDiscussion().getNumber());
            }
        };
        generalRxHttpExecute(new IObservableCreator<GraphQLResponse<AddDiscussionData>>() {
            @Override
            public Observable<Response<GraphQLResponse<AddDiscussionData>>> createObservable(boolean forceNetWork) {
                return getGraphQLService().addDiscussion(request);
            }
        }, httpObserver, false, mView.getProgressDialog(getLoadTip()));
    }

}
