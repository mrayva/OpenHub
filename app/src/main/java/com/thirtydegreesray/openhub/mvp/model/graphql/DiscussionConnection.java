package com.thirtydegreesray.openhub.mvp.model.graphql;

import java.util.ArrayList;

public class DiscussionConnection {

    private int totalCount;
    private PageInfo pageInfo;
    private ArrayList<Discussion> nodes;

    public int getTotalCount() {
        return totalCount;
    }

    public PageInfo getPageInfo() {
        return pageInfo;
    }

    public ArrayList<Discussion> getNodes() {
        return nodes;
    }

}
