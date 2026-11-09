package com.grizmtech.towny.town;

import java.util.UUID;

public class TownMember {

    private final UUID playerUuid;
    private final UUID townUuid;
    private TownRole role;
    private final long joinedAt;
    private double contributedMoney;

    public TownMember(UUID playerUuid, UUID townUuid, TownRole role, long joinedAt, double contributedMoney) {
        this.playerUuid = playerUuid;
        this.townUuid = townUuid;
        this.role = role;
        this.joinedAt = joinedAt;
        this.contributedMoney = contributedMoney;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public UUID getTownUuid() {
        return townUuid;
    }

    public TownRole getRole() {
        return role;
    }

    public void setRole(TownRole role) {
        this.role = role;
    }

    public long getJoinedAt() {
        return joinedAt;
    }

    public double getContributedMoney() {
        return contributedMoney;
    }

    public void addContribution(double amount) {
        this.contributedMoney += amount;
    }
}
