

package com.thirtydegreesray.openhub.http;

import androidx.annotation.NonNull;

import com.thirtydegreesray.openhub.mvp.model.Gist;
import com.thirtydegreesray.openhub.mvp.model.GistComment;
import com.thirtydegreesray.openhub.mvp.model.request.CommentRequestModel;
import com.thirtydegreesray.openhub.mvp.model.request.CreateGistModel;

import java.util.ArrayList;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import rx.Observable;

/**
 * Created for the Gists feature (viewing phase, then extended for
 * interaction/create/edit/delete - see plan).
 */

public interface GistService {

    @NonNull @GET("gists")
    Observable<Response<ArrayList<Gist>>> getMyGists(
            @Header("forceNetWork") boolean forceNetWork,
            @Query("page") int page
    );

    @NonNull @GET("gists/starred")
    Observable<Response<ArrayList<Gist>>> getStarredGists(
            @Header("forceNetWork") boolean forceNetWork,
            @Query("page") int page
    );

    @NonNull @GET("gists/public")
    Observable<Response<ArrayList<Gist>>> getPublicGists(
            @Header("forceNetWork") boolean forceNetWork,
            @Query("page") int page
    );

    @NonNull @GET("users/{user}/gists")
    Observable<Response<ArrayList<Gist>>> getUserGists(
            @Header("forceNetWork") boolean forceNetWork,
            @Path("user") String user,
            @Query("page") int page
    );

    @NonNull @GET("gists/{gistId}")
    Observable<Response<Gist>> getGistInfo(
            @Header("forceNetWork") boolean forceNetWork,
            @Path("gistId") String gistId
    );

    @NonNull @GET("gists/{gistId}/comments")
    Observable<Response<ArrayList<GistComment>>> getGistComments(
            @Header("forceNetWork") boolean forceNetWork,
            @Path("gistId") String gistId,
            @Query("page") int page
    );

    /**
     * Check if you are starring a gist
     */
    @NonNull @GET("gists/{gistId}/star")
    Observable<Response<ResponseBody>> checkGistStarred(
            @Path("gistId") String gistId
    );

    @NonNull @PUT("gists/{gistId}/star")
    Observable<Response<ResponseBody>> starGist(
            @Path("gistId") String gistId
    );

    @NonNull @DELETE("gists/{gistId}/star")
    Observable<Response<ResponseBody>> unstarGist(
            @Path("gistId") String gistId
    );

    @NonNull @POST("gists/{gistId}/forks")
    Observable<Response<Gist>> forkGist(
            @Path("gistId") String gistId
    );

    @NonNull @POST("gists/{gistId}/comments")
    Observable<Response<GistComment>> createGistComment(
            @Path("gistId") String gistId,
            @Body CommentRequestModel comment
    );

    @NonNull @PATCH("gists/{gistId}/comments/{commentId}")
    Observable<Response<GistComment>> editGistComment(
            @Path("gistId") String gistId,
            @Path("commentId") String commentId,
            @Body CommentRequestModel comment
    );

    @NonNull @DELETE("gists/{gistId}/comments/{commentId}")
    Observable<Response<ResponseBody>> deleteGistComment(
            @Path("gistId") String gistId,
            @Path("commentId") String commentId
    );

    @NonNull @POST("gists")
    Observable<Response<Gist>> createGist(
            @Body CreateGistModel gist
    );

    /**
     * Uses a raw RequestBody (rather than CreateGistModel directly) because
     * removing a file requires sending an explicit JSON null for it, which
     * needs a Gson instance built with serializeNulls() - the app's shared
     * Retrofit Gson converter doesn't serialize nulls, so the caller builds
     * this body itself with its own Gson instance.
     */
    @NonNull @PATCH("gists/{gistId}")
    Observable<Response<Gist>> editGist(
            @Path("gistId") String gistId,
            @Body RequestBody gist
    );

    @NonNull @DELETE("gists/{gistId}")
    Observable<Response<ResponseBody>> deleteGist(
            @Path("gistId") String gistId
    );

}
