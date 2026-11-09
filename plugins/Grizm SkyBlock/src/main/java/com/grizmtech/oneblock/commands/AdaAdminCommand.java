package com.grizmtech.oneblock.commands;

import com.grizmtech.oneblock.GrizmOneBlock;
import com.grizmtech.oneblock.managers.PhaseManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class AdaAdminCommand implements CommandExecutor, TabCompleter {

    private final GrizmOneBlock plugin;

    public AdaAdminCommand(GrizmOneBlock plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("grizmoneblock.admin")) {
            sender.sendMessage(Component.text("Bu komut için yetkiniz yok!").color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(Component.text("=== Grizm OneBlock Yönetici Araçları ===").color(NamedTextColor.GOLD));
            sender.sendMessage(Component.text("/obadmin spy <oyuncu> - Adanın tüm verilerini uzaktan inceler").color(NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/obadmin setphase <oyuncu> <aşama> - Aşamayı manuel değiştirir").color(NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/obadmin setblocks <oyuncu> <sayı> - Kırılan blok sayısını ayarlar").color(NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/obadmin respawnblock <oyuncu> - Merkez bloğu yeniden oluşturur").color(NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/obadmin bank <oyuncu> <add/set/take> <miktar> - Bankayı yönetir").color(NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/obadmin minyonver <oyuncu> [seviye] - Su-Lav Jeneratör Minyonu verir").color(NamedTextColor.YELLOW));
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "spy":
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Kullanım: /obadmin spy <oyuncu>").color(NamedTextColor.RED));
                    return true;
                }
                if (!(sender instanceof Player admin)) {
                    sender.sendMessage("Bu komut sadece oyun içinden kullanılabilir.");
                    return true;
                }
                @SuppressWarnings("deprecation")
                OfflinePlayer spyTarget = Bukkit.getOfflinePlayer(args[1]);
                plugin.getGuiManager().openAdminSpyMenu(admin, spyTarget);
                return true;

            case "setphase":
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Kullanım: /obadmin setphase <oyuncu> <ORMAN/COL/NETHER/END vb.>").color(NamedTextColor.RED));
                    return true;
                }
                @SuppressWarnings("deprecation")
                OfflinePlayer phaseTarget = Bukkit.getOfflinePlayer(args[1]);
                try {
                    PhaseManager.Phase phase = PhaseManager.Phase.valueOf(args[2].toUpperCase());
                    plugin.getDataManager().setBlocksBroken(phaseTarget.getUniqueId(), phase.getRequiredBlocks());
                    plugin.getDataManager().saveConfig();
                    plugin.getHologramManager().updateHologram(phaseTarget.getUniqueId());
                    sender.sendMessage(Component.text(phaseTarget.getName() + " adlı oyuncunun aşaması " + phase.getName() + " yapıldı!").color(NamedTextColor.GREEN));
                } catch (Exception e) {
                    sender.sendMessage(Component.text("Geçersiz aşama adı!").color(NamedTextColor.RED));
                }
                return true;

            case "setblocks":
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Kullanım: /obadmin setblocks <oyuncu> <sayı>").color(NamedTextColor.RED));
                    return true;
                }
                @SuppressWarnings("deprecation")
                OfflinePlayer blocksTarget = Bukkit.getOfflinePlayer(args[1]);
                try {
                    int count = Integer.parseInt(args[2]);
                    plugin.getDataManager().setBlocksBroken(blocksTarget.getUniqueId(), count);
                    plugin.getDataManager().saveConfig();
                    plugin.getHologramManager().updateHologram(blocksTarget.getUniqueId());
                    sender.sendMessage(Component.text(blocksTarget.getName() + " adlı oyuncunun blok sayısı " + count + " yapıldı!").color(NamedTextColor.GREEN));
                } catch (Exception e) {
                    sender.sendMessage(Component.text("Geçersiz sayı!").color(NamedTextColor.RED));
                }
                return true;

            case "respawnblock":
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Kullanım: /obadmin respawnblock <oyuncu>").color(NamedTextColor.RED));
                    return true;
                }
                @SuppressWarnings("deprecation")
                OfflinePlayer respawnTarget = Bukkit.getOfflinePlayer(args[1]);
                Location loc = plugin.getDataManager().getOneBlockLocation(respawnTarget.getUniqueId());
                if (loc != null && loc.getWorld() != null) {
                    int blocks = plugin.getDataManager().getBlocksBroken(respawnTarget.getUniqueId());
                    PhaseManager.Phase phase = plugin.getPhaseManager().getCurrentPhase(blocks);
                    Material mat = plugin.getPhaseManager().getRandomBlock(phase, 1.0);
                    loc.getBlock().setType(mat);
                    plugin.getHologramManager().updateHologram(respawnTarget.getUniqueId());
                    sender.sendMessage(Component.text("Merkez blok başarıyla yenilendi: " + mat.name()).color(NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text("Oyuncunun ada dünyası bulunamadı!").color(NamedTextColor.RED));
                }
                return true;

            case "minyonver":
            case "jeneratorminyon":
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Kullanım: /obadmin minyonver <oyuncu> [seviye]").color(NamedTextColor.RED));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null || !target.isOnline()) {
                    sender.sendMessage(Component.text("Oyuncu bulunamadı veya çevrimdışı!").color(NamedTextColor.RED));
                    return true;
                }
                int lvl = 1;
                if (args.length >= 3) {
                    try {
                        lvl = Integer.parseInt(args[2]);
                    } catch (Exception ignored) {}
                }
                org.bukkit.inventory.ItemStack minionItem = plugin.getGeneratorMinionManager().createMinionItem(lvl);
                target.getInventory().addItem(minionItem);
                target.sendMessage(Component.text("★ Size Seviye " + lvl + " Jeneratör Minyonu verildi!").color(NamedTextColor.GREEN));
                sender.sendMessage(Component.text(target.getName() + " adlı oyuncuya Lv." + lvl + " Jeneratör Minyonu verildi.").color(NamedTextColor.GREEN));
                return true;

            case "bank":
                if (args.length < 4) {
                    sender.sendMessage(Component.text("Kullanım: /obadmin bank <oyuncu> <add|set|take> <miktar>").color(NamedTextColor.RED));
                    return true;
                }
                @SuppressWarnings("deprecation")
                OfflinePlayer bankTarget = Bukkit.getOfflinePlayer(args[1]);
                String action = args[2].toLowerCase();
                try {
                    double amount = Double.parseDouble(args[3]);
                    UUID targetUUID = bankTarget.getUniqueId();
                    double current = plugin.getBankManager().getBalance(targetUUID);

                    if (action.equals("add")) {
                        plugin.getBankManager().setBalance(targetUUID, current + amount);
                        sender.sendMessage(Component.text("Bankaya " + (long)amount + " eklendi. Yeni bakiye: " + (long)(current + amount)).color(NamedTextColor.GREEN));
                    } else if (action.equals("set")) {
                        plugin.getBankManager().setBalance(targetUUID, amount);
                        sender.sendMessage(Component.text("Banka bakiyesi " + (long)amount + " olarak ayarlandı.").color(NamedTextColor.GREEN));
                    } else if (action.equals("take")) {
                        plugin.getBankManager().setBalance(targetUUID, Math.max(0, current - amount));
                        sender.sendMessage(Component.text("Bankadan " + (long)amount + " çekildi. Yeni bakiye: " + (long)Math.max(0, current - amount)).color(NamedTextColor.GREEN));
                    }
                } catch (Exception e) {
                    sender.sendMessage(Component.text("Geçersiz miktar!").color(NamedTextColor.RED));
                }
                return true;
        }

        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("grizmoneblock.admin")) return java.util.Collections.emptyList();

        if (args.length == 1) {
            List<String> list = Arrays.asList("spy", "setphase", "setblocks", "respawnblock", "bank", "minyonver");
            List<String> filtered = new ArrayList<>();
            for (String s : list) {
                if (s.toLowerCase().startsWith(args[0].toLowerCase())) {
                    filtered.add(s);
                }
            }
            return filtered;
        } else if (args.length == 2) {
            List<String> playerNames = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    playerNames.add(p.getName());
                }
            }
            return playerNames;
        } else if (args.length == 3 && args[0].equalsIgnoreCase("setphase")) {
            List<String> phases = new ArrayList<>();
            for (PhaseManager.Phase p : PhaseManager.Phase.values()) {
                if (p.name().toLowerCase().startsWith(args[2].toLowerCase())) {
                    phases.add(p.name());
                }
            }
            return phases;
        } else if (args.length == 3 && args[0].equalsIgnoreCase("bank")) {
            return Arrays.asList("add", "set", "take");
        }
        return java.util.Collections.emptyList();
    }
}
