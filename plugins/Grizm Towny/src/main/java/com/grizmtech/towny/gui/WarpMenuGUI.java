package com.grizmtech.towny.gui;

import com.grizmtech.towny.teleport.Warp;
import com.grizmtech.towny.teleport.WarpManager;
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

public class WarpMenuGUI {

    public static final String TITLE = "§8Warp & Işınlanma Rehberi";

    public static void open(Player player, WarpManager warpManager, TownManager townManager) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text(TITLE));

        // Dekoratif camlar
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 9; i++) inv.setItem(i, filler);
        for (int i = 45; i < 54; i++) inv.setItem(i, filler);

        int slot = 9;

        // 1. Sunucu Warpları
        for (Warp warp : warpManager.getServerWarps()) {
            if (slot >= 45) break;
            List<String> lore = List.of(
                    "§7Tür: §bGenel Sunucu Noktası",
                    "§7Işınlanma Ücreti: §e" + (warp.getVisitCost() > 0 ? warp.getVisitCost() + "$" : "Ücretsiz"),
                    " ",
                    "§e[Tıkla] Işınlan"
            );
            inv.setItem(slot++, createItem(Material.ENDER_PEARL, "§b§l" + warp.getWarpId().toUpperCase(), lore));
        }

        // 2. Oyuncunun Kasaba Warpları
        Town town = townManager.getTownByPlayer(player.getUniqueId());
        if (town != null) {
            for (Warp warp : warpManager.getTownWarps(town.getUuid())) {
                if (slot >= 45) break;
                List<String> lore = List.of(
                        "§7Tür: §6" + town.getName() + " Kasaba Noktası",
                        "§7Ziyaret Ücreti: §e" + warp.getVisitCost() + "$",
                        " ",
                        "§e[Tıkla] Işınlan"
                );
                inv.setItem(slot++, createItem(Material.COMPASS, "§6§l" + warp.getWarpId(), lore));
            }
        }

        player.openInventory(inv);
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
