package com.grizmtech.towny.listeners;

import com.grizmtech.towny.claim.ClaimChunk;
import com.grizmtech.towny.claim.ClaimManager;
import com.grizmtech.towny.claim.PlotFlag;
import com.grizmtech.towny.claim.PlotGroup;
import com.grizmtech.towny.claim.PlotType;
import com.grizmtech.towny.core.MessageUtil;
import com.grizmtech.towny.gui.PlotMenuGUI;
import com.grizmtech.towny.gui.TownMenuGUI;
import com.grizmtech.towny.gui.WarpMenuGUI;
import com.grizmtech.towny.hooks.VaultHook;
import com.grizmtech.towny.storage.DatabaseManager;
import com.grizmtech.towny.teleport.Warp;
import com.grizmtech.towny.teleport.WarpManager;
import com.grizmtech.towny.town.Town;
import com.grizmtech.towny.town.TownManager;
import com.grizmtech.towny.town.TownMember;
import com.grizmtech.towny.war.WarManager;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class InventoryClickListener implements Listener {

    private final TownManager townManager;
    private final ClaimManager claimManager;
    private final WarManager warManager;
    private final WarpManager warpManager;
    private final DatabaseManager databaseManager;
    private final VaultHook vaultHook;

    public InventoryClickListener(TownManager townManager, ClaimManager claimManager, WarManager warManager,
                                  WarpManager warpManager, DatabaseManager databaseManager, VaultHook vaultHook) {
        this.townManager = townManager;
        this.claimManager = claimManager;
        this.warManager = warManager;
        this.warpManager = warpManager;
        this.databaseManager = databaseManager;
        this.vaultHook = vaultHook;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!title.equals(TownMenuGUI.TITLE) && !title.equals(PlotMenuGUI.TITLE) && !title.equals(WarpMenuGUI.TITLE)) {
            return;
        }

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        int slot = event.getRawSlot();
        ClickType click = event.getClick();

        if (title.equals(TownMenuGUI.TITLE)) {
            handleTownMenu(player, slot, click);
        } else if (title.equals(PlotMenuGUI.TITLE)) {
            handlePlotMenu(player, slot, click);
        } else if (title.equals(WarpMenuGUI.TITLE)) {
            handleWarpMenu(player, clicked);
        }
    }

    private void handleTownMenu(Player player, int slot, ClickType click) {
        Town town = townManager.getTownByPlayer(player.getUniqueId());
        if (town == null) return;

        if (slot == 13) {
            // Banka
            if (click == ClickType.LEFT) {
                townManager.deposit(town, player, 100.0);
            } else if (click == ClickType.SHIFT_LEFT) {
                townManager.deposit(town, player, 1000.0);
            } else if (click == ClickType.RIGHT) {
                townManager.withdraw(town, player, 100.0);
            } else if (click == ClickType.SHIFT_RIGHT) {
                townManager.withdraw(town, player, 1000.0);
            }
            TownMenuGUI.open(player, townManager, claimManager, warManager);
        } else if (slot == 29) {
            // Parsel Menüsü
            player.closeInventory();
            PlotMenuGUI.open(player, claimManager, townManager);
        } else if (slot == 31) {
            // Seviye Yükselt
            townManager.levelUp(town, player);
            TownMenuGUI.open(player, townManager, claimManager, warManager);
        }
    }

    private void handlePlotMenu(Player player, int slot, ClickType click) {
        ClaimChunk claim = claimManager.getClaim(player.getLocation());
        if (claim == null) {
            player.closeInventory();
            return;
        }

        TownMember member = townManager.getMember(player.getUniqueId());
        if (member == null || !claim.getTownUuid().equals(member.getTownUuid())) {
            player.closeInventory();
            return;
        }

        if (slot == 10) {
            // Tip Değiştir
            if (!member.getRole().canManagePlots()) {
                MessageUtil.sendMessage(player, "<red>Parsel tipini değiştirme yetkiniz yok!</red>");
                return;
            }
            PlotType[] types = PlotType.values();
            int nextIdx = (claim.getPlotType().ordinal() + 1) % types.length;
            claim.setPlotType(types[nextIdx]);
            databaseManager.saveClaimAsync(claim);
            MessageUtil.sendMessage(player, "<green>Parsel tipi güncellendi: " + claim.getPlotType().getFormattedName() + "</green>");
            PlotMenuGUI.open(player, claimManager, townManager);

        } else if (slot == 12) {
            // RESIDENT Bayrakları
            toggleGroupFlags(player, claim, PlotGroup.RESIDENT, member);
        } else if (slot == 14) {
            // ALLY Bayrakları
            toggleGroupFlags(player, claim, PlotGroup.ALLY, member);
        } else if (slot == 16) {
            // OUTSIDER Bayrakları
            toggleGroupFlags(player, claim, PlotGroup.OUTSIDER, member);
        } else if (slot == 31) {
            // Satış / Satın Alma
            if (click == ClickType.LEFT && claim.isForSale()) {
                // Satın al
                if (vaultHook.takeMoney(player, claim.getPrice())) {
                    claim.setPlotOwnerUuid(player.getUniqueId());
                    claim.setPrice(0.0);
                    databaseManager.saveClaimAsync(claim);
                    MessageUtil.sendMessage(player, "<green>Bu parseli başarıyla satın aldınız!</green>");
                } else {
                    MessageUtil.sendMessage(player, "<red>Yeterli paranız yok!</red>");
                }
            } else if (click == ClickType.RIGHT && member.getRole().canManagePlots()) {
                // 500$ fiyatla satışa çıkar
                claim.setPrice(500.0);
                databaseManager.saveClaimAsync(claim);
                MessageUtil.sendMessage(player, "<yellow>Parsel 500$ fiyatla satışa çıkarıldı.</yellow>");
            }
            PlotMenuGUI.open(player, claimManager, townManager);
        }
    }

    private void toggleGroupFlags(Player player, ClaimChunk claim, PlotGroup group, TownMember member) {
        if (!member.getRole().canManagePlots()) {
            MessageUtil.sendMessage(player, "<red>Parsel izinlerini düzenleme yetkiniz yok!</red>");
            return;
        }

        // Sırayla ilk kapalı bayrağı aç veya hepsini döngüye al
        for (PlotFlag flag : PlotFlag.values()) {
            claim.toggleFlag(group, flag);
            break; // Birer birer değiştir
        }
        databaseManager.saveClaimAsync(claim);
        PlotMenuGUI.open(player, claimManager, townManager);
    }

    private void handleWarpMenu(Player player, ItemStack clicked) {
        if (clicked.getItemMeta() == null || clicked.getItemMeta().displayName() == null) return;
        String name = PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName()).replace("§b§l", "").replace("§6§l", "").trim();

        Warp warp = warpManager.getWarp(name);
        if (warp != null) {
            player.closeInventory();
            warpManager.teleportToWarp(player, warp);
        }
    }
}
