package com.thirtydegreesray.openhub.mvp.model.graphql;

/**
 * GraphQL's "actor" shape (login/avatarUrl in camelCase) - kept separate
 * from the REST mvp.model.User, which expects snake_case JSON
 * (@SerializedName("avatar_url")) and would silently fail to populate its
 * avatar field against a GraphQL response.
 */
public class DiscussionUser {

    private String login;
    private String avatarUrl;

    public String getLogin() {
        return login;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

}
