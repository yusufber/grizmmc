package com.grizmtech.oneblock.listeners;

import com.grizmtech.oneblock.GrizmOneBlock;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.UUID;

public class HopperLimitListener implements Listener {

    private final GrizmOneBlock plugin;

    public HopperLimitListener(GrizmOneBlock plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHopperPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() == Material.HOPPER) {
            Player player = event.getPlayer();
            String worldName = event.getBlock().getWorld().getName();

            // Sadece ada dünyalarında sınır uygula
            if (worldName.startsWith("island_")) {
                try {
                    UUID ownerUUID = UUID.fromString(worldName.replace("island_", ""));
                    int currentLevel = plugin.getDataManager().getHopperLevel(ownerUUID);
                    int limit = plugin.getUpgradeManager().getHopperLimit(currentLevel);
                    int placed = plugin.getDataManager().getPlacedHoppers(ownerUUID);

                    if (placed >= limit) {
                        event.setCancelled(true);
                        player.sendMessage(Component.text("Bu adadaki huni sınırına ulaşıldı! (Maks: " + limit + "). Lütfen '/ada' menüsünden geliştirin.").color(NamedTextColor.RED));
                    } else {
                        plugin.getDataManager().setPlacedHoppers(ownerUUID, placed + 1);
                        plugin.getDataManager().saveConfig();
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHopperBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.HOPPER) {
            decrementHoppers(event.getBlock().getWorld().getName(), 1);
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(org.bukkit.event.entity.EntityExplodeEvent event) {
        handleExplodedHoppers(event.blockList());
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(org.bukkit.event.block.BlockExplodeEvent event) {
        handleExplodedHoppers(event.blockList());
    }

    private void handleExplodedHoppers(java.util.List<org.bukkit.block.Block> blocks) {
        java.util.Map<String, Integer> countByWorld = new java.util.HashMap<>();
        for (org.bukkit.block.Block b : blocks) {
            if (b.getType() == Material.HOPPER) {
                String worldName = b.getWorld().getName();
                countByWorld.put(worldName, countByWorld.getOrDefault(worldName, 0) + 1);
            }
        }
        for (java.util.Map.Entry<String, Integer> entry : countByWorld.entrySet()) {
            decrementHoppers(entry.getKey(), entry.getValue());
        }
    }

    private void decrementHoppers(String worldName, int count) {
        if (worldName.startsWith("island_")) {
            try {
                UUID ownerUUID = UUID.fromString(worldName.replace("island_", ""));
                int placed = plugin.getDataManager().getPlacedHoppers(ownerUUID);
                int updated = Math.max(0, placed - count);
                plugin.getDataManager().setPlacedHoppers(ownerUUID, updated);
                plugin.getDataManager().saveConfig();
            } catch (Exception ignored) {}
        }
    }
}
