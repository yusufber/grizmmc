package com.grizmtech.auth.api;

import com.grizmtech.auth.model.UserProfileData;

public class AuthVerifyResult {
    private final boolean success;
    private final boolean authenticated;
    private final boolean launcherVerified;
    private final boolean registeredOnWeb;
    private final String message;
    private final UserProfileData profile;

    public AuthVerifyResult(boolean success, boolean authenticated, boolean launcherVerified, boolean registeredOnWeb, String message, UserProfileData profile) {
        this.success = success;
        this.authenticated = authenticated;
        this.launcherVerified = launcherVerified;
        this.registeredOnWeb = registeredOnWeb;
        this.message = message;
        this.profile = profile;
    }

    public static AuthVerifyResult failed(String message, boolean registeredOnWeb) {
        return new AuthVerifyResult(false, false, false, registeredOnWeb, message, null);
    }

    public static AuthVerifyResult success(boolean launcherVerified, UserProfileData profile) {
        return new AuthVerifyResult(true, true, launcherVerified, true, "Success", profile);
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public boolean isLauncherVerified() {
        return launcherVerified;
    }

    public boolean isRegisteredOnWeb() {
        return registeredOnWeb;
    }

    public String getMessage() {
        return message;
    }

    public UserProfileData getProfile() {
        return profile;
    }
}
