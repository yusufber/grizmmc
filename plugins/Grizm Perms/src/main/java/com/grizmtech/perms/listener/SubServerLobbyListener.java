package com.grizmtech.perms.listener;

import com.grizmtech.perms.GrizmPermsPlugin;
import com.grizmtech.perms.engine.PermissionManager;
import com.grizmtech.perms.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import java.io.File;
import java.util.Locale;

/**
 * Ana Lobi haricindeki tüm alt sunucularda (SkyBlock, Towny/SMP, PvP)
 * "lobby" (Middle Ages small lobby) dünyasını otomatik yükler,
 * başlangıç/spawn/respawn/void noktalarını yönetir ve lobi dünyasını korur.
 */
public class SubServerLobbyListener implements Listener {

    private static final String LOBBY_WORLD_NAME = "lobby";
    private static final double SPAWN_X = 0.5;
    private static final double SPAWN_Y = 0.0;
    private static final double SPAWN_Z = 0.5;
    private static final float SPAWN_YAW = 0.0f;
    private static final float SPAWN_PITCH = 0.0f;

    private final GrizmPermsPlugin plugin;
    private final PermissionManager manager;

    public SubServerLobbyListener(GrizmPermsPlugin plugin, PermissionManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    private boolean isSubServer() {
        return !manager.getServerId().equalsIgnoreCase("lobby");
    }

    public World getOrLoadLobbyWorld() {
        if (!isSubServer()) {
            return null;
        }
        World existing = Bukkit.getWorld(LOBBY_WORLD_NAME);
        if (existing != null) {
            return existing;
        }
        File lobbyFolder = new File(Bukkit.getWorldContainer(), LOBBY_WORLD_NAME);
        File levelDat = new File(lobbyFolder, "level.dat");
        if (!levelDat.exists()) {
            return null;
        }
        try {
            plugin.getLogger().info("[GrizmPerms] Alt sunucu lobi dünyası ('lobby' - Middle Ages small lobby) yükleniyor...");
            World created = Bukkit.createWorld(new WorldCreator(LOBBY_WORLD_NAME));
            if (created != null) {
                configureLobbyWorld(created);
                plugin.getLogger().info("[GrizmPerms] 'lobby' dünyası başarıyla yüklendi ve koruma altına alındı.");
            }
            return created;
        } catch (Throwable t) {
            plugin.getLogger().warning("[GrizmPerms] 'lobby' dünyası yüklenirken hata: " + t.getMessage());
            return null;
        }
    }

    public void configureLobbyWorld(World world) {
        if (world == null) return;
        try {
            world.setSpawnLocation(new Location(world, SPAWN_X, SPAWN_Y, SPAWN_Z, SPAWN_YAW, SPAWN_PITCH));
            world.setPVP(false);
            world.setDifficulty(Difficulty.PEACEFUL);
            world.setTime(6000L);
            world.setStorm(false);
            world.setThundering(false);
            world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            world.setGameRule(GameRule.FALL_DAMAGE, false);
            world.setGameRule(GameRule.SPAWN_RADIUS, 0);
        } catch (Throwable ignored) {
        }
    }

    public Location getLobbySpawnLocation() {
        World w = getOrLoadLobbyWorld();
        if (w == null) return null;
        return new Location(w, SPAWN_X, SPAWN_Y, SPAWN_Z, SPAWN_YAW, SPAWN_PITCH);
    }

    private boolean isInLobbyWorld(World world) {
        return isSubServer() && world != null && world.getName().equalsIgnoreCase(LOBBY_WORLD_NAME);
    }

    private boolean canBypassProtection(Player player) {
        return player != null && (player.isOp() || player.hasPermission("grizmperms.admin"));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerSpawnLocation(PlayerSpawnLocationEvent event) {
        if (!isSubServer()) return;
        Player player = event.getPlayer();
        Location lobbySpawn = getLobbySpawnLocation();
        if (lobbySpawn == null) return;

        String srvId = manager.getServerId();
        Location current = event.getSpawnLocation();
        String currentWorldName = current.getWorld() != null ? current.getWorld().getName() : "";

        // İlk girişte, ya da PvP sunucusunda, ya da SkyBlock'ta 'world' dünyasında kalmışsa 'lobby' dünyasında başlat
        if (!player.hasPlayedBefore()
                || currentWorldName.equalsIgnoreCase(LOBBY_WORLD_NAME)
                || srvId.equalsIgnoreCase("pvp")
                || (srvId.equalsIgnoreCase("skyblock") && currentWorldName.equalsIgnoreCase("world"))) {
            event.setSpawnLocation(lobbySpawn);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!isSubServer()) return;
        Player player = event.getPlayer();
        Location lobbySpawn = getLobbySpawnLocation();
        if (lobbySpawn == null) return;

        String srvId = manager.getServerId();
        String worldName = player.getWorld().getName();

        if (!player.hasPlayedBefore()
                || worldName.equalsIgnoreCase(LOBBY_WORLD_NAME)
                || srvId.equalsIgnoreCase("pvp")
                || (srvId.equalsIgnoreCase("skyblock") && worldName.equalsIgnoreCase("world"))) {
            player.teleport(lobbySpawn);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!isSubServer()) return;
        if (event.isBedSpawn() || event.isAnchorSpawn()) return;
        Location lobbySpawn = getLobbySpawnLocation();
        if (lobbySpawn != null) {
            event.setRespawnLocation(lobbySpawn);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpawnCommand(PlayerCommandPreprocessEvent event) {
        if (!isSubServer()) return;
        String msg = event.getMessage().trim().toLowerCase(Locale.ROOT);
        if (msg.equals("/spawn") || msg.equals("/essentials:spawn") || msg.equals("/esPawn")) {
            Location lobbySpawn = getLobbySpawnLocation();
            if (lobbySpawn != null) {
                event.setCancelled(true);
                Player player = event.getPlayer();
                player.teleport(lobbySpawn);
                player.sendMessage(ColorUtil.colorize("&8[&b&lGRIZM&f&lMC&8] &aSunucu lobisine ışınlandınız."));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVoidFallInLobby(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        if (!isInLobbyWorld(event.getTo().getWorld())) return;
        if (event.getTo().getY() < -20) {
            Location lobbySpawn = getLobbySpawnLocation();
            if (lobbySpawn != null) {
                Player p = event.getPlayer();
                p.setFallDistance(0f);
                p.teleport(lobbySpawn);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (isInLobbyWorld(event.getBlock().getWorld()) && !canBypassProtection(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isInLobbyWorld(event.getBlock().getWorld()) && !canBypassProtection(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (isInLobbyWorld(event.getBlock().getWorld()) && !canBypassProtection(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (isInLobbyWorld(event.getBlock().getWorld()) && !canBypassProtection(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onLobbyDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player && isInLobbyWorld(event.getEntity().getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onLobbyHunger(FoodLevelChangeEvent event) {
        if (isInLobbyWorld(event.getEntity().getWorld())) {
            event.setCancelled(true);
            event.setFoodLevel(20);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onLobbyMobSpawn(CreatureSpawnEvent event) {
        if (isInLobbyWorld(event.getLocation().getWorld())
                && event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onLobbyEntityExplode(EntityExplodeEvent event) {
        if (isInLobbyWorld(event.getLocation().getWorld())) {
            event.blockList().clear();
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onLobbyBlockExplode(BlockExplodeEvent event) {
        if (isInLobbyWorld(event.getBlock().getWorld())) {
            event.blockList().clear();
            event.setCancelled(true);
        }
    }
}

