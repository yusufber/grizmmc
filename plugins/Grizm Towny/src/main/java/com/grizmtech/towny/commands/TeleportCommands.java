package com.grizmtech.towny.commands;

import com.grizmtech.towny.core.MessageUtil;
import com.grizmtech.towny.gui.WarpMenuGUI;
import com.grizmtech.towny.teleport.RTPManager;
import com.grizmtech.towny.teleport.TPAManager;
import com.grizmtech.towny.teleport.TPARequest;
import com.grizmtech.towny.teleport.Warp;
import com.grizmtech.towny.teleport.WarpManager;
import com.grizmtech.towny.town.TownManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class TeleportCommands implements CommandExecutor, TabCompleter {

    private final RTPManager rtpManager;
    private final WarpManager warpManager;
    private final TPAManager tpaManager;
    private final TownManager townManager;

    public TeleportCommands(RTPManager rtpManager, WarpManager warpManager, TPAManager tpaManager, TownManager townManager) {
        this.rtpManager = rtpManager;
        this.warpManager = warpManager;
        this.tpaManager = tpaManager;
        this.townManager = townManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Bu komut sadece oyuncular içindir!");
            return true;
        }

        String cmd = command.getName().toLowerCase();

        switch (cmd) {
            case "rtp" -> rtpManager.executeRtp(player);
            case "warp" -> handleWarp(player, args);
            case "setwarp" -> handleSetWarp(player, args);
            case "delwarp" -> handleDelWarp(player, args);
            case "tpa" -> handleTpa(player, args, TPARequest.Type.TPA);
            case "tpahere" -> handleTpa(player, args, TPARequest.Type.TPAHERE);
            case "tpaccept" -> tpaManager.acceptRequest(player);
            case "tpdeny" -> tpaManager.denyRequest(player);
        }

        return true;
    }

    private void handleWarp(Player player, String[] args) {
        if (args.length == 0) {
            WarpMenuGUI.open(player, warpManager, townManager);
            return;
        }
        Warp warp = warpManager.getWarp(args[0]);
        if (warp == null) {
            MessageUtil.sendMessage(player, "<red>Böyle bir warp noktası bulunamadı!</red>");
            return;
        }
        warpManager.teleportToWarp(player, warp);
    }

    private void handleSetWarp(Player player, String[] args) {
        if (!player.hasPermission("grizmtowny.admin")) {
            MessageUtil.sendMessage(player, "<red>Bu komutu kullanma yetkiniz yok!</red>");
            return;
        }
        if (args.length < 1) {
            MessageUtil.sendMessage(player, "<red>Kullanım: /setwarp <isim> [ucret]</red>");
            return;
        }
        double cost = args.length >= 2 ? Double.parseDouble(args[1]) : 0.0;
        warpManager.setServerWarp(player, args[0], cost, null);
    }

    private void handleDelWarp(Player player, String[] args) {
        if (!player.hasPermission("grizmtowny.admin")) {
            MessageUtil.sendMessage(player, "<red>Bu komutu kullanma yetkiniz yok!</red>");
            return;
        }
        if (args.length < 1) {
            MessageUtil.sendMessage(player, "<red>Kullanım: /delwarp <isim></red>");
            return;
        }
        warpManager.deleteWarp(player, args[0]);
    }

    private void handleTpa(Player player, String[] args, TPARequest.Type type) {
        if (args.length < 1) {
            MessageUtil.sendMessage(player, "<red>Kullanım: /" + (type == TPARequest.Type.TPA ? "tpa" : "tpahere") + " <oyuncu></red>");
            return;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            MessageUtil.sendMessage(player, "<red>Hedef oyuncu bulunamadı veya çevrimdışı!</red>");
            return;
        }
        tpaManager.sendRequest(player, target, type);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String cmd = command.getName().toLowerCase();
        if (cmd.equals("warp") || cmd.equals("delwarp")) {
            if (args.length == 1) {
                return warpManager.getAllWarps().stream().map(Warp::getWarpId).toList();
            }
        } else if (cmd.equals("tpa") || cmd.equals("tpahere")) {
            if (args.length == 1) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            }
        }
        return Collections.emptyList();
    }
}
