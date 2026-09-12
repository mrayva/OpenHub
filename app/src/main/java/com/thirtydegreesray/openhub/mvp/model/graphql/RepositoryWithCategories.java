package com.thirtydegreesray.openhub.mvp.model.graphql;

/**
 * "data.repository" shape for CreateDiscussionPresenter's combined
 * repositoryId+categories query.
 */
public class RepositoryWithCategories {

    private String id;
    private DiscussionCategoryConnection discussionCategories;

    public String getId() {
        return id;
    }

    public DiscussionCategoryConnection getDiscussionCategories() {
        return discussionCategories;
    }

}
