package com.thirtydegreesray.openhub.mvp.model.graphql;

import java.util.Date;

public class Discussion {

    private String id;
    private int number;
    private String title;
    private String bodyHTML;
    private Date createdAt;
    private Date updatedAt;
    private int upvoteCount;
    private boolean isAnswered;
    private boolean viewerHasUpvoted;
    private String url;
    private DiscussionUser author;
    private DiscussionCategory category;
    private DiscussionCommentConnection comments;

    public String getId() {
        return id;
    }

    public int getNumber() {
        return number;
    }

    public String getTitle() {
        return title;
    }

    public String getBodyHTML() {
        return bodyHTML;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public int getUpvoteCount() {
        return upvoteCount;
    }

    public boolean isAnswered() {
        return isAnswered;
    }

    public boolean isViewerHasUpvoted() {
        return viewerHasUpvoted;
    }

    public String getUrl() {
        return url;
    }

    public DiscussionUser getAuthor() {
        return author;
    }

    public DiscussionCategory getCategory() {
        return category;
    }

    public DiscussionCommentConnection getComments() {
        return comments;
    }

    public int getCommentsCount() {
        return comments == null ? 0 : comments.getTotalCount();
    }

}
