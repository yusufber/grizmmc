package com.grizmtech.perms.engine;

import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.Permission;

/**
 * Paper / Spigot oyuncularının yerleşik PermissibleBase nesnesinin yerini alır.
 * Wildcard (*, plugin.*) ve negasyon (-izin) kurallarını gerçek zamanlı çözer.
 */
public class GrizmPermissible extends PermissibleBase {

    private final Player player;
    private final PermissionManager manager;

    public GrizmPermissible(Player player, PermissionManager manager) {
        super(player);
        this.player = player;
        this.manager = manager;
    }

    @Override
    public boolean isPermissionSet(String name) {
        if (manager != null && player != null && name != null) {
            EffectivePermissions eff = manager.getEffectivePermissions(player.getName());
            if (eff.resolve(name) != null) {
                return true;
            }
        }
        return super.isPermissionSet(name);
    }

    @Override
    public boolean isPermissionSet(Permission perm) {
        if (perm != null && isPermissionSet(perm.getName())) {
            return true;
        }
        return super.isPermissionSet(perm);
    }

    @Override
    public boolean hasPermission(String inName) {
        if (inName == null) {
            return false;
        }
        if (manager != null && player != null) {
            EffectivePermissions eff = manager.getEffectivePermissions(player.getName());
            Boolean resolved = eff.resolve(inName);
            if (resolved != null) {
                return resolved;
            }
        }
        return super.hasPermission(inName);
    }

    @Override
    public boolean hasPermission(Permission perm) {
        if (perm == null) {
            return false;
        }
        if (manager != null && player != null) {
            EffectivePermissions eff = manager.getEffectivePermissions(player.getName());
            Boolean resolved = eff.resolve(perm.getName());
            if (resolved != null) {
                return resolved;
            }
        }
        return super.hasPermission(perm);
    }
}

