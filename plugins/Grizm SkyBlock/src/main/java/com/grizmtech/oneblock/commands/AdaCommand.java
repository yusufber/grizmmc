package com.grizmtech.oneblock.commands;

import com.grizmtech.oneblock.GrizmOneBlock;
import com.grizmtech.oneblock.models.IslandSetting;
import com.grizmtech.oneblock.models.WarpCategory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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
import java.util.Map;
import java.util.UUID;

public class AdaCommand implements CommandExecutor, TabCompleter {

    private final GrizmOneBlock plugin;

    public AdaCommand(GrizmOneBlock plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Bu komutu sadece oyuncular kullanabilir!");
            return true;
        }

        if (args.length > 0) {
            String sub = args[0].toLowerCase();
            switch (sub) {
                case "git":
                case "go":
                case "home":
                    plugin.getIslandManager().teleportToIsland(player);
                    return true;

                case "olustur":
                case "create":
                    plugin.getIslandManager().createIsland(player);
                    return true;

                case "gelistir":
                case "yukselt":
                case "upgrade":
                    plugin.getGuiManager().openUpgradeMenu(player);
                    return true;

                case "banka":
                case "bank":
                    plugin.getGuiManager().openBankMenu(player);
                    return true;

                case "coop":
                    if (args.length < 2) {
                        player.sendMessage(Component.text("Kullanım: /ada coop <oyuncu>").color(NamedTextColor.YELLOW));
                        return true;
                    }
                    Player targetCoop = Bukkit.getPlayerExact(args[1]);
                    if (targetCoop == null) {
                        player.sendMessage(Component.text("Oyuncu bulunamadı veya çevrimdışı!").color(NamedTextColor.RED));
                        return true;
                    }
                    if (targetCoop.getUniqueId().equals(player.getUniqueId())) {
                        player.sendMessage(Component.text("Kendinize Co-op veremezsiniz!").color(NamedTextColor.RED));
                        return true;
                    }
                    plugin.getRoleManager().addCoop(player.getUniqueId(), targetCoop.getUniqueId());
                    player.sendMessage(Component.text(targetCoop.getName() + " adlı oyuncuya geçici Co-op (Ortak) yetkisi verildi! (Oyundan çıktığınızda silinir).").color(NamedTextColor.GREEN));
                    targetCoop.sendMessage(Component.text(player.getName() + " adlı oyuncunun adasında geçici Co-op yetkisi kazandınız!").color(NamedTextColor.GOLD));
                    return true;

                case "uncoop":
                    if (args.length < 2) {
                        player.sendMessage(Component.text("Kullanım: /ada uncoop <oyuncu>").color(NamedTextColor.YELLOW));
                        return true;
                    }
                    Player targetUncoop = Bukkit.getPlayerExact(args[1]);
                    if (targetUncoop != null) {
                        plugin.getRoleManager().removeCoop(player.getUniqueId(), targetUncoop.getUniqueId());
                        player.sendMessage(Component.text(targetUncoop.getName() + " adlı oyuncunun Co-op yetkisi kaldırıldı.").color(NamedTextColor.YELLOW));
                    }
                    return true;

                case "gorevler":
                case "missions":
                case "gorev":
                    plugin.getGuiManager().openMissionsMenu(player);
                    return true;

                case "biyom":
                case "biomes":
                    plugin.getGuiManager().openBiomesMenu(player);
                    return true;

                case "prestij":
                case "rebirth":
                    plugin.getRebirthManager().doPrestige(player);
                    return true;

                case "seviye":
                case "level":
                    plugin.getIslandLevelManager().calculateIslandLevelAsync(player, null);
                    return true;

                case "top":
                case "sirala":
                    player.sendMessage(Component.text("=== En İyi 10 Ada (Ada Seviyesi) ===").color(NamedTextColor.GOLD));
                    List<Map.Entry<String, Long>> top = plugin.getIslandLevelManager().getTop10();
                    int rank = 1;
                    for (Map.Entry<String, Long> entry : top) {
                        player.sendMessage(Component.text("#" + (rank++) + " ").color(NamedTextColor.YELLOW)
                                .append(Component.text(entry.getKey()).color(NamedTextColor.WHITE))
                                .append(Component.text(" - ").color(NamedTextColor.GRAY))
                                .append(Component.text(entry.getValue() + " Seviye").color(NamedTextColor.GREEN)));
                    }
                    return true;

                case "ayar":
                case "ayarlar":
                case "settings":
                case "izin":
                case "izinler":
                    plugin.getGuiManager().openSettingsMenu(player);
                    return true;

                case "ziyaret":
                case "visit":
                    if (args.length < 2) {
                        player.sendMessage(Component.text("Kullanım: /ada ziyaret <oyuncu_adı>").color(NamedTextColor.YELLOW));
                        return true;
                    }

                    String targetName = args[1];
                    Player onlineTarget = Bukkit.getPlayerExact(targetName);
                    UUID targetUUID;
                    if (onlineTarget != null) {
                        targetUUID = onlineTarget.getUniqueId();
                    } else {
                        @SuppressWarnings("deprecation")
                        OfflinePlayer offline = Bukkit.getOfflinePlayer(targetName);
                        targetUUID = offline.getUniqueId();
                    }

                    if (targetUUID.equals(player.getUniqueId())) {
                        plugin.getIslandManager().teleportToIsland(player);
                        return true;
                    }

                    if (!plugin.getDataManager().hasIsland(targetUUID)) {
                        player.sendMessage(Component.text("Belirtilen oyuncunun henüz bir adası bulunmuyor!").color(NamedTextColor.RED));
                        return true;
                    }

                    // Ada Kilidi kontrolü
                    boolean isLocked = plugin.getIslandSettingManager().getBoolean(targetUUID, IslandSetting.VISITOR_LOCK);
                    if (isLocked && !player.hasPermission("grizmoneblock.admin")) {
                        player.sendMessage(Component.text("Bu ada sahibi tarafından kilitlenmiştir! Ziyaretçi kabul edilmiyor.").color(NamedTextColor.RED));
                        return true;
                    }

                    Location targetIslandLoc = plugin.getDataManager().getOneBlockLocation(targetUUID);
                    if (targetIslandLoc != null && targetIslandLoc.getWorld() != null) {
                        Location tpLoc = targetIslandLoc.clone().add(0.5, 1, 0.5);
                        player.teleport(tpLoc);
                        player.sendMessage(Component.text(targetName + " adlı oyuncunun adasına ışınlandınız!").color(NamedTextColor.GREEN));
                    }
                    return true;

                case "setblock":
                    if (!player.hasPermission("grizmoneblock.admin")) {
                        player.sendMessage(Component.text("Bunun için yetkiniz yok!").color(NamedTextColor.RED));
                        return true;
                    }
                    org.bukkit.block.Block targetBlock = player.getTargetBlockExact(5);
                    if (targetBlock != null) {
                        plugin.getDataManager().setOneBlockLocation(player.getUniqueId(), targetBlock.getLocation());
                        plugin.getDataManager().saveConfig();
                        plugin.getHologramManager().updateHologram(player.getUniqueId());
                        player.sendMessage(Component.text("Tek Blok (OneBlock) konumu başarıyla ayarlandı!").color(NamedTextColor.GREEN));
                    }
                    return true;

                case "reload":
                    if (!player.hasPermission("grizmoneblock.admin")) {
                        player.sendMessage(Component.text("Bunun için yetkiniz yok!").color(NamedTextColor.RED));
                        return true;
                    }
                    plugin.getDataManager().setup();
                    player.sendMessage(Component.text("GrizmOneBlock yapılandırması yeniden yüklendi!").color(NamedTextColor.GREEN));
                    return true;

                case "warp":
                case "warps":
                    if (args.length == 1) {
                        plugin.getGuiManager().openWarpsMenu(player, null);
                        return true;
                    }
                    if (args[1].equalsIgnoreCase("olustur") || args[1].equalsIgnoreCase("set")) {
                        WarpCategory category = WarpCategory.MARKET;
                        int descStart = 2;
                        if (args.length >= 3) {
                            try {
                                category = WarpCategory.valueOf(args[2].toUpperCase());
                                descStart = 3;
                            } catch (Exception ignored) {}
                        }
                        StringBuilder desc = new StringBuilder();
                        for (int i = descStart; i < args.length; i++) {
                            desc.append(args[i]).append(" ");
                        }
                        String description = desc.toString().trim();
                        if (description.isEmpty()) description = player.getName() + " Adasına Hoş Geldiniz!";
                        plugin.getIslandSocialManager().setWarp(player, category, description);
                        return true;
                    } else if (args[1].equalsIgnoreCase("sil") || args[1].equalsIgnoreCase("remove")) {
                        plugin.getIslandSocialManager().removeWarp(player.getUniqueId());
                        player.sendMessage(Component.text("Ada warpınız kaldırıldı.").color(NamedTextColor.YELLOW));
                        return true;
                    }
                    plugin.getGuiManager().openWarpsMenu(player, null);
                    return true;

                case "minyon":
                case "miner":
                case "otominer":
                    plugin.getGuiManager().openAutoMinerMenu(player);
                    return true;

                case "beceri":
                case "beceriler":
                case "skills":
                case "skill":
                    plugin.getGuiManager().openSkillsMenu(player);
                    return true;

                case "begen":
                case "like":
                    if (args.length < 2) {
                        player.sendMessage(Component.text("Kullanım: /ada begen <oyuncu>").color(NamedTextColor.YELLOW));
                        return true;
                    }
                    @SuppressWarnings("deprecation")
                    OfflinePlayer targetLike = Bukkit.getOfflinePlayer(args[1]);
                    plugin.getIslandSocialManager().likeIsland(player, targetLike.getUniqueId());
                    return true;

                case "bahsis":
                case "tip":
                    if (args.length < 3) {
                        player.sendMessage(Component.text("Kullanım: /ada bahsis <oyuncu> <miktar>").color(NamedTextColor.YELLOW));
                        return true;
                    }
                    try {
                        double amount = Double.parseDouble(args[2]);
                        @SuppressWarnings("deprecation")
                        OfflinePlayer targetTip = Bukkit.getOfflinePlayer(args[1]);
                        plugin.getIslandSocialManager().tipOwner(player, targetTip.getUniqueId(), amount);
                    } catch (NumberFormatException e) {
                        player.sendMessage(Component.text("Geçersiz miktar!").color(NamedTextColor.RED));
                    }
                    return true;

                case "mesaj":
                case "defter":
                case "guestbook":
                    if (args.length < 3) {
                        player.sendMessage(Component.text("Kullanım: /ada mesaj <oyuncu> <mesaj>").color(NamedTextColor.YELLOW));
                        return true;
                    }
                    @SuppressWarnings("deprecation")
                    OfflinePlayer targetMsg = Bukkit.getOfflinePlayer(args[1]);
                    StringBuilder msgBuilder = new StringBuilder();
                    for (int i = 2; i < args.length; i++) {
                        msgBuilder.append(args[i]).append(" ");
                    }
                    plugin.getIslandSocialManager().leaveMessage(player, targetMsg.getUniqueId(), msgBuilder.toString().trim());
                    return true;

                case "yardim":
                case "help":
                    player.sendMessage(Component.text("--- Grizm OneBlock Komutları ---").color(NamedTextColor.GOLD));
                    player.sendMessage(Component.text("/ada - Ana ada menüsünü açar").color(NamedTextColor.YELLOW));
                    player.sendMessage(Component.text("/ada git - Adanıza ışınlar").color(NamedTextColor.YELLOW));
                    player.sendMessage(Component.text("/ada banka - Ada bankasını açar").color(NamedTextColor.YELLOW));
                    player.sendMessage(Component.text("/ada warp - Ada İlan Panosu & Warplarını açar").color(NamedTextColor.YELLOW));
                    player.sendMessage(Component.text("/ada warp olustur [Kategori] [Açıklama] - Warp yayınlar").color(NamedTextColor.YELLOW));
                    player.sendMessage(Component.text("/ada minyon - Otomatik Kırıcı / Minyon menüsü").color(NamedTextColor.YELLOW));
                    player.sendMessage(Component.text("/ada beceri - Ada Yetenek Ağacı menüsü").color(NamedTextColor.YELLOW));
                    player.sendMessage(Component.text("/ada begen <oyuncu> - Gezdiğiniz adaya beğeni verir").color(NamedTextColor.YELLOW));
                    player.sendMessage(Component.text("/ada bahsis <oyuncu> <miktar> - Ada sahibine bahşiş atar").color(NamedTextColor.YELLOW));
                    player.sendMessage(Component.text("/ada mesaj <oyuncu> <mesaj> - Ziyaretçi defterine yazar").color(NamedTextColor.YELLOW));
                    player.sendMessage(Component.text("/ada coop <oyuncu> - Geçici ortak yetkisi verir").color(NamedTextColor.YELLOW));
                    player.sendMessage(Component.text("/ada seviye - Ada seviyesini asenkron hesaplar").color(NamedTextColor.YELLOW));
                    player.sendMessage(Component.text("/ada top - En yüksek ada seviyelerini listeler").color(NamedTextColor.YELLOW));
                    player.sendMessage(Component.text("/ada gorevler - Aşama görevlerini açar").color(NamedTextColor.YELLOW));
                    player.sendMessage(Component.text("/ada biyom - Ada biyomunu değiştirir").color(NamedTextColor.YELLOW));
                    player.sendMessage(Component.text("/ada prestij - Aşamaları bitirip prestij kazanır").color(NamedTextColor.YELLOW));
                    player.sendMessage(Component.text("/ada ayar - Ada ayarları ve izinler menüsü").color(NamedTextColor.YELLOW));
                    return true;
            }
        }

