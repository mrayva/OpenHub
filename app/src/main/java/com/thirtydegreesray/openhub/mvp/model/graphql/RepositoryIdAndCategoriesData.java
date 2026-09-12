package com.thirtydegreesray.openhub.mvp.model.graphql;

/**
 * Top-level "data" shape for CreateDiscussionPresenter's combined
 * repositoryId+categories query.
 */
public class RepositoryIdAndCategoriesData {

    private RepositoryWithCategories repository;

    public RepositoryWithCategories getRepository() {
        return repository;
    }

}
