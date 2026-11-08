package com.grizmtech.perms.model;

import com.google.gson.annotations.SerializedName;

public class RoleAssignment {
    @SerializedName("role_id")
    private String roleId;

    @SerializedName("assigned_at")
    private long assignedAt;

    @SerializedName("expires_at")
    private long expiresAt;

    private String source = "system";

    public RoleAssignment() {
    }

    public RoleAssignment(String roleId, long assignedAt, long expiresAt, String source) {
        this.roleId = roleId != null ? roleId.toLowerCase() : "";
        this.assignedAt = assignedAt;
        this.expiresAt = expiresAt;
        this.source = source != null ? source : "system";
    }

    public String getRoleId() {
        return roleId == null ? "" : roleId.toLowerCase();
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId != null ? roleId.toLowerCase() : "";
    }

    public long getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(long assignedAt) {
        this.assignedAt = assignedAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getSource() {
        return source == null ? "system" : source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public boolean isExpired(long nowEpochSeconds) {
        return expiresAt > 0 && expiresAt <= nowEpochSeconds;
    }
}

