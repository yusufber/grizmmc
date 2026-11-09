package com.grizmtech.oneblock.listeners;

import com.grizmtech.oneblock.GrizmOneBlock;
import com.grizmtech.oneblock.models.IslandSetting;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.UUID;

public class IslandWorldRulesListener implements Listener {

    private final GrizmOneBlock plugin;

    public IslandWorldRulesListener(GrizmOneBlock plugin) {
        this.plugin = plugin;
    }

    private UUID getIslandOwner(World world) {
        if (world == null) return null;
        String name = world.getName();
        if (name.startsWith("island_")) {
            try {
                return UUID.fromString(name.replace("island_", ""));
            } catch (Exception ignored) {}
        }
        return null;
    }

    // 1. Ziyaretçi Girişi & Ada Kilidi (VISITOR_LOCK)
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Location to = event.getTo();
        if (to.getWorld() == null) return;

        UUID ownerUUID = getIslandOwner(to.getWorld());
        if (ownerUUID == null) return;

        Player player = event.getPlayer();
        if (player.hasPermission("grizmoneblock.admin")) return;
        if (player.getUniqueId().equals(ownerUUID)) return;

        boolean isLocked = plugin.getIslandSettingManager().getBoolean(ownerUUID, IslandSetting.VISITOR_LOCK);
        if (isLocked) {
            event.setCancelled(true);
            player.sendMessage(Component.text("Bu ada sahibi tarafından kilitlenmiştir! Ziyaretçi kabul edilmiyor.").color(NamedTextColor.RED));
        }
    }

    // 2. Ada Saati ve Hava Durumu (TIME_WEATHER) Atmosfer Güncellemesi
    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        plugin.getIslandSettingManager().applyPlayerAtmosphere(player, player.getWorld());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getIslandSettingManager().applyPlayerAtmosphere(player, player.getWorld());
    }

    // 3. Doğal Yaratık Doğumu (NATURAL_MOB_SPAWN)
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        UUID ownerUUID = getIslandOwner(event.getLocation().getWorld());
        if (ownerUUID == null) return;

        // Sadece doğal doğan yaratıkları filtrele (Spawner, yumurta, komut veya tek blok hariç)
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL) {
            boolean allowNaturalSpawn = plugin.getIslandSettingManager().getBoolean(ownerUUID, IslandSetting.NATURAL_MOB_SPAWN);
            if (!allowNaturalSpawn) {
                event.setCancelled(true);
            }
        }
    }

    // 4. Patlama ve Mob Hasarı (MOB_GRIEFING)
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        UUID ownerUUID = getIslandOwner(event.getLocation().getWorld());
        if (ownerUUID == null) return;

        boolean griefing = plugin.getIslandSettingManager().getBoolean(ownerUUID, IslandSetting.MOB_GRIEFING);
        if (!griefing) {
            // Blokların patlayıp yok olmasını engelle
            event.blockList().clear();
            event.setYield(0);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        UUID ownerUUID = getIslandOwner(event.getBlock().getWorld());
        if (ownerUUID == null) return;

        boolean griefing = plugin.getIslandSettingManager().getBoolean(ownerUUID, IslandSetting.MOB_GRIEFING);
        if (!griefing) {
            event.blockList().clear();
            event.setYield(0);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        UUID ownerUUID = getIslandOwner(event.getBlock().getWorld());
        if (ownerUUID == null) return;

        boolean griefing = plugin.getIslandSettingManager().getBoolean(ownerUUID, IslandSetting.MOB_GRIEFING);
        if (!griefing) {
            // Enderman blok çalması, Wither blok kırması, Silverfish vb. engelle
            event.setCancelled(true);
        }
    }

    // 5. Ateş Yayılımı ve Lav Tehlikesi (FIRE_SPREAD)
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent event) {
        UUID ownerUUID = getIslandOwner(event.getBlock().getWorld());
        if (ownerUUID == null) return;

        Material type = event.getSource().getType();
        if (type == Material.FIRE || type == Material.SOUL_FIRE) {
            boolean fireSpread = plugin.getIslandSettingManager().getBoolean(ownerUUID, IslandSetting.FIRE_SPREAD);
            if (!fireSpread) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        UUID ownerUUID = getIslandOwner(event.getBlock().getWorld());
        if (ownerUUID == null) return;

        boolean fireSpread = plugin.getIslandSettingManager().getBoolean(ownerUUID, IslandSetting.FIRE_SPREAD);
        if (!fireSpread) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        UUID ownerUUID = getIslandOwner(event.getBlock().getWorld());
        if (ownerUUID == null) return;

        BlockIgniteEvent.IgniteCause cause = event.getCause();
        if (cause == BlockIgniteEvent.IgniteCause.SPREAD || cause == BlockIgniteEvent.IgniteCause.LAVA || cause == BlockIgniteEvent.IgniteCause.LIGHTNING) {
            boolean fireSpread = plugin.getIslandSettingManager().getBoolean(ownerUUID, IslandSetting.FIRE_SPREAD);
            if (!fireSpread) {
                event.setCancelled(true);
            }
        }
    }
}
