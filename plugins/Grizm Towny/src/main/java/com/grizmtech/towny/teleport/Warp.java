package com.grizmtech.towny.teleport;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

public class Warp {

    private final String warpId;
    private final UUID townUuid; // null ise sunucu warptır
    private final UUID worldUuid;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private double visitCost;
    private String permission;

    public Warp(String warpId, UUID townUuid, UUID worldUuid, double x, double y, double z,
                float yaw, float pitch, double visitCost, String permission) {
        this.warpId = warpId;
        this.townUuid = townUuid;
        this.worldUuid = worldUuid;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.visitCost = Math.max(0.0, visitCost);
        this.permission = permission;
    }

    public boolean isServerWarp() {
        return townUuid == null;
    }

    public boolean isTownWarp() {
        return townUuid != null;
    }

    public Location toLocation() {
        World world = Bukkit.getWorld(worldUuid);
        if (world == null) return null;
        return new Location(world, x, y, z, yaw, pitch);
    }

    public String getWarpId() { return warpId; }
    public UUID getTownUuid() { return townUuid; }
    public UUID getWorldUuid() { return worldUuid; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public double getVisitCost() { return visitCost; }
    public void setVisitCost(double visitCost) { this.visitCost = visitCost; }
    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }
}
