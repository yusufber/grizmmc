package com.grizmtech.perms.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserData {
    private String username = "";
    private String email = "";
    private double credits = 0.0;
    private List<RoleAssignment> roles = new ArrayList<>();
    private List<String> permissions = new ArrayList<>();

    @SerializedName("server_permissions")
    private Map<String, List<String>> serverPermissions = new HashMap<>();

    @SerializedName("pending_deliveries")
    private List<PendingDelivery> pendingDeliveries = new ArrayList<>();

    @SerializedName("updated_at")
    private long updatedAt;

    public UserData() {
    }

    public UserData(String username) {
        this.username = username;
        this.updatedAt = System.currentTimeMillis() / 1000L;
    }

    public String getUsername() {
        return username == null ? "" : username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email == null ? "" : email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public double getCredits() {
        return credits;
    }

    public void setCredits(double credits) {
        this.credits = credits;
    }

    public List<RoleAssignment> getRoles() {
        if (roles == null) roles = new ArrayList<>();
        return roles;
    }

    public List<String> getPermissions() {
        if (permissions == null) permissions = new ArrayList<>();
        return permissions;
    }

    public Map<String, List<String>> getServerPermissions() {
        if (serverPermissions == null) serverPermissions = new HashMap<>();
        return serverPermissions;
    }

    public List<PendingDelivery> getPendingDeliveries() {
        if (pendingDeliveries == null) pendingDeliveries = new ArrayList<>();
        return pendingDeliveries;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}

