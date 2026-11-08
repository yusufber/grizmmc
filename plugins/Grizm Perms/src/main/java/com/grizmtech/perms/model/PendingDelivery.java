package com.grizmtech.perms.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class PendingDelivery {
    private String id;

    @SerializedName("role_id")
    private String roleId;

    @SerializedName("role_name")
    private String roleName;

    @SerializedName("duration_days")
    private int durationDays;

    @SerializedName("created_at")
    private long createdAt;

    private boolean celebrated;

    @SerializedName("required_servers")
    private List<String> requiredServers = new ArrayList<>();

    @SerializedName("executed_servers")
    private List<String> executedServers = new ArrayList<>();

    public String getId() {
        return id == null ? "" : id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRoleId() {
        return roleId == null ? "" : roleId.toLowerCase();
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public String getRoleName() {
        return roleName == null ? getRoleId() : roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public int getDurationDays() {
        return durationDays;
    }

    public void setDurationDays(int durationDays) {
        this.durationDays = durationDays;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isCelebrated() {
        return celebrated;
    }

    public void setCelebrated(boolean celebrated) {
        this.celebrated = celebrated;
    }

    public List<String> getRequiredServers() {
        if (requiredServers == null) requiredServers = new ArrayList<>();
        return requiredServers;
    }

    public List<String> getExecutedServers() {
        if (executedServers == null) executedServers = new ArrayList<>();
        return executedServers;
    }
}

