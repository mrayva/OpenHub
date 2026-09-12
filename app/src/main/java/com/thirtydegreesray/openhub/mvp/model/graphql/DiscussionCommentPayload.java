package com.thirtydegreesray.openhub.mvp.model.graphql;

/**
 * Shared payload shape for addDiscussionComment/updateDiscussionComment
 * mutations - both return {"comment": {...}}.
 */
public class DiscussionCommentPayload {

    private DiscussionComment comment;

    public DiscussionComment getComment() {
        return comment;
    }

}
