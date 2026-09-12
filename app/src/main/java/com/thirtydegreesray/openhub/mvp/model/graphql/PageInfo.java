package com.thirtydegreesray.openhub.mvp.model.graphql;

/**
 * GraphQL Relay-style cursor pagination info, shared by every connection
 * (discussions, comments) this app queries.
 */
public class PageInfo {

    private boolean hasNextPage;
    private String endCursor;

    public boolean isHasNextPage() {
        return hasNextPage;
    }

    public String getEndCursor() {
        return endCursor;
    }

}
