package com.huyin.inner_auction.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateAvatarRequest {
    @NotBlank
    private String avatarUrl;

    public UpdateAvatarRequest() {}

    public UpdateAvatarRequest(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}