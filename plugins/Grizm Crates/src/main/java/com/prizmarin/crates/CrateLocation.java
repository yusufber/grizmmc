package com.prizmarin.crates;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class CrateLocation {

    private final String id;
    private final CrateTier tier;
    private final String worldName;
    private final int x;
    private final int y;
    private final int z;

    public CrateLocation(String id, CrateTier tier, String worldName, int x, int y, int z) {
        this.id = id;
        this.tier = tier;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public String getId() {
        return id;
    }

    public CrateTier getTier() {
        return tier;
    }

    public String getWorldName() {
        return worldName;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public Location toLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world, x, y, z);
    }

    public boolean matches(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        return loc.getWorld().getName().equals(worldName)
                && loc.getBlockX() == x
                && loc.getBlockY() == y
                && loc.getBlockZ() == z;
    }
}