        plugin.getGuiManager().openAdaMenu(player);
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>(Arrays.asList("git", "olustur", "gelistir", "banka", "coop", "uncoop", "seviye", "top", "gorevler", "biyom", "prestij", "ayar", "ziyaret", "warp", "minyon", "beceri", "begen", "bahsis", "mesaj", "yardim"));
            if (sender.hasPermission("grizmoneblock.admin")) {
                list.add("setblock");
                list.add("reload");
            }
            List<String> filtered = new ArrayList<>();
            for (String s : list) {
                if (s.toLowerCase().startsWith(args[0].toLowerCase())) {
                    filtered.add(s);
                }
            }
            return filtered;
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("ziyaret") || sub.equals("coop") || sub.equals("uncoop") || sub.equals("begen") || sub.equals("bahsis") || sub.equals("mesaj")) {
                List<String> playerNames = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        playerNames.add(p.getName());
                    }
                }
                return playerNames;
            } else if (sub.equals("warp")) {
                List<String> warpSubs = Arrays.asList("olustur", "sil");
                List<String> filtered = new ArrayList<>();
                for (String w : warpSubs) {
                    if (w.startsWith(args[1].toLowerCase())) filtered.add(w);
                }
                return filtered;
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("warp") && args[1].equalsIgnoreCase("olustur")) {
            List<String> categories = new ArrayList<>();
            for (WarpCategory c : WarpCategory.values()) {
                if (c.name().toLowerCase().startsWith(args[2].toLowerCase())) {
                    categories.add(c.name());
                }
            }
            return categories;
        }
        return java.util.Collections.emptyList();
    }
}
