package com.grizmtech.perms.hook;

import com.google.gson.JsonObject;
import com.grizmtech.perms.GrizmPermsPlugin;
import com.grizmtech.perms.engine.PermissionManager;
import com.grizmtech.perms.model.Role;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;

@SuppressWarnings("deprecation")
public class VaultChatProvider extends Chat {

    private final GrizmPermsPlugin plugin;
    private final PermissionManager manager;

    public VaultChatProvider(GrizmPermsPlugin plugin, Permission perms, PermissionManager manager) {
        super(perms);
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public String getName() {
        return "GrizmPerms-Chat";
    }

    @Override
    public boolean isEnabled() {
        return plugin.isEnabled();
    }

    @Override
    public String getPlayerPrefix(String world, String player) {
        return manager.getDisplayPrefix(player);
    }

    @Override
    public void setPlayerPrefix(String world, String player, String prefix) {
        // Grup tabanlı prefix yönetimi kullanılmaktadır
    }

    @Override
    public String getPlayerSuffix(String world, String player) {
        return manager.getDisplaySuffix(player);
    }

    @Override
    public void setPlayerSuffix(String world, String player, String suffix) {
    }

    @Override
    public String getGroupPrefix(String world, String group) {
        Role role = manager.getRole(group);
        return role != null ? role.getPrefix() : "";
    }

    @Override
    public void setGroupPrefix(String world, String group, String prefix) {
        Role role = manager.getRole(group);
        if (role != null) {
            role.setPrefix(prefix);
            manager.saveLocalCache();
            JsonObject mut = new JsonObject();
            mut.addProperty("operation", "update_role_meta");
            mut.addProperty("role_id", role.getId());
            mut.addProperty("prefix", prefix);
            plugin.getSyncService().pushMutationAsync(mut);
        }
    }

    @Override
    public String getGroupSuffix(String world, String group) {
        Role role = manager.getRole(group);
        return role != null ? role.getSuffix() : "";
    }

    @Override
    public void setGroupSuffix(String world, String group, String suffix) {
        Role role = manager.getRole(group);
        if (role != null) {
            role.setSuffix(suffix);
            manager.saveLocalCache();
            JsonObject mut = new JsonObject();
            mut.addProperty("operation", "update_role_meta");
            mut.addProperty("role_id", role.getId());
            mut.addProperty("suffix", suffix);
            plugin.getSyncService().pushMutationAsync(mut);
        }
    }

    @Override
    public int getPlayerInfoInteger(String world, String player, String node, int defaultValue) {
        if ("weight".equalsIgnoreCase(node)) {
            return manager.getPrimaryRole(player).getWeight();
        }
        return defaultValue;
    }

    @Override
    public void setPlayerInfoInteger(String world, String player, String node, int value) {
    }

    @Override
    public int getGroupInfoInteger(String world, String group, String node, int defaultValue) {
        Role r = manager.getRole(group);
        if (r != null && "weight".equalsIgnoreCase(node)) {
            return r.getWeight();
        }
        return defaultValue;
    }

    @Override
    public void setGroupInfoInteger(String world, String group, String node, int value) {
    }

    @Override
    public double getPlayerInfoDouble(String world, String player, String node, double defaultValue) {
        return defaultValue;
    }

    @Override
    public void setPlayerInfoDouble(String world, String player, String node, double value) {
    }

    @Override
    public double getGroupInfoDouble(String world, String group, String node, double defaultValue) {
        return defaultValue;
    }

    @Override
    public void setGroupInfoDouble(String world, String group, String node, double value) {
    }

    @Override
    public boolean getPlayerInfoBoolean(String world, String player, String node, boolean defaultValue) {
        return defaultValue;
    }

    @Override
    public void setPlayerInfoBoolean(String world, String player, String node, boolean value) {
    }

    @Override
    public boolean getGroupInfoBoolean(String world, String group, String node, boolean defaultValue) {
        return defaultValue;
    }

    @Override
    public void setGroupInfoBoolean(String world, String group, String node, boolean value) {
    }

    @Override
    public String getPlayerInfoString(String world, String player, String node, String defaultValue) {
        if ("prefix".equalsIgnoreCase(node)) return getPlayerPrefix(world, player);
        if ("suffix".equalsIgnoreCase(node)) return getPlayerSuffix(world, player);
        return defaultValue;
    }

    @Override
    public void setPlayerInfoString(String world, String player, String node, String value) {
    }

    @Override
    public String getGroupInfoString(String world, String group, String node, String defaultValue) {
        if ("prefix".equalsIgnoreCase(node)) return getGroupPrefix(world, group);
        if ("suffix".equalsIgnoreCase(node)) return getGroupSuffix(world, group);
        return defaultValue;
    }

    @Override
    public void setGroupInfoString(String world, String group, String node, String value) {
    }
}

