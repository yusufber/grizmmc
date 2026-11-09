package com.grizmtech.oneblock.models;

import org.bukkit.Location;

import java.util.UUID;

public class IslandWarp {
    private final UUID ownerUUID;
    private final String ownerName;
    private final Location location;
    private final WarpCategory category;
    private final String description;
    private int likes;
    private final long createdTime;

    public IslandWarp(UUID ownerUUID, String ownerName, Location location, WarpCategory category, String description, int likes, long createdTime) {
        this.ownerUUID = ownerUUID;
        this.ownerName = ownerName;
        this.location = location;
        this.category = category;
        this.description = description;
        this.likes = likes;
        this.createdTime = createdTime;
    }

    public UUID getOwnerUUID() { return ownerUUID; }
    public String getOwnerName() { return ownerName; }
    public Location getLocation() { return location; }
    public WarpCategory getCategory() { return category; }
    public String getDescription() { return description; }
    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = likes; }
    public long getCreatedTime() { return createdTime; }
}
