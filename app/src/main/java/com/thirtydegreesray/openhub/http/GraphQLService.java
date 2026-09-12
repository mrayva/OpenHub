package com.thirtydegreesray.openhub.http;

import androidx.annotation.NonNull;

import com.thirtydegreesray.openhub.mvp.model.graphql.AddDiscussionCommentData;
import com.thirtydegreesray.openhub.mvp.model.graphql.AddDiscussionData;
import com.thirtydegreesray.openhub.mvp.model.graphql.DiscussionQueryData;
import com.thirtydegreesray.openhub.mvp.model.graphql.DiscussionsQueryData;
import com.thirtydegreesray.openhub.mvp.model.graphql.GraphQLResponse;
import com.thirtydegreesray.openhub.mvp.model.graphql.RepositoryIdAndCategoriesData;
import com.thirtydegreesray.openhub.mvp.model.graphql.UpdateDiscussionCommentData;
import com.thirtydegreesray.openhub.mvp.model.request.GraphQLRequest;

import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.POST;
import rx.Observable;

/**
 * GitHub Discussions have no REST API - this is the app's first (and so far
 * only) GraphQL client, one method per named query/mutation rather than one
 * endpoint per resource. See GraphQLResponse for why every caller must check
 * hasErrors() - a query/mutation failure comes back as HTTP 200 with an
 * "errors" body, not as an HTTP error status.
 */
public interface GraphQLService {

    @NonNull @POST("graphql")
    Observable<Response<GraphQLResponse<DiscussionsQueryData>>> getDiscussions(
            @Body GraphQLRequest request
    );

    @NonNull @POST("graphql")
    Observable<Response<GraphQLResponse<DiscussionQueryData>>> getDiscussion(
            @Body GraphQLRequest request
    );

    @NonNull @POST("graphql")
    Observable<Response<GraphQLResponse<RepositoryIdAndCategoriesData>>> getRepositoryIdAndCategories(
            @Body GraphQLRequest request
    );

    @NonNull @POST("graphql")
    Observable<Response<GraphQLResponse<AddDiscussionCommentData>>> addDiscussionComment(
            @Body GraphQLRequest request
    );

    @NonNull @POST("graphql")
    Observable<Response<GraphQLResponse<UpdateDiscussionCommentData>>> updateDiscussionComment(
            @Body GraphQLRequest request
    );

    /**
     * deleteDiscussionComment/addUpvote/removeUpvote only ever need
     * hasErrors() checked - the caller already knows the local state it
     * optimistically applied, so there's no reason to model their small
     * response payloads with a dedicated class.
     */
    @NonNull @POST("graphql")
    Observable<Response<GraphQLResponse<Object>>> deleteDiscussionComment(
            @Body GraphQLRequest request
    );

    @NonNull @POST("graphql")
    Observable<Response<GraphQLResponse<Object>>> addUpvote(
            @Body GraphQLRequest request
    );

    @NonNull @POST("graphql")
    Observable<Response<GraphQLResponse<Object>>> removeUpvote(
            @Body GraphQLRequest request
    );

    @NonNull @POST("graphql")
    Observable<Response<GraphQLResponse<AddDiscussionData>>> addDiscussion(
            @Body GraphQLRequest request
    );

}
