package com.thirtydegreesray.openhub.mvp.model.graphql;

import java.util.ArrayList;

/**
 * Reused both for a discussion's top-level "comments" connection (which
 * queries totalCount/pageInfo/nodes) and for one comment's "replies"
 * connection (which only queries nodes, one level deep) - the unused fields
 * simply stay at their default/null when a particular query didn't ask for
 * them, which Gson handles without complaint.
 */
public class DiscussionCommentConnection {

    private int totalCount;
    private PageInfo pageInfo;
    private ArrayList<DiscussionComment> nodes;

    public int getTotalCount() {
        return totalCount;
    }

    public PageInfo getPageInfo() {
        return pageInfo;
    }

    public ArrayList<DiscussionComment> getNodes() {
        return nodes;
    }

}
