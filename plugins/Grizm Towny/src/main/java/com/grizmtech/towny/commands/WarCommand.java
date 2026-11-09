package com.grizmtech.towny.commands;

import com.grizmtech.towny.core.MessageUtil;
import com.grizmtech.towny.town.Town;
import com.grizmtech.towny.town.TownManager;
import com.grizmtech.towny.town.TownMember;
import com.grizmtech.towny.war.SiegeWar;
import com.grizmtech.towny.war.WarManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class WarCommand implements CommandExecutor, TabCompleter {

    private final WarManager warManager;
    private final TownManager townManager;

    public WarCommand(WarManager warManager, TownManager townManager) {
        this.warManager = warManager;
        this.townManager = townManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Bu komut sadece oyuncular içindir!");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "declare" -> handleDeclare(player, args);
            case "status" -> handleStatus(player);
            case "surrender" -> handleSurrender(player);
            case "top" -> handleTop(player);
            case "banner" -> handleBanner(player);
            default -> sendHelp(player);
        }
        return true;
    }

    private void handleDeclare(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendMessage(player, "<red>Kullanım: /war declare <hedef_kasaba></red>");
            return;
        }
        warManager.declareWar(player, args[1]);
    }

    private void handleStatus(Player player) {
        Town town = townManager.getTownByPlayer(player.getUniqueId());
        if (town == null) {
            MessageUtil.sendMessage(player, "<red>Bir kasabanız bulunmuyor!</red>");
            return;
        }

        SiegeWar war = warManager.getWarByTown(town.getUuid());
        if (war == null) {
            if (townManager.isUnderTruce(town.getUuid())) {
                MessageUtil.sendMessage(player, "<green>Kasabanız şu anda 48 saatlik savaş dokunulmazlığı (Truce) altındadır.</green>");
            } else {
                MessageUtil.sendMessage(player, "<yellow>Kasabanız şu anda herhangi bir aktif savaş veya kuşatmada değildir.</yellow>");
            }
            return;
        }

        Town attacker = townManager.getTown(war.getAttackerTownUuid());
        Town defender = townManager.getTown(war.getDefenderTownUuid());

        String status = String.format("<gold>═══════ [ <red><b>KUŞATMA DURUMU</b></red> ] ═══════</gold>\n" +
                "<gray>Aşama: %s</gray>\n" +
                "<gray>Saldıran: <red>%s</red> (Puan: <yellow>%d</yellow>)</gray>\n" +
                "<gray>Savunan: <blue>%s</blue> (Puan: <yellow>%d</yellow>)</gray>\n" +
                "<gold>═════════════════════════════════</gold>",
                war.getState().getFormattedName(),
                attacker != null ? attacker.getName() : "Bilinmiyor", war.getScoreAttacker(),
                defender != null ? defender.getName() : "Bilinmiyor", war.getScoreDefender());

        MessageUtil.sendRawMessage(player, status);
    }

    private void handleSurrender(Player player) {
        Town town = townManager.getTownByPlayer(player.getUniqueId());
        if (town == null) return;

        SiegeWar war = warManager.getWarByTown(town.getUuid());
        if (war == null) {
            MessageUtil.sendMessage(player, "<red>Kasabanız şu anda bir savaşta değildir!</red>");
            return;
        }

        if (!town.getLeaderUuid().equals(player.getUniqueId())) {
            MessageUtil.sendMessage(player, "<red>Sadece belediye başkanı teslim olabilir!</red>");
            return;
        }

        Town winner = war.getAttackerTownUuid().equals(town.getUuid()) ?
                townManager.getTown(war.getDefenderTownUuid()) : townManager.getTown(war.getAttackerTownUuid());

        MessageUtil.broadcast("<yellow><b>" + town.getName() + "</b> kasabası savaştan çekilerek teslim oldu!</yellow>");
        warManager.endWar(war, winner);
    }

    private void handleTop(Player player) {
        MessageUtil.sendMessage(player, "<gold>═══════ [ <yellow><b>EN GÜÇLÜ KASABALAR</b></yellow> ] ═══════</gold>");
        int rank = 1;
        for (Town town : townManager.getAllTowns()) {
            if (rank > 10) break;
            MessageUtil.sendRawMessage(player, String.format("<gray>%d. <yellow>%s</yellow> - Seviye %d (Bakiye: %.0f$)</gray>",
                    rank++, town.getName(), town.getLevel(), town.getBankBalance()));
        }
        MessageUtil.sendMessage(player, "<gold>═════════════════════════════════</gold>");
    }

    private void handleBanner(Player player) {
        handleGiveBanner(player);
    }

    private void handleGiveBanner(Player player) {
        Town town = townManager.getTownByPlayer(player.getUniqueId());
        if (town == null) return;

        TownMember member = townManager.getMember(player.getUniqueId());
        if (member == null || !member.getTownUuid().equals(town.getUuid()) || !member.getRole().canDeclareWar()) {
            MessageUtil.sendMessage(player, "<red>Kuşatma sancağı alma yetkiniz yok!</red>");
            return;
        }

        SiegeWar war = warManager.getWarByTown(town.getUuid());
        if (war == null || !war.getAttackerTownUuid().equals(town.getUuid())) {
            MessageUtil.sendMessage(player, "<red>Aktif bir saldırı savaşınız yok!</red>");
            return;
        }

        if (war.getBannerLocation() != null) {
            MessageUtil.sendMessage(player, "<red>Kuşatma sancağı zaten dikilmiş durumda!</red>");
            return;
        }

        if (!warManager.isPrimeTime()) {
            MessageUtil.sendMessage(player, "<red>Kuşatma sancağı yalnızca Prime-Time saatlerinde alınabilir!</red>");
            return;
        }

        Town defender = townManager.getTown(war.getDefenderTownUuid());
        ItemStack banner = warManager.createSiegeBanner(war.getWarId(), town.getName(), defender != null ? defender.getName() : "Savunan");
        player.getInventory().addItem(banner);
        MessageUtil.sendMessage(player, "<green>Kuşatma sancağı tekrar verildi!</green>");
    }

    private void sendHelp(Player player) {
        String help = "<gold>═══════ [ <red><b>KLAN KUŞATMA KOMUTLARI</b></red> ] ═══════</gold>\n" +
                "<yellow>/war declare <kasaba></yellow> <gray>- Hedef kasabaya savaş ilan eder</gray>\n" +
                "<yellow>/war status</yellow> <gray>- Kuşatmanın puan durumunu gösterir</gray>\n" +
                "<yellow>/war surrender</yellow> <gray>- Savaştan çekilerek teslim olur</gray>\n" +
                "<yellow>/war top</yellow> <gray>- En güçlü kasabaları listeler</gray>\n" +
                "<yellow>/war banner</yellow> <gray>- Kuşatma sancağını tekrar alır</gray>\n" +
                "<gold>═════════════════════════════════════</gold>";
        MessageUtil.sendRawMessage(player, help);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("declare", "status", "surrender", "top", "banner");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("declare")) {
            return townManager.getAllTowns().stream().map(Town::getName).toList();
        }
        return Collections.emptyList();
    }
}
