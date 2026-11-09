package com.grizmtech.towny.teleport;

import com.grizmtech.towny.combat.CombatTagManager;
import com.grizmtech.towny.core.MessageUtil;
import com.grizmtech.towny.hooks.VaultHook;
import com.grizmtech.towny.storage.DatabaseManager;
import com.grizmtech.towny.town.Town;
import com.grizmtech.towny.town.TownManager;
import com.grizmtech.towny.war.SiegeWar;
import com.grizmtech.towny.war.WarManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WarpManager {

    private final JavaPlugin plugin;
    private final DatabaseManager databaseManager;
    private final TownManager townManager;
    private final WarManager warManager;
    private final CombatTagManager combatTagManager;
    private final VaultHook vaultHook;

    private final Map<String, Warp> warps = new ConcurrentHashMap<>();

    public WarpManager(JavaPlugin plugin, DatabaseManager databaseManager, TownManager townManager,
                       WarManager warManager, CombatTagManager combatTagManager, VaultHook vaultHook) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.townManager = townManager;
        this.warManager = warManager;
        this.combatTagManager = combatTagManager;
        this.vaultHook = vaultHook;

        loadAll();
    }

    private void loadAll() {
        warps.putAll(databaseManager.loadWarps());
        plugin.getLogger().info("Yüklendi: " + warps.size() + " warp noktası.");
    }

    public Warp getWarp(String warpId) {
        if (warpId == null) return null;
        return warps.get(warpId.toLowerCase());
    }

    public Collection<Warp> getAllWarps() {
        return Collections.unmodifiableCollection(warps.values());
    }

    public List<Warp> getServerWarps() {
        List<Warp> list = new ArrayList<>();
        for (Warp w : warps.values()) {
            if (w.isServerWarp()) list.add(w);
        }
        return list;
    }

    public List<Warp> getTownWarps(UUID townUuid) {
        List<Warp> list = new ArrayList<>();
        for (Warp w : warps.values()) {
            if (w.getTownUuid() != null && w.getTownUuid().equals(townUuid)) {
                list.add(w);
            }
        }
        return list;
    }

    public boolean setServerWarp(Player player, String warpId, double cost, String permission) {
        Location loc = player.getLocation();
        Warp warp = new Warp(warpId.toLowerCase(), null, loc.getWorld().getUID(),
                loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch(), cost, permission);

        warps.put(warpId.toLowerCase(), warp);
        databaseManager.saveWarpAsync(warp);

        MessageUtil.sendMessage(player, "<green>Sunucu warp noktası kaydedildi: <yellow>" + warpId + "</yellow></green>");
        return true;
    }

    public boolean setTownWarp(Player player, Town town, String warpId, double cost) {
        Location loc = player.getLocation();
        String fullKey = town.getName().toLowerCase() + "_" + warpId.toLowerCase();

        Warp warp = new Warp(fullKey, town.getUuid(), loc.getWorld().getUID(),
                loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch(), cost, null);

        warps.put(fullKey, warp);
        databaseManager.saveWarpAsync(warp);

        MessageUtil.sendMessage(player, "<green>Kasaba warp noktası kaydedildi: <yellow>" + warpId + "</yellow></green>");
        return true;
    }

    public boolean deleteWarp(Player player, String warpId) {
        Warp warp = warps.remove(warpId.toLowerCase());
        if (warp == null) {
            MessageUtil.sendMessage(player, "<red>Böyle bir warp bulunamadı!</red>");
            return false;
        }

        databaseManager.deleteWarpAsync(warpId.toLowerCase());
        MessageUtil.sendMessage(player, "<yellow>Warp başarıyla silindi: " + warpId + "</yellow>");
        return true;
    }

    public void teleportToWarp(Player player, Warp warp) {
        if (combatTagManager.isTagged(player.getUniqueId())) {
            MessageUtil.sendMessage(player, "<red>Savaş halindeyken ışınlanamazsınız!</red>");
            return;
        }

        Location targetLoc = warp.toLocation();
        if (targetLoc == null || targetLoc.getWorld() == null) {
            MessageUtil.sendMessage(player, "<red>Warp hedef dünyası yüklenemedi!</red>");
            return;
        }

        // Eğer kasaba warpıysa kuşatma kontrollerini uygula
        if (warp.isTownWarp()) {
            Town town = townManager.getTown(warp.getTownUuid());
            if (town != null) {
                if (!checkTownTeleportSafety(player, town, targetLoc)) {
                    return;
                }
            }
        }

        // Ücret kontrolü
        if (warp.getVisitCost() > 0) {
            if (!vaultHook.takeMoney(player, warp.getVisitCost())) {
                MessageUtil.sendMessage(player, "<red>Bu noktaya ışınlanmak için yeterli paranız yok! Ücret: <yellow>" + warp.getVisitCost() + "$</yellow></red>");
                return;
            }
            if (warp.isTownWarp()) {
                Town town = townManager.getTown(warp.getTownUuid());
                if (town != null) {
                    town.deposit(warp.getVisitCost());
                    databaseManager.saveTownAsync(town);
                }
            }
        }

        player.teleportAsync(targetLoc).thenAccept(success -> {
            if (success) {
                MessageUtil.sendMessage(player, "<green>Warp noktasına başarıyla ışınlandınız: <yellow>" + warp.getWarpId() + "</yellow></green>");
            }
        });
    }

    public void teleportToTownSpawn(Player player, Town town) {
        if (combatTagManager.isTagged(player.getUniqueId())) {
            MessageUtil.sendMessage(player, "<red>Savaş halindeyken ışınlanamazsınız!</red>");
            return;
        }

        Location spawn = town.getHomeLocation();
        if (spawn == null || spawn.getWorld() == null) {
            MessageUtil.sendMessage(player, "<red>Kasabanın spawn noktası ayarlanmamış!</red>");
            return;
        }

        if (!checkTownTeleportSafety(player, town, spawn)) {
            return;
        }

        // Dışarıdan gelen ziyaretçiler için spawn vergisi kontrolü
        Town playerTown = townManager.getTownByPlayer(player.getUniqueId());
        boolean isResident = (playerTown != null && playerTown.getUuid().equals(town.getUuid()));

        if (!isResident && town.getSpawnCost() > 0) {
            if (!vaultHook.takeMoney(player, town.getSpawnCost())) {
                MessageUtil.sendMessage(player, "<red>Bu kasabaya ışınlanmak için giriş vergisi gereklidir! Ücret: <yellow>" + town.getSpawnCost() + "$</yellow></red>");
                return;
            }
            town.deposit(town.getSpawnCost());
            databaseManager.saveTownAsync(town);
            MessageUtil.sendMessage(player, "<yellow>" + town.getSpawnCost() + "$ giriş vergisi kasaba bankasına ödendi.</yellow>");
        }

        player.teleportAsync(spawn).thenAccept(success -> {
            if (success) {
                MessageUtil.sendMessage(player, "<green><b>" + town.getName() + "</b> kasaba merkezine ışınlandınız!</green>");
            }
        });
    }

    private boolean checkTownTeleportSafety(Player player, Town town, Location targetLoc) {
        // Kasaba aktif kuşatma altında mı?
        if (warManager.isBesieged(town.getUuid())) {
            Town playerTown = townManager.getTownByPlayer(player.getUniqueId());
            boolean isResident = (playerTown != null && playerTown.getUuid().equals(town.getUuid()));

            if (!isResident) {
                MessageUtil.sendMessage(player, "<red>Bu kasaba şu anda aktif kuşatma altındadır! Yabancıların ışınlanması kilitlidir.</red>");
                return false;
            }

            // Kasaba üyesiyse: sancağa 50 bloktan yakın mı?
            SiegeWar war = warManager.getWarByTown(town.getUuid());
            if (war != null && war.getBannerLocation() != null) {
                if (war.isInsideSiegeZone(targetLoc, 50.0)) {
                    MessageUtil.sendMessage(player, "<red>Spawn noktası kuşatma sancağına 50 bloktan daha yakındır! Savaş güvenliği sebebiyle ışınlanamazsınız.</red>");
                    return false;
                }
            }
        }
        return true;
    }
}
