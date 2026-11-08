package com.grizmtech.perms.hook;

import com.grizmtech.perms.GrizmPermsPlugin;
import com.grizmtech.perms.engine.PermissionManager;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;

public final class VaultHook {

    private VaultHook() {
    }

    public static void register(GrizmPermsPlugin plugin, PermissionManager manager) {
        VaultPermissionProvider permProvider = new VaultPermissionProvider(plugin, manager);
        VaultChatProvider chatProvider = new VaultChatProvider(plugin, permProvider, manager);

        Bukkit.getServicesManager().register(Permission.class, permProvider, plugin, ServicePriority.Highest);
        Bukkit.getServicesManager().register(Chat.class, chatProvider, plugin, ServicePriority.Highest);
        plugin.getLogger().info("[GrizmPerms] Vault Permission & Chat servisleri (Highest Priority) kaydedildi.");
    }
}

