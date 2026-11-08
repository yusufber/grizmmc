package com.grizmtech.perms.listener;

import com.grizmtech.perms.GrizmPermsPlugin;
import com.grizmtech.perms.engine.PermissionManager;
import com.grizmtech.perms.model.Role;
import com.grizmtech.perms.util.ColorUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

@SuppressWarnings("deprecation")
public class ChatFormatListener implements Listener {

    private final GrizmPermsPlugin plugin;
    private final PermissionManager manager;

    public ChatFormatListener(GrizmPermsPlugin plugin, PermissionManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAsyncChat(AsyncPlayerChatEvent event) {
        if (!plugin.getConfig().getBoolean("chat.enabled", true)) {
            return;
        }

        Player player = event.getPlayer();
        Role role = manager.getPrimaryRole(player.getName());

        String rawFormat = plugin.getConfig().getString(
                "chat.format",
                "{prefix}{player}{suffix} &8» {chat_color}{message}"
        );

        String message = event.getMessage();
        if (player.hasPermission("essentials.chat.color") || player.hasPermission("grizmperms.chat.color")) {
            message = ColorUtil.colorize(message);
        }

        String formattedHead = ColorUtil.colorize(
                rawFormat
                        .replace("{prefix}", manager.getDisplayPrefix(player.getName()))
                        .replace("{suffix}", manager.getDisplaySuffix(player.getName()))
                        .replace("{role}", role.getName())
                        .replace("{chat_color}", role.getChatColor())
                        .replace("{player}", "%1$s")
                        .replace("{message}", "%2$s")
        );

        event.setMessage(message);
        event.setFormat(formattedHead);
    }
}

