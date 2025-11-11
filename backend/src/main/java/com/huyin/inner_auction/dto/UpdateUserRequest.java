package com.huyin.inner_auction.dto;

public class UpdateUserRequest {
    // All fields optional; only non-null fields will be updated.
    private String displayName;
    private String bio;
    private String avatarUrl;

    public UpdateUserRequest() {}

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}