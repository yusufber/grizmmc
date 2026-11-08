package com.grizmtech.perms.hook;

import com.grizmtech.perms.GrizmPermsPlugin;
import com.grizmtech.perms.engine.PermissionManager;
import com.grizmtech.perms.model.Role;
import com.grizmtech.perms.model.RoleAssignment;
import com.grizmtech.perms.model.UserData;
import com.grizmtech.perms.util.ColorUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * PlaceholderAPI entegrasyonu (%grizmperms_prefix%, %grizmperms_role_name%, %grizmperms_expiry% vb.)
 */
public class GrizmPermsExpansion extends PlaceholderExpansion {

    private final GrizmPermsPlugin plugin;
    private final PermissionManager manager;

    public GrizmPermsExpansion(GrizmPermsPlugin plugin, PermissionManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "grizmperms";
    }

    @Override
    public @NotNull String getAuthor() {
        return "GrizmTech";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null || player.getName() == null) {
            return "";
        }
        String name = player.getName();
        Role primary = manager.getPrimaryRole(name);
        String key = params.toLowerCase(Locale.ROOT);

        return switch (key) {
            case "prefix", "tag_prefix", "tab_prefix" -> ColorUtil.colorize(manager.getDisplayPrefix(name));
            case "suffix", "tag_suffix", "tab_suffix" -> ColorUtil.colorize(manager.getDisplaySuffix(name));
            case "role", "group" -> primary.getId();
            case "role_name", "group_name" -> primary.getName();
            case "roles" -> String.join(", ", manager.getPlayerActiveGroupIds(name));
            case "perms", "permissions" -> {
                var exact = manager.getEffectivePermissions(name).getExactNodes().keySet();
                yield exact.isEmpty() ? "Varsayılan" : String.join(", ", exact);
            }
            case "chat_color" -> ColorUtil.colorize(primary.getChatColor());
            case "weight" -> String.valueOf(primary.getWeight());
            case "vip" -> {
                RoleAssignment vipAss = manager.getActiveVipAssignment(name);
                if (vipAss == null) yield "Yok";
                Role vipRole = manager.getRole(vipAss.getRoleId());
                yield vipRole != null ? vipRole.getName() : vipAss.getRoleId();
            }
            case "expiry", "vip_remaining" -> {
                RoleAssignment vipAss = manager.getActiveVipAssignment(name);
                if (vipAss == null) yield "Yok";
                yield ColorUtil.formatRemainingSeconds(vipAss.getExpiresAt());
            }
            case "credits", "balance" -> {
                UserData u = manager.getUserData(name);
                double c = u != null ? u.getCredits() : 0.0;
                yield String.format(Locale.US, "%.2f", c);
            }
            default -> "";
        };
    }
}

