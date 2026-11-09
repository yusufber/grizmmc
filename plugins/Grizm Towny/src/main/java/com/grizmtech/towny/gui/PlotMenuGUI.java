package com.grizmtech.towny.gui;

import com.grizmtech.towny.claim.ClaimChunk;
import com.grizmtech.towny.claim.ClaimManager;
import com.grizmtech.towny.claim.PlotFlag;
import com.grizmtech.towny.claim.PlotGroup;
import com.grizmtech.towny.claim.PlotType;
import com.grizmtech.towny.core.MessageUtil;
import com.grizmtech.towny.town.Town;
import com.grizmtech.towny.town.TownManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class PlotMenuGUI {

    public static final String TITLE = "§8Parsel İzin ve Ayar Paneli";

    public static void open(Player player, ClaimManager claimManager, TownManager townManager) {
        ClaimChunk claim = claimManager.getClaim(player.getLocation());
        if (claim == null) {
            MessageUtil.sendMessage(player, "<red>Şu an sahipli bir parselde değilsiniz!</red>");
            return;
        }

        Town town = townManager.getTown(claim.getTownUuid());
        Town playerTown = townManager.getTownByPlayer(player.getUniqueId());

        if (town == null || playerTown == null || !town.getUuid().equals(playerTown.getUuid())) {
            MessageUtil.sendMessage(player, "<red>Bu parsel sizin kasabanıza ait değil!</red>");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 36, Component.text(TITLE));

        // Dekoratif camlar
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 36; i++) {
            inv.setItem(i, filler);
        }

        // Parsel Tipi (Slot 10)
        List<String> typeLore = List.of(
                "§7Mevcut Tip: " + claim.getPlotType().getDisplayName(),
                "§7Açıklama: §f" + claim.getPlotType().getDescription(),
                " ",
                "§e[Tıkla] Parsel Tipini Değiştir"
        );
        inv.setItem(10, createItem(Material.OAK_SIGN, "§e§lParsel Tipi", typeLore));

        // RESIDENT Bayrakları (Slot 12)
        List<String> resLore = getGroupFlagLore(claim, PlotGroup.RESIDENT);
        inv.setItem(12, createItem(Material.LIME_CONCRETE, "§a§lVatandaş İzinleri", resLore));

        // ALLY Bayrakları (Slot 14)
        List<String> allyLore = getGroupFlagLore(claim, PlotGroup.ALLY);
        inv.setItem(14, createItem(Material.LIGHT_BLUE_CONCRETE, "§b§lMüttefik İzinleri", allyLore));

        // OUTSIDER Bayrakları (Slot 16)
        List<String> outLore = getGroupFlagLore(claim, PlotGroup.OUTSIDER);
        inv.setItem(16, createItem(Material.RED_CONCRETE, "§c§lYabancı İzinleri", outLore));

        // Satış Durumu (Slot 31)
        List<String> saleLore = List.of(
                "§7Parsel Sahibi: §e" + (claim.getPlotOwnerUuid() != null ? Bukkit.getOfflinePlayer(claim.getPlotOwnerUuid()).getName() : "Kasaba Ortak Alanı"),
                "§7Satış Fiyatı: §6" + (claim.isForSale() ? claim.getPrice() + "$" : "Satılık Değil"),
                " ",
                "§e[Sol Tık] Parseli Satın Al (Satılıksa)",
                "§6[Sağ Tık] 500$ Fiyatla Satışa Çıkar (Yetkili)"
        );
        inv.setItem(31, createItem(Material.EMERALD, "§6§lParsel Mülkiyeti & Satış", saleLore));

        player.openInventory(inv);
    }

    private static List<String> getGroupFlagLore(ClaimChunk claim, PlotGroup group) {
        List<String> list = new ArrayList<>();
        list.add("§7Bu grup için geçerli izinler:");
        list.add(" ");
        for (PlotFlag flag : PlotFlag.values()) {
            boolean allowed = claim.hasFlag(group, flag);
            list.add(String.format("§8• §7%s: %s", flag.getDisplayName(), allowed ? "§aAçık" : "§cKapalı"));
        }
        list.add(" ");
        list.add("§e[Tıkla] İzinleri Sırayla Değiştir");
        return list;
    }

    private static ItemStack createItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name));
            if (lore != null) {
                List<Component> list = new ArrayList<>();
                for (String l : lore) {
                    list.add(Component.text(l));
                }
                meta.lore(list);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
