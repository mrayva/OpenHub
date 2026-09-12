

package com.thirtydegreesray.openhub.http;

import androidx.annotation.NonNull;

import com.thirtydegreesray.openhub.mvp.model.Gist;
import com.thirtydegreesray.openhub.mvp.model.GistComment;

import java.util.ArrayList;

import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;
import retrofit2.http.Query;
import rx.Observable;

/**
 * Created for the Gists feature (viewing phase - see plan for later phases:
 * star/fork/comment-write/create/edit/delete).
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

}
