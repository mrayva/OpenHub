package com.thirtydegreesray.openhub.mvp.model.graphql;

/**
 * Top-level "data" shape for the single-discussion GraphQL query.
 */
public class DiscussionQueryData {

    private RepositoryDiscussion repository;

    public RepositoryDiscussion getRepository() {
        return repository;
    }

}
