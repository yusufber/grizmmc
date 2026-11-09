package com.grizmtech.towny.commands;

import com.grizmtech.towny.claim.ClaimChunk;
import com.grizmtech.towny.claim.ClaimManager;
import com.grizmtech.towny.core.MessageUtil;
import com.grizmtech.towny.gui.PlotMenuGUI;
import com.grizmtech.towny.gui.TownMenuGUI;
import com.grizmtech.towny.gui.WarpMenuGUI;
import com.grizmtech.towny.storage.DatabaseManager;
import com.grizmtech.towny.teleport.WarpManager;
import com.grizmtech.towny.town.Town;
import com.grizmtech.towny.town.TownManager;
import com.grizmtech.towny.town.TownMember;
import com.grizmtech.towny.town.TownRole;
import com.grizmtech.towny.visual.BorderParticleTask;
import com.grizmtech.towny.war.WarManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TownCommand implements CommandExecutor, TabCompleter {

    private final TownManager townManager;
    private final ClaimManager claimManager;
    private final WarManager warManager;
    private final WarpManager warpManager;
    private final DatabaseManager databaseManager;
    private final BorderParticleTask borderTask;

    public TownCommand(TownManager townManager, ClaimManager claimManager, WarManager warManager,
                       WarpManager warpManager, DatabaseManager databaseManager, BorderParticleTask borderTask) {
        this.townManager = townManager;
        this.claimManager = claimManager;
        this.warManager = warManager;
        this.warpManager = warpManager;
        this.databaseManager = databaseManager;
        this.borderTask = borderTask;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Bu komut sadece oyuncular tarafından kullanılabilir!");
            return true;
        }

        if (args.length == 0) {
            TownMenuGUI.open(player, townManager, claimManager, warManager);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "menu" -> TownMenuGUI.open(player, townManager, claimManager, warManager);
            case "create" -> handleCreate(player, args);
            case "claim" -> handleClaim(player, args);
            case "unclaim" -> claimManager.unclaim(player);
            case "invite" -> handleInvite(player, args);
            case "join" -> handleJoin(player, args);
            case "kick" -> handleKick(player, args);
            case "rank" -> handleRank(player, args);
            case "deposit" -> handleDeposit(player, args);
            case "withdraw" -> handleWithdraw(player, args);
            case "plot" -> PlotMenuGUI.open(player, claimManager, townManager);
            case "spawn" -> handleSpawn(player);
            case "setspawn" -> handleSetSpawn(player);
            case "setspawncost" -> handleSetSpawnCost(player, args);
            case "border" -> handleBorder(player);
            case "setwarp" -> handleSetWarp(player, args);
            case "warp" -> handleWarp(player, args);
            case "disband" -> handleDisband(player);
            case "info" -> handleInfo(player, args);
            default -> sendHelp(player);
        }
        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendMessage(player, "<red>Kullanım: /t create <kasaba_ismi></red>");
            return;
        }
        boolean success = townManager.createTown(player, args[1]);
        if (success) {
            Town town = townManager.getTownByPlayer(player.getUniqueId());
            if (town != null) {
                claimManager.claim(player, false);
            }
        }
    }

    private void handleClaim(Player player, String[] args) {
        boolean isOutpost = (args.length >= 2 && args[1].equalsIgnoreCase("outpost"));
        claimManager.claim(player, isOutpost);
    }

    private void handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendMessage(player, "<red>Kullanım: /t invite <oyuncu></red>");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            MessageUtil.sendMessage(player, "<red>Oyuncu bulunamadı!</red>");
            return;
        }
        Town town = townManager.getTownByPlayer(player.getUniqueId());
        townManager.invitePlayer(town, player, target);
    }

    private void handleJoin(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendMessage(player, "<red>Kullanım: /t join <kasaba></red>");
            return;
        }
        townManager.joinTown(player, args[1]);
    }

    private void handleKick(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendMessage(player, "<red>Kullanım: /t kick <oyuncu></red>");
            return;
        }
        Town town = townManager.getTownByPlayer(player.getUniqueId());
        if (town == null) return;

        Player target = Bukkit.getPlayer(args[1]);
        UUID targetUuid = target != null ? target.getUniqueId() : null;
        if (targetUuid == null) {
            for (TownMember m : townManager.getTownMembers(town.getUuid())) {
                String name = Bukkit.getOfflinePlayer(m.getPlayerUuid()).getName();
                if (name != null && name.equalsIgnoreCase(args[1])) {
                    targetUuid = m.getPlayerUuid();
                    break;
                }
            }
        }

        if (targetUuid == null) {
            MessageUtil.sendMessage(player, "<red>Kasabanızda bu isimde bir üye bulunamadı!</red>");
            return;
        }

        townManager.kickPlayer(town, player, targetUuid);
    }

    private void handleRank(Player player, String[] args) {
        if (args.length < 3) {
            MessageUtil.sendMessage(player, "<red>Kullanım: /t rank <oyuncu> <MAYOR|CO_MAYOR|GENERAL|BUILDER|RESIDENT></red>");
            return;
        }
        Town town = townManager.getTownByPlayer(player.getUniqueId());
        if (town == null) return;

        TownRole role;
        try {
            role = TownRole.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            MessageUtil.sendMessage(player, "<red>Geçersiz rütbe! Geçerli rütbeler: CO_MAYOR, GENERAL, BUILDER, RESIDENT, MAYOR</red>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        UUID targetUuid = target != null ? target.getUniqueId() : null;
        if (targetUuid == null) {
            for (TownMember m : townManager.getTownMembers(town.getUuid())) {
                String name = Bukkit.getOfflinePlayer(m.getPlayerUuid()).getName();
                if (name != null && name.equalsIgnoreCase(args[1])) {
                    targetUuid = m.getPlayerUuid();
                    break;
                }
            }
        }

        if (targetUuid == null) {
            MessageUtil.sendMessage(player, "<red>Kasaba üyesi bulunamadı!</red>");
            return;
        }

        townManager.setRole(town, player, targetUuid, role);
    }

    private void handleDeposit(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendMessage(player, "<red>Kullanım: /t deposit <miktar></red>");
            return;
        }
        try {
            double amount = Double.parseDouble(args[1]);
            if (!Double.isFinite(amount) || amount <= 0) {
                MessageUtil.sendMessage(player, "<red>Geçersiz miktar!</red>");
                return;
            }
            Town town = townManager.getTownByPlayer(player.getUniqueId());
            if (town != null) {
                townManager.deposit(town, player, amount);
            }
        } catch (NumberFormatException e) {
            MessageUtil.sendMessage(player, "<red>Geçersiz miktar!</red>");
        }
    }

    private void handleWithdraw(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendMessage(player, "<red>Kullanım: /t withdraw <miktar></red>");
            return;
        }
        try {
            double amount = Double.parseDouble(args[1]);
            if (!Double.isFinite(amount) || amount <= 0) {
                MessageUtil.sendMessage(player, "<red>Geçersiz miktar!</red>");
                return;
            }
            Town town = townManager.getTownByPlayer(player.getUniqueId());
            if (town != null) {
                townManager.withdraw(town, player, amount);
            }
        } catch (NumberFormatException e) {
            MessageUtil.sendMessage(player, "<red>Geçersiz miktar!</red>");
        }
    }

    private void handleSpawn(Player player) {
        Town town = townManager.getTownByPlayer(player.getUniqueId());
        if (town == null) {
            MessageUtil.sendMessage(player, "<red>Bir kasabanız bulunmuyor!</red>");
            return;
        }
        warpManager.teleportToTownSpawn(player, town);
    }

    private void handleSetSpawn(Player player) {
        Town town = townManager.getTownByPlayer(player.getUniqueId());
        if (town == null) return;

        TownMember member = townManager.getMember(player.getUniqueId());
        if (member == null || !member.getRole().canSetSpawn()) {
            MessageUtil.sendMessage(player, "<red>Kasaba merkezini değiştirme yetkiniz yok!</red>");
            return;
        }

        Location loc = player.getLocation();
        ClaimChunk claim = claimManager.getClaim(loc);
        if (claim == null || !claim.getTownUuid().equals(town.getUuid())) {
            MessageUtil.sendMessage(player, "<red>Kasaba merkezi sadece kasabanızın sınırları içine yerleştirilebilir!</red>");
            return;
        }

        town.setHomeLocation(loc);
        databaseManager.saveTownAsync(town);
        MessageUtil.sendMessage(player, "<green>Kasaba merkezi ve spawn noktası başarıyla güncellendi!</green>");
    }

    private void handleSetSpawnCost(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendMessage(player, "<red>Kullanım: /t setspawncost <miktar></red>");
            return;
        }
        Town town = townManager.getTownByPlayer(player.getUniqueId());
        if (town == null) return;

        TownMember member = townManager.getMember(player.getUniqueId());
        if (member == null || !member.getRole().isMayor()) {
            MessageUtil.sendMessage(player, "<red>Sadece belediye başkanı giriş vergisini belirleyebilir!</red>");
            return;
        }

        try {
            double cost = Double.parseDouble(args[1]);
            if (!Double.isFinite(cost) || cost < 0) {
                MessageUtil.sendMessage(player, "<red>Geçersiz miktar!</red>");
                return;
            }
            town.setSpawnCost(cost);
            databaseManager.saveTownAsync(town);
            MessageUtil.sendMessage(player, "<green>Dışarıdan gelen ziyaretçiler için giriş vergisi güncellendi: <yellow>" + cost + "$</yellow></green>");
        } catch (NumberFormatException e) {
            MessageUtil.sendMessage(player, "<red>Geçersiz miktar!</red>");
        }
    }

    private void handleBorder(Player player) {
        boolean active = borderTask.toggle(player);
        if (active) {
            MessageUtil.sendMessage(player, "<green>Arazi sınır parçacıkları açıldı. Kapatmak için tekrar <yellow>/t border</yellow> yazın.</green>");
        } else {
            MessageUtil.sendMessage(player, "<yellow>Arazi sınır parçacıkları kapatıldı.</yellow>");
        }
    }

    private void handleSetWarp(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendMessage(player, "<red>Kullanım: /t setwarp <isim> [ucret]</red>");
            return;
        }
        Town town = townManager.getTownByPlayer(player.getUniqueId());
        if (town == null) return;

        TownMember member = townManager.getMember(player.getUniqueId());
        if (member == null || !member.getRole().canSetSpawn()) {
            MessageUtil.sendMessage(player, "<red>Kasaba warpı oluşturma yetkiniz yok!</red>");
            return;
        }

        ClaimChunk claim = claimManager.getClaim(player.getLocation());
        if (claim == null || !claim.getTownUuid().equals(town.getUuid())) {
            MessageUtil.sendMessage(player, "<red>Kasaba warpı sadece kasabanızın sınırları içine yerleştirilebilir!</red>");
            return;
        }

        double cost = 0.0;
        if (args.length >= 3) {
            try {
                cost = Double.parseDouble(args[2]);
            } catch (NumberFormatException e) {
                MessageUtil.sendMessage(player, "<red>Geçersiz miktar!</red>");
                return;
            }
        }
        if (!Double.isFinite(cost) || cost < 0) {
            MessageUtil.sendMessage(player, "<red>Geçersiz miktar!</red>");
            return;
        }
        warpManager.setTownWarp(player, town, args[1], cost);
    }

    private void handleWarp(Player player, String[] args) {
        if (args.length < 2) {
            WarpMenuGUI.open(player, warpManager, townManager);
            return;
        }
        Town town = townManager.getTownByPlayer(player.getUniqueId());
        if (town == null) return;

        String fullKey = town.getName().toLowerCase() + "_" + args[1].toLowerCase();
        com.grizmtech.towny.teleport.Warp warp = warpManager.getWarp(fullKey);
        if (warp != null) {
            warpManager.teleportToWarp(player, warp);
        } else {
            MessageUtil.sendMessage(player, "<red>Bu isimde bir kasaba warpı bulunamadı!</red>");
        }
    }

    private void handleDisband(Player player) {
        Town town = townManager.getTownByPlayer(player.getUniqueId());
        if (town != null) {
            claimManager.unclaimAll(town);
            townManager.disbandTown(town, player);
        }
    }

    private void handleInfo(Player player, String[] args) {
        Town town = args.length >= 2 ? townManager.getTown(args[1]) : townManager.getTownByPlayer(player.getUniqueId());
        if (town == null) {
            MessageUtil.sendMessage(player, "<red>Kasaba bulunamadı!</red>");
            return;
        }

        String leader = Bukkit.getOfflinePlayer(town.getLeaderUuid()).getName();
        int claims = claimManager.getTownClaimCount(town.getUuid());
        int members = townManager.getTownMembers(town.getUuid()).size();

        String info = String.format("<gold>═══════ [ <yellow><b>%s</b></yellow> ] ═══════</gold>\n" +
                "<gray>Belediye Başkanı: <yellow>%s</yellow></gray>\n" +
                "<gray>Seviye: <yellow>%d</yellow> | Banka: <yellow>%.1f$</yellow></gray>\n" +
                "<gray>Arazi Sayısı: <yellow>%d</yellow> | Üye Sayısı: <yellow>%d</yellow></gray>\n" +
                "<gray>Spawn Vergisi: <yellow>%.1f$</yellow></gray>\n" +
                "<gold>══════════════════════════</gold>",
                town.getName(), leader, town.getLevel(), town.getBankBalance(), claims, members, town.getSpawnCost());

        MessageUtil.sendRawMessage(player, info);
    }

    private void sendHelp(Player player) {
        String help = "<gold>═══════ [ <gradient:#ff8a00:#e52e71><b>GRIZM TOWNY KOMUTLARI</b></gradient> ] ═══════</gold>\n" +
                "<yellow>/t menu</yellow> <gray>- Kasaba yönetim panelini açar</gray>\n" +
                "<yellow>/t create <isim></yellow> <gray>- Yeni kasaba kurar</gray>\n" +
                "<yellow>/t claim [outpost]</yellow> <gray>- Bulunulan chunk'ı sahiplenir</gray>\n" +
                "<yellow>/t unclaim</yellow> <gray>- Bulunulan chunk'ı terk eder</gray>\n" +
                "<yellow>/t invite <oyuncu></yellow> <gray>- Kasabaya üye davet eder</gray>\n" +
                "<yellow>/t join <kasaba></yellow> <gray>- Kasaba davetini kabul eder</gray>\n" +
                "<yellow>/t deposit/withdraw <miktar></yellow> <gray>- Kasaba banka işlemleri</gray>\n" +
                "<yellow>/t spawn</yellow> <gray>- Kasaba merkezine ışınlar</gray>\n" +
                "<yellow>/t border</yellow> <gray>- 16x16 chunk sınır parçacıklarını açar</gray>\n" +
                "<yellow>/t plot</yellow> <gray>- Parsel izin ve satış menüsünü açar</gray>\n" +
                "<gold>════════════════════════════════════════</gold>";
        MessageUtil.sendRawMessage(player, help);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("menu", "create", "claim", "unclaim", "invite", "join", "kick", "rank",
                    "deposit", "withdraw", "plot", "spawn", "setspawn", "setspawncost", "border",
                    "setwarp", "warp", "disband", "info");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("invite") || args[0].equalsIgnoreCase("kick"))) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("rank")) {
            return List.of("MAYOR", "CO_MAYOR", "GENERAL", "BUILDER", "RESIDENT");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("claim")) {
            return List.of("outpost");
        }
        return Collections.emptyList();
    }
}
