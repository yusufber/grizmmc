package com.grizmtech.towny.listeners;

import com.grizmtech.towny.core.MessageUtil;
import com.grizmtech.towny.war.SiegeWar;
import com.grizmtech.towny.war.WarManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public class SiegeBannerListener implements Listener {

    private final WarManager warManager;

    public SiegeBannerListener(WarManager warManager) {
        this.warManager = warManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBannerPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (!warManager.isSiegeBannerItem(item)) return;

        Player player = event.getPlayer();
        Integer warId = warManager.getWarIdFromBanner(item);
        if (warId == null) {
            event.setCancelled(true);
            return;
        }

        boolean success = warManager.placeSiegeBanner(player, event.getBlockPlaced().getLocation(), warId);
        if (!success) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBannerBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.RED_BANNER && block.getType() != Material.RED_WALL_BANNER) return;

        Player player = event.getPlayer();
        Location loc = block.getLocation();

        if (warManager.isSiegeBannerLocation(loc)) {
            if (!player.hasPermission("grizmtowny.admin")) {
                event.setCancelled(true);
                MessageUtil.sendMessage(player, "<red>Aktif kuşatma sancağı kırılamaz!</red>");
            }
        }
    }
}
