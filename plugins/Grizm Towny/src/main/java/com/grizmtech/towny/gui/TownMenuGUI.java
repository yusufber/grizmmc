package com.grizmtech.towny.gui;

import com.grizmtech.towny.claim.ClaimManager;
import com.grizmtech.towny.core.MessageUtil;
import com.grizmtech.towny.town.Town;
import com.grizmtech.towny.town.TownManager;
import com.grizmtech.towny.town.TownMember;
import com.grizmtech.towny.war.SiegeWar;
import com.grizmtech.towny.war.WarManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class TownMenuGUI {

    public static final String TITLE = "§8Kasaba Yönetim Paneli";

    public static void open(Player player, TownManager townManager, ClaimManager claimManager, WarManager warManager) {
        Town town = townManager.getTownByPlayer(player.getUniqueId());
        if (town == null) {
            MessageUtil.sendMessage(player, "<red>Bir kasabaya üye değilsiniz!</red>");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 45, Component.text(TITLE));

        // Dekoratif camlar
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 45; i++) {
            inv.setItem(i, filler);
        }

        // 1. Kasaba Bilgisi (Slot 11)
        List<String> infoLore = List.of(
                "§7Belediye Başkanı: §e" + (Bukkit.getOfflinePlayer(town.getLeaderUuid()).getName()),
                "§7Kasaba Seviyesi: §6" + town.getLevel(),
                "§7Toplam Vatandaş: §a" + townManager.getTownMembers(town.getUuid()).size(),
                "§7Toplam Arazi: §b" + claimManager.getTownClaimCount(town.getUuid()) + " chunk",
                "§7Giriş Vergisi: §d" + town.getSpawnCost() + "$",
                town.getUpkeepWarningTime() > 0 ? "§c⚠ Harabe Modunda!" : "§a✔ Kasaba Düzenli"
        );
        inv.setItem(11, createItem(Material.BEACON, "§6§l" + town.getName() + " Kasabası", infoLore));

        // 2. Kasaba Bankası & Hazine (Slot 13)
        List<String> bankLore = List.of(
                "§7Mevcut Bakiye: §e" + town.getBankBalance() + "$",
                " ",
                "§e[Sol Tık] §a100$ Yatır",
                "§e[Shift + Sol Tık] §a1.000$ Yatır",
                "§6[Sağ Tık] §c100$ Çek (Yetkili)",
                "§6[Shift + Sağ Tık] §c1.000$ Çek (Yetkili)"
        );
        inv.setItem(13, createItem(Material.GOLD_INGOT, "§6§lKasaba Bankası", bankLore));

        // 3. Üye & Vatandaş Listesi (Slot 15)
        List<String> membersLore = new ArrayList<>();
        membersLore.add("§7Vatandaşları görüntüleyin ve");
        membersLore.add("§7rütbe yönetimini sağlayın.");
        membersLore.add(" ");
        int count = 0;
        for (TownMember m : townManager.getTownMembers(town.getUuid())) {
            if (count++ >= 8) {
                membersLore.add("§7... ve " + (townManager.getTownMembers(town.getUuid()).size() - 8) + " daha");
                break;
            }
            String pName = Bukkit.getOfflinePlayer(m.getPlayerUuid()).getName();
            membersLore.add("§8• §e" + (pName != null ? pName : "Bilinmiyor") + " §8- " + m.getRole().getDisplayName());
        }
        inv.setItem(15, createItem(Material.PLAYER_HEAD, "§e§lVatandaş Yönetimi", membersLore));

        // 4. Parsel & Toprak Yönetimi (Slot 29)
        List<String> plotLore = List.of(
                "§7Bulunduğunuz chunk'ın izinlerini",
                "§7ve satış durumunu düzenleyin.",
                " ",
                "§eTıklayarak Parsel Menüsünü Aç"
        );
        inv.setItem(29, createItem(Material.GRASS_BLOCK, "§a§lParsel & Toprak Ayarları", plotLore));

        // 5. Seviye Yükseltme (Slot 31)
        double upgradeCost = town.getLevel() * 5000.0;
        List<String> upgradeLore = List.of(
                "§7Mevcut Seviye: §6" + town.getLevel(),
                "§7Sonraki Seviye: §a" + (town.getLevel() + 1),
                "§7Gereken Hazine: §e" + upgradeCost + "$",
                " ",
                "§7Her seviye +8 Arazi ve +5 Üye kapasitesi kazandırır.",
                " ",
                "§e[Tıkla] Seviyeyi Yükselt"
        );
        inv.setItem(31, createItem(Material.NETHER_STAR, "§b§lKasaba Seviyesini Yükselt", upgradeLore));

        // 6. Savaş & Diplomasi Durumu (Slot 33)
        SiegeWar war = warManager.getWarByTown(town.getUuid());
        List<String> warLore = new ArrayList<>();
        if (war != null) {
            warLore.add("§c⚔ Durum: " + war.getState().getDisplayName());
            warLore.add("§7Saldıran Puanı: §c" + war.getScoreAttacker());
            warLore.add("§7Savunan Puanı: §9" + war.getScoreDefender());
        } else if (townManager.isUnderTruce(town.getUuid())) {
            warLore.add("§a🛡 Savaş Dokunulmazlığı (Truce) Aktif");
        } else {
            warLore.add("§aBarış Dönemi - Aktif Savaş Yok");
        }
        inv.setItem(33, createItem(Material.DIAMOND_SWORD, "§c§lDiplomasi ve Savaş Durumu", warLore));

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
