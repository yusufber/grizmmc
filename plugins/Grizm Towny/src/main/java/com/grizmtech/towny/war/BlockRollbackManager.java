package com.grizmtech.towny.war;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

public class BlockRollbackManager {

    private final JavaPlugin plugin;
    // warId -> Location -> Original BlockData
    private final Map<Integer, Map<Location, BlockData>> originalBlocksByWar = new ConcurrentHashMap<>();
    private final Map<Integer, BukkitTask> activeRollbackTasks = new ConcurrentHashMap<>();

    public BlockRollbackManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void recordBlock(int warId, Location location) {
        if (location == null || location.getWorld() == null) return;
        Map<Location, BlockData> map = originalBlocksByWar.computeIfAbsent(warId, k -> new ConcurrentHashMap<>());
        // Sadece ilk orijinal halini sakla
        map.putIfAbsent(location.clone(), location.getBlock().getBlockData().clone());
    }

    public void startRollback(int warId, Runnable onComplete) {
        Map<Location, BlockData> map = originalBlocksByWar.remove(warId);
        if (map == null || map.isEmpty()) {
            if (onComplete != null) onComplete.run();
            return;
        }

        Queue<Map.Entry<Location, BlockData>> queue = new LinkedList<>(map.entrySet());

        // Saniyede 20 blok hızında (her tick 1 blok) onar
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (queue.isEmpty()) {
                    cancel();
                    activeRollbackTasks.remove(warId);
                    if (onComplete != null) {
                        onComplete.run();
                    }
                    return;
                }

                // Her tick 1 blok geri yükle (saniyede 20 blok)
                Map.Entry<Location, BlockData> entry = queue.poll();
                if (entry != null) {
                    Location loc = entry.getKey();
                    BlockData data = entry.getValue();
                    if (loc.getWorld() != null && loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                        loc.getBlock().setBlockData(data, false);
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);

        activeRollbackTasks.put(warId, task);
    }

    public void clear(int warId) {
        originalBlocksByWar.remove(warId);
        BukkitTask task = activeRollbackTasks.remove(warId);
        if (task != null) {
            task.cancel();
        }
    }

    public void shutdown() {
        for (BukkitTask task : activeRollbackTasks.values()) {
            task.cancel();
        }
        activeRollbackTasks.clear();
        originalBlocksByWar.clear();
    }
}
