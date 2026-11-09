package com.prizmarin.crates.command;

import com.prizmarin.crates.CrateTier;
import com.prizmarin.crates.CratesPlugin;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GiveKeyCommand implements CommandExecutor, TabCompleter {

    private final CratesPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public GiveKeyCommand(CratesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("kasa.admin")) {
            sender.sendMessage(plugin.getPrefixedMessage("no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(miniMessage.deserialize("<red>Kullanım: /anahtarver <oyuncu> <seviye> [adet]</red>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            String msg = plugin.getConfig().getString("messages.player-not-found", "<red>Oyuncu bulunamadı: <yellow>{player}</yellow></red>");
            sender.sendMessage(plugin.getPrefix().append(miniMessage.deserialize(msg.replace("{player}", args[0]))));
            return true;
        }

        CrateTier tier = CrateTier.fromId(args[1]);
        if (tier == null) {
            String invalidTierMsg = plugin.getConfig().getString("messages.invalid-tier", "<red>Geçersiz kasa kademesi! Mevcut kademeler: <yellow>{tiers}</yellow></red>");
            StringBuilder tiersList = new StringBuilder();
            for (CrateTier t : CrateTier.values()) {
                tiersList.append(t.getId()).append(" ");
            }
            sender.sendMessage(plugin.getPrefix().append(miniMessage.deserialize(invalidTierMsg.replace("{tiers}", tiersList.toString().trim()))));
            return true;
        }

        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Math.max(1, Integer.parseInt(args[2]));
            } catch (NumberFormatException e) {
                sender.sendMessage(miniMessage.deserialize("<red>Lütfen geçerli bir sayı girin!</red>"));
                return true;
            }
        }

        plugin.getKeyManager().giveKey(target, tier, amount);

        String keyName = plugin.getConfig().getString("tiers." + tier.getId() + ".key-name", tier.getDisplayName() + " Anahtarı");

        // Message to sender
        String senderMsg = plugin.getConfig().getString("messages.key-given", "<green><yellow>{amount}</yellow> adet <yellow>{key}</yellow> anahtarı <aqua>{player}</aqua> adlı oyuncuya verildi.</green>");
        senderMsg = senderMsg.replace("{amount}", String.valueOf(amount))
                .replace("{key}", keyName)
                .replace("{player}", target.getName());
        sender.sendMessage(plugin.getPrefix().append(miniMessage.deserialize(senderMsg)));

        // Message to target
        String targetMsg = plugin.getConfig().getString("messages.key-received", "<green>Hesabınıza <yellow>{amount}</yellow> adet <yellow>{key}</yellow> anahtarı eklendi!</green>");
        targetMsg = targetMsg.replace("{amount}", String.valueOf(amount))
                .replace("{key}", keyName);
        target.sendMessage(plugin.getPrefix().append(miniMessage.deserialize(targetMsg)));

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("kasa.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    list.add(player.getName());
                }
            }
            return list;
        }

        if (args.length == 2) {
            List<String> list = new ArrayList<>();
            for (CrateTier tier : CrateTier.values()) {
                if (tier.getId().startsWith(args[1].toLowerCase())) {
                    list.add(tier.getId());
                }
            }
            return list;
        }

        if (args.length == 3) {
            return List.of("1", "5", "10", "32", "64");
        }

        return Collections.emptyList();
    }
}
