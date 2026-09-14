package com.thirtydegreesray.openhub.mvp.model;

import java.util.Date;

/**
 * One exported/imported Bookmarks entry - a plain, flat, Gson-friendly DTO,
 * not a GreenDAO entity. A live bookmark is really three rows (a Bookmark
 * join row keyed by type/userId/repoId, plus a LocalRepo or LocalUser row
 * for the actual display data - see BookmarkHelper's class comment), which
 * wouldn't survive a data wipe as three separately-exported tables without
 * risking dangling references. Denormalizing into one flat record per
 * bookmark (same approach as IgnoredRepo) makes each entry self-contained
 * and safely re-importable on its own.
 */
public class BookmarkBackup {

    private String type; // "repo" or "user"
    private Date markTime;

    // "repo" fields
    private Long repoId;
    private String repoName;
    private String repoDescription;
    private String repoLanguage;
    private Integer repoStargazersCount;
    private Integer repoWatchersCount;
    private Integer repoForksCount;
    private Boolean repoFork;
    private String repoOwnerLogin;
    private String repoOwnerAvatarUrl;

    // "user" fields
    private String userLogin;
    private String userName;
    private String userAvatarUrl;
    private Integer userFollowers;
    private Integer userFollowing;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Date getMarkTime() {
        return markTime;
    }

    public void setMarkTime(Date markTime) {
        this.markTime = markTime;
    }

    public Long getRepoId() {
        return repoId;
    }

    public void setRepoId(Long repoId) {
        this.repoId = repoId;
    }

    public String getRepoName() {
        return repoName;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }

    public String getRepoDescription() {
        return repoDescription;
    }

    public void setRepoDescription(String repoDescription) {
        this.repoDescription = repoDescription;
    }

    public String getRepoLanguage() {
        return repoLanguage;
    }

    public void setRepoLanguage(String repoLanguage) {
        this.repoLanguage = repoLanguage;
    }

    public Integer getRepoStargazersCount() {
        return repoStargazersCount;
    }

    public void setRepoStargazersCount(Integer repoStargazersCount) {
        this.repoStargazersCount = repoStargazersCount;
    }

    public Integer getRepoWatchersCount() {
        return repoWatchersCount;
    }

    public void setRepoWatchersCount(Integer repoWatchersCount) {
        this.repoWatchersCount = repoWatchersCount;
    }

    public Integer getRepoForksCount() {
        return repoForksCount;
    }

    public void setRepoForksCount(Integer repoForksCount) {
        this.repoForksCount = repoForksCount;
    }

    public Boolean getRepoFork() {
        return repoFork;
    }

    public void setRepoFork(Boolean repoFork) {
        this.repoFork = repoFork;
    }

    public String getRepoOwnerLogin() {
        return repoOwnerLogin;
    }

    public void setRepoOwnerLogin(String repoOwnerLogin) {
        this.repoOwnerLogin = repoOwnerLogin;
    }

    public String getRepoOwnerAvatarUrl() {
        return repoOwnerAvatarUrl;
    }

    public void setRepoOwnerAvatarUrl(String repoOwnerAvatarUrl) {
        this.repoOwnerAvatarUrl = repoOwnerAvatarUrl;
    }

    public String getUserLogin() {
        return userLogin;
    }

    public void setUserLogin(String userLogin) {
        this.userLogin = userLogin;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserAvatarUrl() {
        return userAvatarUrl;
    }

    public void setUserAvatarUrl(String userAvatarUrl) {
        this.userAvatarUrl = userAvatarUrl;
    }

    public Integer getUserFollowers() {
        return userFollowers;
    }

    public void setUserFollowers(Integer userFollowers) {
        this.userFollowers = userFollowers;
    }

    public Integer getUserFollowing() {
        return userFollowing;
    }

    public void setUserFollowing(Integer userFollowing) {
        this.userFollowing = userFollowing;
    }
}
