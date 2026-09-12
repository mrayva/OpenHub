package com.thirtydegreesray.openhub.mvp.model.graphql;

/**
 * Top-level "data" shape for the discussions-list GraphQL query.
 */
public class DiscussionsQueryData {

    private RepositoryDiscussions repository;

    public RepositoryDiscussions getRepository() {
        return repository;
    }

}
