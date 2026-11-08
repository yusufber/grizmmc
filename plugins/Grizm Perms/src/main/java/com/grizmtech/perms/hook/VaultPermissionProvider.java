package com.grizmtech.perms.hook;

import com.google.gson.JsonObject;
import com.grizmtech.perms.GrizmPermsPlugin;
import com.grizmtech.perms.engine.EffectivePermissions;
import com.grizmtech.perms.engine.PermissionManager;
import com.grizmtech.perms.model.Role;
import com.grizmtech.perms.model.RoleAssignment;
import com.grizmtech.perms.model.UserData;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

@SuppressWarnings("deprecation")
public class VaultPermissionProvider extends Permission {

    private final GrizmPermsPlugin grizmPlugin;
    private final PermissionManager manager;

    public VaultPermissionProvider(GrizmPermsPlugin plugin, PermissionManager manager) {
        this.plugin = plugin;
        this.grizmPlugin = plugin;
        this.manager = manager;
    }

    @Override
    public String getName() {
        return "GrizmPerms";
    }

    @Override
    public boolean isEnabled() {
        return grizmPlugin.isEnabled();
    }

    @Override
    public boolean hasSuperPermsCompat() {
        return true;
    }

    @Override
    public boolean hasGroupSupport() {
        return true;
    }

    @Override
    public boolean playerHas(String world, String player, String permission) {
        Player online = Bukkit.getPlayerExact(player);
        if (online != null && online.isOnline()) {
            return online.hasPermission(permission);
        }
        return manager.hasPermission(player, permission);
    }

    @Override
    public boolean playerAdd(String world, String player, String permission) {
        UserData user = manager.getOrCreateUserData(player);
        if (!user.getPermissions().contains(permission)) {
            user.getPermissions().add(permission);
            manager.updateSingleUser(user, manager.getCurrentRevision());

            JsonObject mut = new JsonObject();
            mut.addProperty("operation", "add_user_perm");
            mut.addProperty("actor", "Vault");
            mut.addProperty("username", player);
            mut.addProperty("permission", permission);
            grizmPlugin.getSyncService().pushMutationAsync(mut);
        }
        return true;
    }

    @Override
    public boolean playerRemove(String world, String player, String permission) {
        UserData user = manager.getUserData(player);
        if (user == null) return false;
        boolean removed = user.getPermissions().removeIf(p -> p.equalsIgnoreCase(permission));
        if (removed) {
            manager.updateSingleUser(user, manager.getCurrentRevision());
            JsonObject mut = new JsonObject();
            mut.addProperty("operation", "remove_user_perm");
            mut.addProperty("actor", "Vault");
            mut.addProperty("username", player);
            mut.addProperty("permission", permission);
            grizmPlugin.getSyncService().pushMutationAsync(mut);
        }
        return removed;
    }

    @Override
    public boolean groupHas(String world, String group, String permission) {
        Role role = manager.getRole(group);
        if (role == null) return false;
        EffectivePermissions eff = EffectivePermissions.compile(role.getPermissions());
        Boolean res = eff.resolve(permission);
        return res != null && res;
    }

    @Override
    public boolean groupAdd(String world, String group, String permission) {
        Role role = manager.getRole(group);
        if (role == null) return false;
        if (!role.getPermissions().contains(permission)) {
            role.getPermissions().add(permission);
            manager.saveLocalCache();
            for (Player p : Bukkit.getOnlinePlayers()) {
                manager.refreshPlayer(p);
            }
            JsonObject mut = new JsonObject();
            mut.addProperty("operation", "add_role_perm");
            mut.addProperty("actor", "Vault");
            mut.addProperty("role_id", role.getId());
            mut.addProperty("permission", permission);
            grizmPlugin.getSyncService().pushMutationAsync(mut);
        }
        return true;
    }

    @Override
    public boolean groupRemove(String world, String group, String permission) {
        Role role = manager.getRole(group);
        if (role == null) return false;
        boolean removed = role.getPermissions().removeIf(p -> p.equalsIgnoreCase(permission));
        if (removed) {
            manager.saveLocalCache();
            for (Player p : Bukkit.getOnlinePlayers()) {
                manager.refreshPlayer(p);
            }
            JsonObject mut = new JsonObject();
            mut.addProperty("operation", "remove_role_perm");
            mut.addProperty("actor", "Vault");
            mut.addProperty("role_id", role.getId());
            mut.addProperty("permission", permission);
            grizmPlugin.getSyncService().pushMutationAsync(mut);
        }
        return removed;
    }

    @Override
    public boolean playerInGroup(String world, String player, String group) {
        return manager.isPlayerInGroup(player, group);
    }

    @Override
    public boolean playerAddGroup(String world, String player, String group) {
        Role role = manager.getRole(group);
        if (role == null) return false;
        UserData user = manager.getOrCreateUserData(player);
        long now = System.currentTimeMillis() / 1000L;
        boolean exists = user.getRoles().stream().anyMatch(a -> a.getRoleId().equalsIgnoreCase(role.getId()) && !a.isExpired(now));
        if (!exists) {
            user.getRoles().add(new RoleAssignment(role.getId(), now, 0L, "vault"));
            manager.updateSingleUser(user, manager.getCurrentRevision());

            JsonObject mut = new JsonObject();
            mut.addProperty("operation", "grant_role");
            mut.addProperty("actor", "Vault");
            mut.addProperty("username", player);
            mut.addProperty("role_id", role.getId());
            mut.addProperty("duration_days", 0);
            grizmPlugin.getSyncService().pushMutationAsync(mut);
        }
        return true;
    }

    @Override
    public boolean playerRemoveGroup(String world, String player, String group) {
        UserData user = manager.getUserData(player);
        if (user == null) return false;
        boolean removed = user.getRoles().removeIf(a -> a.getRoleId().equalsIgnoreCase(group));
        if (removed) {
            manager.updateSingleUser(user, manager.getCurrentRevision());
            JsonObject mut = new JsonObject();
            mut.addProperty("operation", "revoke_role");
            mut.addProperty("actor", "Vault");
            mut.addProperty("username", player);
            mut.addProperty("role_id", group.toLowerCase());
            grizmPlugin.getSyncService().pushMutationAsync(mut);
        }
        return removed;
    }

    @Override
    public String[] getPlayerGroups(String world, String player) {
        List<String> groups = manager.getPlayerActiveGroupIds(player);
        return groups.toArray(new String[0]);
    }

    @Override
    public String getPrimaryGroup(String world, String player) {
        return manager.getPrimaryRole(player).getId();
    }

    @Override
    public String[] getGroups() {
        return manager.getAllRolesSorted().stream().map(Role::getId).toArray(String[]::new);
    }
}

