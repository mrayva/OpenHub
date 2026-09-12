package com.thirtydegreesray.openhub.mvp.model.graphql;

/**
 * Payload shape for the addDiscussion mutation - {"discussion": {"number": N}}.
 */
public class DiscussionPayload {

    private Discussion discussion;

    public Discussion getDiscussion() {
        return discussion;
    }

}
