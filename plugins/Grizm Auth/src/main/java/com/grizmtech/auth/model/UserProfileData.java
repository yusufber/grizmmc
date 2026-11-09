package com.grizmtech.auth.model;

public class UserProfileData {
    private final String username;
    private final String email;
    private final String fullName;
    private final String avatarUrl;
    private final String sessionId;
    private final long authenticatedAt;

    public UserProfileData(String username, String email, String fullName, String avatarUrl, String sessionId) {
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.avatarUrl = avatarUrl;
        this.sessionId = sessionId;
        this.authenticatedAt = System.currentTimeMillis();
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getSessionId() {
        return sessionId;
    }

    public long getAuthenticatedAt() {
        return authenticatedAt;
    }
}
