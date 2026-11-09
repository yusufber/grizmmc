package com.grizmtech.oneblock.listeners;

import com.grizmtech.oneblock.GrizmOneBlock;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.UUID;

public class IslandLimiterListener implements Listener {

    private final GrizmOneBlock plugin;

    public IslandLimiterListener(GrizmOneBlock plugin) {
        this.plugin = plugin;
    }

    private UUID getOwner(String worldName) {
        if (worldName.startsWith("island_")) {
            try {
                return UUID.fromString(worldName.replace("island_", ""));
            } catch (Exception ignored) {}
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        String worldName = event.getBlock().getWorld().getName();
        UUID owner = getOwner(worldName);
        if (owner == null) return;

        Material type = event.getBlock().getType();
        Player player = event.getPlayer();

        if (type == Material.HOPPER) {
            int level = plugin.getDataManager().getHopperLevel(owner);
            int limit = plugin.getUpgradeManager().getHopperLimit(level);
            int placed = plugin.getDataManager().getConfig().getInt("players." + owner + ".limits.hoppers", 0);

            if (placed >= limit) {
                event.setCancelled(true);
                player.sendMessage(Component.text("Bu adadaki huni sınırına ulaşıldı! (" + limit + " Huni)").color(NamedTextColor.RED));
            } else {
                plugin.getDataManager().getConfig().set("players." + owner + ".limits.hoppers", placed + 1);
                plugin.getDataManager().saveConfig();
            }
        } else if (type == Material.PISTON || type == Material.STICKY_PISTON) {
            int level = plugin.getDataManager().getConfig().getInt("players." + owner + ".piston_level", 1);
            int limit = plugin.getUpgradeManager().getPistonLimit(level);
            int placed = plugin.getDataManager().getConfig().getInt("players." + owner + ".limits.pistons", 0);

            if (placed >= limit) {
                event.setCancelled(true);
                player.sendMessage(Component.text("Bu adadaki piston sınırına ulaşıldı! (" + limit + " Piston)").color(NamedTextColor.RED));
            } else {
                plugin.getDataManager().getConfig().set("players." + owner + ".limits.pistons", placed + 1);
                plugin.getDataManager().saveConfig();
            }
        } else if (type == Material.SPAWNER) {
            int level = plugin.getDataManager().getConfig().getInt("players." + owner + ".spawner_level", 1);
            int limit = plugin.getUpgradeManager().getSpawnerLimit(level);
            int placed = plugin.getDataManager().getConfig().getInt("players." + owner + ".limits.spawners", 0);

            if (placed >= limit) {
                event.setCancelled(true);
                player.sendMessage(Component.text("Bu adadaki spawner sınırına ulaşıldı! (" + limit + " Spawner)").color(NamedTextColor.RED));
            } else {
                plugin.getDataManager().getConfig().set("players." + owner + ".limits.spawners", placed + 1);
                plugin.getDataManager().saveConfig();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        String worldName = event.getBlock().getWorld().getName();
        UUID owner = getOwner(worldName);
        if (owner == null) return;

        Material type = event.getBlock().getType();
        if (type == Material.HOPPER) {
            decrementLimit(owner, "hoppers");
        } else if (type == Material.PISTON || type == Material.STICKY_PISTON) {
            decrementLimit(owner, "pistons");
        } else if (type == Material.SPAWNER) {
            decrementLimit(owner, "spawners");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVillagerSpawn(CreatureSpawnEvent event) {
        if (event.getEntityType() != EntityType.VILLAGER) return;
        String worldName = event.getLocation().getWorld().getName();
        UUID owner = getOwner(worldName);
        if (owner == null) return;

        int level = plugin.getDataManager().getConfig().getInt("players." + owner + ".villager_level", 1);
        int limit = plugin.getUpgradeManager().getVillagerLimit(level);

        int currentVillagers = 0;
        for (org.bukkit.entity.Entity e : event.getLocation().getWorld().getEntities()) {
            if (e instanceof Villager) {
                currentVillagers++;
            }
        }

        if (currentVillagers >= limit) {
            event.setCancelled(true);
        }
    }

    private void decrementLimit(UUID owner, String key) {
        int current = plugin.getDataManager().getConfig().getInt("players." + owner + ".limits." + key, 0);
        plugin.getDataManager().getConfig().set("players." + owner + ".limits." + key, Math.max(0, current - 1));
        plugin.getDataManager().saveConfig();
    }
}
