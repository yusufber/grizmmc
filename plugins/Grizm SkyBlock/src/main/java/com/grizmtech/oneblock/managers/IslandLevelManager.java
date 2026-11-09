package com.grizmtech.oneblock.managers;

import com.grizmtech.oneblock.GrizmOneBlock;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class IslandLevelManager {

    private final GrizmOneBlock plugin;
    private final Map<UUID, Long> cachedLevels = new HashMap<>();
    private final List<Map.Entry<String, Long>> top10 = new ArrayList<>();

    public IslandLevelManager(GrizmOneBlock plugin) {
        this.plugin = plugin;
        loadCachedLevels();
    }

    private void loadCachedLevels() {
        if (plugin.getDataManager().getConfig().contains("players")) {
            for (String uuidStr : plugin.getDataManager().getConfig().getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    long level = plugin.getDataManager().getConfig().getLong("players." + uuidStr + ".island_level", 0L);
                    cachedLevels.put(uuid, level);
                } catch (Exception ignored) {}
            }
        }
        refreshTop10();
    }

    public long getIslandLevel(UUID uuid) {
        return cachedLevels.getOrDefault(uuid, 0L);
    }

    public List<Map.Entry<String, Long>> getTop10() {
        return top10;
    }

    public void calculateIslandLevelAsync(Player player, Consumer<Long> callback) {
        UUID ownerUUID = player.getUniqueId();
        World world = Bukkit.getWorld("island_" + ownerUUID.toString());

        if (world == null) {
            player.sendMessage(Component.text("Ada dünyanız bulunamadı!").color(NamedTextColor.RED));
            return;
        }

        player.sendMessage(Component.text("Ada seviyeniz asenkron olarak hesaplanıyor, lütfen bekleyin...").color(NamedTextColor.YELLOW));

        // Yüklü olan tüm chunk'ların snapshot'larını ana iş parçacığında hızlıca al
        List<ChunkSnapshot> snapshots = new ArrayList<>();
        for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
            snapshots.add(chunk.getChunkSnapshot());
        }

        // Ana iş parçacığını dondurmadan (Asenkron) hesaplama yap
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            long points = 0;

            for (ChunkSnapshot snapshot : snapshots) {
                int minY = world.getMinHeight();
                int maxY = world.getMaxHeight();

                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int y = minY; y < maxY; y++) {
                            Material type = snapshot.getBlockType(x, y, z);
                            if (type == Material.AIR || type == Material.VOID_AIR || type == Material.CAVE_AIR) continue;

                            if (type == Material.NETHERITE_BLOCK) points += 1000;
                            else if (type == Material.DIAMOND_BLOCK) points += 100;
                            else if (type == Material.EMERALD_BLOCK) points += 80;
                            else if (type == Material.GOLD_BLOCK) points += 30;
                            else if (type == Material.IRON_BLOCK) points += 10;
                            else if (type == Material.COPPER_BLOCK) points += 5;
                            else if (type == Material.OBSIDIAN) points += 4;
                            else if (type == Material.BEACON) points += 500;
                        }
                    }
                }
            }

            final long finalLevel = points / 100;

            // Ana iş parçacığına geri dön ve kaydet
            Bukkit.getScheduler().runTask(plugin, () -> {
                cachedLevels.put(ownerUUID, finalLevel);
                plugin.getDataManager().getConfig().set("players." + ownerUUID.toString() + ".island_level", finalLevel);
                plugin.getDataManager().saveConfig();
                refreshTop10();

                player.sendMessage(Component.text("Ada seviyeniz güncellendi: ").color(NamedTextColor.GREEN)
                        .append(Component.text(finalLevel + " Seviye").color(NamedTextColor.GOLD)));
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);

                if (callback != null) {
                    callback.accept(finalLevel);
                }
            });
        });
    }

    public void refreshTop10() {
        Map<String, Long> nameMap = new HashMap<>();
        for (Map.Entry<UUID, Long> entry : cachedLevels.entrySet()) {
            org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(entry.getKey());
            String name = op.getName() != null ? op.getName() : "Bilinmeyen";
            nameMap.put(name, entry.getValue());
        }

        List<Map.Entry<String, Long>> sorted = new ArrayList<>(nameMap.entrySet());
        sorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

        top10.clear();
        for (int i = 0; i < Math.min(10, sorted.size()); i++) {
            top10.add(sorted.get(i));
        }
    }
}
