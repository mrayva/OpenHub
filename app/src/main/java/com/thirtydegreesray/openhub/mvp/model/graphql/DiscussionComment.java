package com.thirtydegreesray.openhub.mvp.model.graphql;

import java.util.Date;

/**
 * Also reused as the flattened list's synthesized row 0 (the discussion's
 * own body, shown as if it were "the first comment") and to mark a reply -
 * discussionTitle/discussionCategory/reply only apply to those two synthetic
 * cases and are otherwise left null/false, mirroring IssueEvent's identical
 * reuse-for-the-first-item trick in IssueTimelinePresenter.getFirstComment().
 */
public class DiscussionComment {

    private String id;
    private String bodyHTML;
    private Date createdAt;
    private int upvoteCount;
    private boolean isAnswer;
    private DiscussionUser author;
    private DiscussionCommentConnection replies;

    private transient String discussionTitle;
    private transient DiscussionCategory discussionCategory;
    private transient boolean reply;

    public String getId() {
        return id;
    }

    public String getBodyHTML() {
        return bodyHTML;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public int getUpvoteCount() {
        return upvoteCount;
    }

    public boolean isAnswer() {
        return isAnswer;
    }

    public DiscussionUser getAuthor() {
        return author;
    }

    public DiscussionCommentConnection getReplies() {
        return replies;
    }

    public String getDiscussionTitle() {
        return discussionTitle;
    }

    public void setDiscussionTitle(String discussionTitle) {
        this.discussionTitle = discussionTitle;
    }

    public DiscussionCategory getDiscussionCategory() {
        return discussionCategory;
    }

    public void setDiscussionCategory(DiscussionCategory discussionCategory) {
        this.discussionCategory = discussionCategory;
    }

    public boolean isReply() {
        return reply;
    }

    public void setReply(boolean reply) {
        this.reply = reply;
    }

    public boolean isHeader() {
        return discussionTitle != null;
    }

    public static DiscussionComment forHeader(Discussion discussion) {
        DiscussionComment header = new DiscussionComment();
        header.discussionTitle = discussion.getTitle();
        header.discussionCategory = discussion.getCategory();
        header.bodyHTML = discussion.getBodyHTML();
        header.createdAt = discussion.getCreatedAt();
        header.author = discussion.getAuthor();
        header.upvoteCount = discussion.getUpvoteCount();
        return header;
    }

}
