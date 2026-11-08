package com.grizmtech.perms.command;

import com.grizmtech.perms.GrizmPermsPlugin;
import com.grizmtech.perms.engine.PermissionManager;
import com.grizmtech.perms.model.Role;
import com.grizmtech.perms.model.RoleAssignment;
import com.grizmtech.perms.model.UserData;
import com.grizmtech.perms.util.ColorUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class VipInfoCommand implements CommandExecutor {

    private final GrizmPermsPlugin plugin;
    private final PermissionManager manager;

    public VipInfoCommand(GrizmPermsPlugin plugin, PermissionManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String targetName;
        if (args.length >= 1 && (sender.hasPermission("grizmperms.admin") || sender.isOp())) {
            targetName = args[0];
        } else if (sender instanceof Player p) {
            targetName = p.getName();
        } else {
            sender.sendMessage(ColorUtil.colorize("&cKullanım: /vipbilgi <oyuncu>"));
            return true;
        }

        Role primary = manager.getPrimaryRole(targetName);
        RoleAssignment vipAss = manager.getActiveVipAssignment(targetName);
        UserData u = manager.getUserData(targetName);
        double credits = u != null ? u.getCredits() : 0.0;
        String storeUrl = plugin.getConfig().getString("messages.store-url", "www.grizm.tech/mc/store.php");

        sender.sendMessage(ColorUtil.colorize("&8&m--------------------------------------------------"));
        sender.sendMessage(ColorUtil.colorize(" &f&lGRIZM&7&lMC &8» &fRütbe & VIP Bilgisi (&e" + targetName + "&f)"));
        sender.sendMessage(ColorUtil.colorize(" &8▪ &7Aktif Birincil Rol: &f" + primary.getName() + " &8(" + primary.getPrefix() + targetName + "&8)"));
        if (vipAss != null) {
            Role vipRole = manager.getRole(vipAss.getRoleId());
            String vipName = vipRole != null ? vipRole.getName() : vipAss.getRoleId();
            sender.sendMessage(ColorUtil.colorize(" &8▪ &7Aktif VIP Paketi: &e" + vipName));
            sender.sendMessage(ColorUtil.colorize(" &8▪ &7Kalan VIP Süresi: &a" + ColorUtil.formatRemainingSeconds(vipAss.getExpiresAt())));
        } else {
            sender.sendMessage(ColorUtil.colorize(" &8▪ &7Aktif VIP Paketi: &cBulunmuyor"));
        }
        sender.sendMessage(ColorUtil.colorize(" &8▪ &7Web Cüzdan Bakiyesi: &f" + String.format(Locale.US, "%.2f", credits) + " ₺"));
        sender.sendMessage(ColorUtil.colorize(" &8▪ &7VIP Mağaza: &b" + storeUrl));
        sender.sendMessage(ColorUtil.colorize("&8&m--------------------------------------------------"));
        return true;
    }
}

