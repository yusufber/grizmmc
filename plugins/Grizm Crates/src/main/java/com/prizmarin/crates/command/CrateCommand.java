package com.prizmarin.crates.command;

import com.prizmarin.crates.CrateLocation;
import com.prizmarin.crates.CrateTier;
import com.prizmarin.crates.CratesPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CrateCommand implements CommandExecutor, TabCompleter {

    private final CratesPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public CrateCommand(CratesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String cmdName = command.getName().toLowerCase();

        if (cmdName.equals("kasareload")) {
            if (!sender.hasPermission("kasa.admin")) {
                sender.sendMessage(plugin.getPrefixedMessage("no-permission"));
                return true;
            }
            plugin.reloadPlugin();
            sender.sendMessage(plugin.getPrefixedMessage("reloaded"));
            return true;
        }

        if (cmdName.equals("kasalist")) {
            if (!sender.hasPermission("kasa.admin")) {
                sender.sendMessage(plugin.getPrefixedMessage("no-permission"));
                return true;
            }
            Collection<CrateLocation> crates = plugin.getCrateRegistry().getAllCrates();
            if (crates.isEmpty()) {
                sender.sendMessage(plugin.getPrefixedMessage("crate-list-empty"));
                return true;
            }

            sender.sendMessage(plugin.getPrefixedMessage("crate-list-header"));
            String itemFormat = plugin.getConfig().getString("messages.crate-list-item", "<gray>- <aqua>{id}</aqua> | Kademe: {tier} | Konum: <yellow>{world}, {x}, {y}, {z}</yellow></gray>");
            for (CrateLocation cl : crates) {
                String line = itemFormat.replace("{id}", cl.getId())
                        .replace("{tier}", cl.getTier().getDisplayName())
                        .replace("{world}", cl.getWorldName())
                        .replace("{x}", String.valueOf(cl.getX()))
                        .replace("{y}", String.valueOf(cl.getY()))
                        .replace("{z}", String.valueOf(cl.getZ()));
                sender.sendMessage(miniMessage.deserialize(line));
            }
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getPrefixedMessage("player-only"));
            return true;
        }

        if (!player.hasPermission("kasa.admin")) {
            player.sendMessage(plugin.getPrefixedMessage("no-permission"));
            return true;
        }

        if (cmdName.equals("kasakoy")) {
            if (args.length < 1) {
                player.sendMessage(miniMessage.deserialize("<red>Kullanım: /kasakoy <seviye></red>"));
                return true;
            }

            CrateTier tier = CrateTier.fromId(args[0]);
            if (tier == null) {
                String invalidTierMsg = plugin.getConfig().getString("messages.invalid-tier", "<red>Geçersiz kasa kademesi! Mevcut kademeler: <yellow>{tiers}</yellow></red>");
                StringBuilder tiersList = new StringBuilder();
                for (CrateTier t : CrateTier.values()) {
                    tiersList.append(t.getId()).append(" ");
                }
                player.sendMessage(plugin.getPrefix().append(miniMessage.deserialize(invalidTierMsg.replace("{tiers}", tiersList.toString().trim()))));
                return true;
            }

            Block targetBlock = player.getTargetBlockExact(5);
            if (targetBlock == null || targetBlock.getType().isAir()) {
                player.sendMessage(plugin.getPrefixedMessage("target-block-invalid"));
                return true;
            }

            // Register crate
            plugin.getCrateRegistry().registerCrate(tier, targetBlock.getLocation());
            String createdMsg = plugin.getConfig().getString("messages.crate-created", "<green><b>{tier}</b> kasası başarıyla oluşturuldu!</green>");
            player.sendMessage(plugin.getPrefix().append(miniMessage.deserialize(createdMsg.replace("{tier}", tier.getDisplayName()))));
            return true;
        }

        if (cmdName.equals("kasasil")) {
            Block targetBlock = player.getTargetBlockExact(5);
            if (targetBlock == null) {
                player.sendMessage(plugin.getPrefixedMessage("target-block-invalid"));
                return true;
            }

            CrateLocation crate = plugin.getCrateRegistry().getCrateAt(targetBlock);
            if (crate == null) {
                player.sendMessage(plugin.getPrefixedMessage("crate-not-found"));
                return true;
            }

            plugin.getCrateRegistry().removeCrate(crate);
            player.sendMessage(plugin.getPrefixedMessage("crate-deleted"));
            return true;
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("kasa.admin")) {
            return Collections.emptyList();
        }

        if (command.getName().equalsIgnoreCase("kasakoy") && args.length == 1) {
            List<String> list = new ArrayList<>();
            for (CrateTier tier : CrateTier.values()) {
                if (tier.getId().startsWith(args[0].toLowerCase())) {
                    list.add(tier.getId());
                }
            }
            return list;
        }

        return Collections.emptyList();
    }
}
