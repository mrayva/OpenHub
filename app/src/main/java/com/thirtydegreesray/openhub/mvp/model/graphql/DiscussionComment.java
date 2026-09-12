package com.thirtydegreesray.openhub.mvp.model.graphql;

import java.util.Date;

/**
 * Also reused as the flattened list's synthesized row 0 (the discussion's
 * own body, shown as if it were "the first comment") and to mark a reply -
 * discussionTitle/discussionCategory/reply only apply to those two synthetic
 * cases and are otherwise left null/false, mirroring IssueEvent's identical
 * reuse-for-the-first-item trick in IssueTimelinePresenter.getFirstComment().
 *
 * id/upvoteCount/viewerHasUpvoted have setters (unlike the other read-only
 * fields here) because CreateDiscussionPresenter... no - DiscussionPresenter's
 * optimistic upvote toggle mutates them directly on whichever row (header or
 * a real comment) the user tapped, the same way RepositoryPresenter.starRepo()
 * flips its local "starred" field before the network call resolves.
 */
public class DiscussionComment {

    private String id;
    private String bodyHTML;
    private Date createdAt;
    private int upvoteCount;
    private boolean isAnswer;
    private boolean viewerHasUpvoted;
    private DiscussionUser author;
    private DiscussionCommentConnection replies;

    private transient String discussionTitle;
    private transient DiscussionCategory discussionCategory;
    private transient boolean reply;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBodyHTML() {
        return bodyHTML;
    }

    public void setBodyHTML(String bodyHTML) {
        this.bodyHTML = bodyHTML;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public int getUpvoteCount() {
        return upvoteCount;
    }

    public void setUpvoteCount(int upvoteCount) {
        this.upvoteCount = upvoteCount;
    }

    public boolean isAnswer() {
        return isAnswer;
    }

    public boolean isViewerHasUpvoted() {
        return viewerHasUpvoted;
    }

    public void setViewerHasUpvoted(boolean viewerHasUpvoted) {
        this.viewerHasUpvoted = viewerHasUpvoted;
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
        header.id = discussion.getId();
        header.discussionTitle = discussion.getTitle();
        header.discussionCategory = discussion.getCategory();
        header.bodyHTML = discussion.getBodyHTML();
        header.createdAt = discussion.getCreatedAt();
        header.author = discussion.getAuthor();
        header.upvoteCount = discussion.getUpvoteCount();
        header.viewerHasUpvoted = discussion.isViewerHasUpvoted();
        return header;
    }

}
