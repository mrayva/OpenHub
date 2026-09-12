package com.thirtydegreesray.openhub.http;

import androidx.annotation.NonNull;

import com.thirtydegreesray.openhub.mvp.model.graphql.DiscussionQueryData;
import com.thirtydegreesray.openhub.mvp.model.graphql.DiscussionsQueryData;
import com.thirtydegreesray.openhub.mvp.model.graphql.GraphQLResponse;
import com.thirtydegreesray.openhub.mvp.model.request.GraphQLRequest;

import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.POST;
import rx.Observable;

/**
 * GitHub Discussions have no REST API - this is the app's first (and so far
 * only) GraphQL client, one method per named query rather than one endpoint
 * per resource. See GraphQLResponse for why every caller must check
 * hasErrors() - a query failure comes back as HTTP 200 with an "errors" body,
 * not as an HTTP error status.
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

}
