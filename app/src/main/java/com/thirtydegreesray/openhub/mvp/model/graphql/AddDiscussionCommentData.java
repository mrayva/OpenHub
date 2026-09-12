package com.thirtydegreesray.openhub.mvp.model.graphql;

/**
 * Top-level "data" shape for the addDiscussionComment mutation.
 */
public class AddDiscussionCommentData {

    private DiscussionCommentPayload addDiscussionComment;

    public DiscussionCommentPayload getAddDiscussionComment() {
        return addDiscussionComment;
    }

}
