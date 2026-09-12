package com.thirtydegreesray.openhub.mvp.model.graphql;

/**
 * Top-level "data" shape for the updateDiscussionComment mutation.
 */
public class UpdateDiscussionCommentData {

    private DiscussionCommentPayload updateDiscussionComment;

    public DiscussionCommentPayload getUpdateDiscussionComment() {
        return updateDiscussionComment;
    }

}
