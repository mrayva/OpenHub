package com.thirtydegreesray.openhub.mvp.model.graphql;

/**
 * "data.repository" shape for the discussions-list query.
 */
public class RepositoryDiscussions {

    private DiscussionCategoryConnection discussionCategories;
    private DiscussionConnection discussions;

    public DiscussionCategoryConnection getDiscussionCategories() {
        return discussionCategories;
    }

    public DiscussionConnection getDiscussions() {
        return discussions;
    }

}
