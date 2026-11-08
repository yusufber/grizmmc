package com.grizmtech.perms.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Role {
    private String id = "default";
    private String name = "Oyuncu";
    private int weight = 10;
    private String prefix = "";
    private String suffix = "";

    @SerializedName("chat_color")
    private String chatColor = "&7";

    @SerializedName("badge_color")
    private String badgeColor = "#71717a";

    @SerializedName("is_default")
    private boolean isDefault = false;

    @SerializedName("is_vip")
    private boolean isVip = false;

    @SerializedName("vip_tier")
    private String vipTier = "";

    @SerializedName("vip_price")
    private double vipPrice = 0.0;

    @SerializedName("vip_duration_days")
    private int vipDurationDays = 30;

    @SerializedName("vip_badge")
    private String vipBadge = "";

    @SerializedName("vip_highlight")
    private boolean vipHighlight = false;

    @SerializedName("vip_perks")
    private List<String> vipPerks = new ArrayList<>();

    @SerializedName("vip_reward_commands")
    private List<RewardCommand> vipRewardCommands = new ArrayList<>();

    private List<String> inheritance = new ArrayList<>();
    private List<String> permissions = new ArrayList<>();

    @SerializedName("server_permissions")
    private Map<String, List<String>> serverPermissions = new HashMap<>();

    public String getId() {
        return id == null ? "default" : id.toLowerCase();
    }

    public void setId(String id) {
        this.id = id != null ? id.toLowerCase() : "default";
    }

    public String getName() {
        return name == null ? getId() : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public String getPrefix() {
        return prefix == null ? "" : prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getSuffix() {
        return suffix == null ? "" : suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public String getChatColor() {
        return chatColor == null || chatColor.isEmpty() ? "&f" : chatColor;
    }

    public void setChatColor(String chatColor) {
        this.chatColor = chatColor;
    }

    public String getBadgeColor() {
        return badgeColor == null ? "#ffffff" : badgeColor;
    }

    public void setBadgeColor(String badgeColor) {
        this.badgeColor = badgeColor;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public boolean isVip() {
        return isVip;
    }

    public void setVip(boolean vip) {
        isVip = vip;
    }

    public double getVipPrice() {
        return vipPrice;
    }

    public void setVipPrice(double vipPrice) {
        this.vipPrice = vipPrice;
    }

    public int getVipDurationDays() {
        return vipDurationDays;
    }

    public void setVipDurationDays(int vipDurationDays) {
        this.vipDurationDays = vipDurationDays;
    }

    public List<RewardCommand> getVipRewardCommands() {
        if (vipRewardCommands == null) vipRewardCommands = new ArrayList<>();
        return vipRewardCommands;
    }

    public List<String> getInheritance() {
        if (inheritance == null) inheritance = new ArrayList<>();
        return inheritance;
    }

    public List<String> getPermissions() {
        if (permissions == null) permissions = new ArrayList<>();
        return permissions;
    }

    public Map<String, List<String>> getServerPermissions() {
        if (serverPermissions == null) serverPermissions = new HashMap<>();
        return serverPermissions;
    }
}

