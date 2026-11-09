package com.grizmtech.oneblock.managers;

import com.grizmtech.oneblock.GrizmOneBlock;
import com.grizmtech.oneblock.models.IslandPermission;
import com.grizmtech.oneblock.models.IslandRole;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RoleManager {

    private final GrizmOneBlock plugin;
    // IslandOwner -> Set of PlayerUUIDs
    private final Map<UUID, Set<UUID>> activeCoops = new ConcurrentHashMap<>();

    public RoleManager(GrizmOneBlock plugin) {
        this.plugin = plugin;
    }

    public IslandRole getRole(UUID islandOwner, UUID playerUUID) {
        if (islandOwner == null || playerUUID == null) return IslandRole.VISITOR;
        if (islandOwner.equals(playerUUID)) return IslandRole.OWNER;

        Set<UUID> coops = activeCoops.get(islandOwner);
        if (coops != null && coops.contains(playerUUID)) {
            return IslandRole.COOP;
        }

        String roleStr = plugin.getDataManager().getConfig().getString("players." + islandOwner + ".members." + playerUUID);
        if (roleStr != null) {
            try {
                return IslandRole.valueOf(roleStr);
            } catch (Exception ignored) {}
        }

        return IslandRole.VISITOR;
    }

    public void addCoop(UUID islandOwner, UUID targetUUID) {
        activeCoops.computeIfAbsent(islandOwner, k -> ConcurrentHashMap.newKeySet()).add(targetUUID);
    }

    public void removeCoop(UUID islandOwner, UUID targetUUID) {
        Set<UUID> coops = activeCoops.get(islandOwner);
        if (coops != null) {
            coops.remove(targetUUID);
        }
    }

    public void clearCoopsForOwner(UUID islandOwner) {
        activeCoops.remove(islandOwner);
    }

    public void clearCoopsForPlayer(UUID targetUUID) {
        for (Set<UUID> set : activeCoops.values()) {
            set.remove(targetUUID);
        }
    }

    public boolean hasPermission(UUID islandOwner, UUID playerUUID, IslandPermission perm) {
        if (islandOwner == null || playerUUID == null) return false;
        if (islandOwner.equals(playerUUID)) return true;

        IslandRole role = getRole(islandOwner, playerUUID);
        if (role == IslandRole.OWNER) return true;

        // Özel ayarlanmış izin var mı?
        String path = "players." + islandOwner + ".permissions." + role.name() + "." + perm.name();
        if (plugin.getDataManager().getConfig().contains(path)) {
            return plugin.getDataManager().getConfig().getBoolean(path);
        }

        // Varsayılan İzin Matrisi
        return getDefaultPermission(role, perm);
    }

    public void setPermission(UUID islandOwner, IslandRole role, IslandPermission perm, boolean allowed) {
        String path = "players." + islandOwner + ".permissions." + role.name() + "." + perm.name();
        plugin.getDataManager().getConfig().set(path, allowed);
        plugin.getDataManager().saveConfig();
    }

    public boolean getDefaultPermission(IslandRole role, IslandPermission perm) {
        switch (role) {
            case OWNER:
            case MODERATOR:
                return true;
            case MEMBER:
                return perm == IslandPermission.BREAK_BLOCK
                        || perm == IslandPermission.PLACE_BLOCK
                        || perm == IslandPermission.OPEN_CONTAINER
                        || perm == IslandPermission.USE_REDSTONE
                        || perm == IslandPermission.BANK_DEPOSIT
                        || perm == IslandPermission.MINE_ONEBLOCK;
            case COOP:
                return perm == IslandPermission.BREAK_BLOCK
                        || perm == IslandPermission.PLACE_BLOCK
                        || perm == IslandPermission.OPEN_CONTAINER
                        || perm == IslandPermission.USE_REDSTONE
                        || perm == IslandPermission.MINE_ONEBLOCK;
            case VISITOR:
            default:
                return false;
        }
    }

    public void setMemberRole(UUID islandOwner, UUID targetUUID, IslandRole role) {
        if (role == null || role == IslandRole.VISITOR) {
            plugin.getDataManager().getConfig().set("players." + islandOwner + ".members." + targetUUID, null);
        } else {
            plugin.getDataManager().getConfig().set("players." + islandOwner + ".members." + targetUUID, role.name());
        }
        plugin.getDataManager().saveConfig();
    }

    public Map<UUID, IslandRole> getMembers(UUID islandOwner) {
        Map<UUID, IslandRole> map = new HashMap<>();
        String path = "players." + islandOwner + ".members";
        if (plugin.getDataManager().getConfig().contains(path)) {
            for (String uuidStr : plugin.getDataManager().getConfig().getConfigurationSection(path).getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    IslandRole role = IslandRole.valueOf(plugin.getDataManager().getConfig().getString(path + "." + uuidStr));
                    map.put(uuid, role);
                } catch (Exception ignored) {}
            }
        }
        return map;
    }
}
