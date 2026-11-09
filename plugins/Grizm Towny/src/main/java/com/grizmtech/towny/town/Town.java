package com.grizmtech.towny.town;

import org.bukkit.Location;

import java.util.UUID;

public class Town {

    private final UUID uuid;
    private String name;
    private UUID leaderUuid;
    private UUID nationUuid;
    private double bankBalance;
    private int level;
    private Location homeLocation;
    private double spawnCost;
    private long upkeepWarningTime;
    private final long createdAt;

    public Town(UUID uuid, String name, UUID leaderUuid, UUID nationUuid, double bankBalance, int level,
                Location homeLocation, double spawnCost, long upkeepWarningTime, long createdAt) {
        this.uuid = uuid;
        this.name = name;
        this.leaderUuid = leaderUuid;
        this.nationUuid = nationUuid;
        this.bankBalance = bankBalance;
        this.level = Math.max(1, level);
        this.homeLocation = homeLocation;
        this.spawnCost = Math.max(0.0, spawnCost);
        this.upkeepWarningTime = upkeepWarningTime;
        this.createdAt = createdAt;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getLeaderUuid() {
        return leaderUuid;
    }

    public void setLeaderUuid(UUID leaderUuid) {
        this.leaderUuid = leaderUuid;
    }

    public UUID getNationUuid() {
        return nationUuid;
    }

    public void setNationUuid(UUID nationUuid) {
        this.nationUuid = nationUuid;
    }

    public double getBankBalance() {
        return bankBalance;
    }

    public void setBankBalance(double bankBalance) {
        this.bankBalance = Math.max(0.0, bankBalance);
    }

    public void deposit(double amount) {
        if (Double.isFinite(amount) && amount > 0) {
            this.bankBalance += amount;
            if (this.upkeepWarningTime > 0 && this.bankBalance > 0) {
                this.upkeepWarningTime = 0; // Hazineye para girdiyse harabe korumasını kaldır
            }
        }
    }

    public boolean withdraw(double amount) {
        if (Double.isFinite(amount) && amount > 0 && this.bankBalance >= amount) {
            this.bankBalance -= amount;
            return true;
        }
        return false;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.max(1, level);
    }

    public Location getHomeLocation() {
        return homeLocation;
    }

    public void setHomeLocation(Location homeLocation) {
        this.homeLocation = homeLocation;
    }

    public double getSpawnCost() {
        return spawnCost;
    }

    public void setSpawnCost(double cost) {
        if (!Double.isFinite(cost) || cost < 0) return;
        this.spawnCost = cost;
    }

    public long getUpkeepWarningTime() {
        return upkeepWarningTime;
    }

    public void setUpkeepWarningTime(long upkeepWarningTime) {
        this.upkeepWarningTime = upkeepWarningTime;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    /**
     * Kasaba hazinesi sıfırlandıktan sonra 24 saat geçmişse kasaba harabedir (Ruin).
     * Koruma tamamen kalkar, sandıklar yağmaya açılır.
     */
    public boolean isRuined(long ruinDurationMillis) {
        if (upkeepWarningTime <= 0) return false;
        return (System.currentTimeMillis() - upkeepWarningTime) >= ruinDurationMillis;
    }

    public boolean isInRuinWarning(long ruinDurationMillis) {
        if (upkeepWarningTime <= 0) return false;
        return (System.currentTimeMillis() - upkeepWarningTime) < ruinDurationMillis;
    }

    public long getRemainingRuinMillis(long ruinDurationMillis) {
        if (upkeepWarningTime <= 0) return 0;
        long elapsed = System.currentTimeMillis() - upkeepWarningTime;
        return Math.max(0, ruinDurationMillis - elapsed);
    }

    public int getMaxClaims(int base, int perLevel) {
        return base + ((level - 1) * perLevel);
    }

    public int getMaxMembers(int base, int perLevel) {
        return base + ((level - 1) * perLevel);
    }
}
