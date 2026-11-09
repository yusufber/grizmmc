package com.grizmtech.oneblock.models;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GeneratorMinion {

    private final UUID id;
    private final UUID ownerUUID;
    private Location location;
    private BlockFace targetFace;
    private int level;
    private final List<ItemStack> storage;
    private UUID armorStandUUID;
    private UUID hologramUUID;

    public GeneratorMinion(UUID id, UUID ownerUUID, Location location, BlockFace targetFace, int level) {
        this.id = id != null ? id : UUID.randomUUID();
        this.ownerUUID = ownerUUID;
        this.location = location;
        this.targetFace = targetFace != null ? targetFace : BlockFace.NORTH;
        this.level = Math.max(1, Math.min(5, level));
        this.storage = new ArrayList<>();
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public BlockFace getTargetFace() {
        return targetFace;
    }

    public void setTargetFace(BlockFace targetFace) {
        this.targetFace = targetFace;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.max(1, Math.min(5, level));
    }

    public List<ItemStack> getStorage() {
        return storage;
    }

    public UUID getArmorStandUUID() {
        return armorStandUUID;
    }

    public void setArmorStandUUID(UUID armorStandUUID) {
        this.armorStandUUID = armorStandUUID;
    }

    public UUID getHologramUUID() {
        return hologramUUID;
    }

    public void setHologramUUID(UUID hologramUUID) {
        this.hologramUUID = hologramUUID;
    }

    public int getMaxStorageSlots() {
        // Seviye 1: 9 slot, Seviye 2: 12, Seviye 3: 15, Seviye 4: 18, Seviye 5: 21 slot
        return 6 + (level * 3);
    }

    public int getBreakIntervalSeconds() {
        // Seviye 1: 6s, Seviye 2: 4.5s, Seviye 3: 3.5s, Seviye 4: 2.5s, Seviye 5: 1.5s
        return Math.max(1, 6 - (level - 1));
    }

    public double getUpgradeCost() {
        return switch (level) {
            case 1 -> 5000.0;
            case 2 -> 15000.0;
            case 3 -> 40000.0;
            case 4 -> 100000.0;
            default -> 0.0;
        };
    }
}

