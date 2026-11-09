package com.grizmtech.oneblock.data;

import com.grizmtech.oneblock.GrizmOneBlock;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DataManager {

    private final GrizmOneBlock plugin;
    private File dataFile;
    private FileConfiguration dataConfig;

    public DataManager(GrizmOneBlock plugin) {
        this.plugin = plugin;
        setup();
    }

    // Dosyayı oluştur ve yükle
    public void setup() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("playerdata.yml oluşturulamadı!");
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public FileConfiguration getConfig() {
        return dataConfig;
    }

    public void saveConfig() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("playerdata.yml kaydedilemedi!");
        }
    }

    // Oyuncunun kırdığı blok sayısını al
    public int getBlocksBroken(UUID uuid) {
        return dataConfig.getInt("players." + uuid.toString() + ".blocks_broken", 0);
    }

    // Oyuncunun kırdığı blok sayısını ayarla
    public void setBlocksBroken(UUID uuid, int amount) {
        dataConfig.set("players." + uuid.toString() + ".blocks_broken", amount);
    }

    // Oyuncunun adası var mı kontrol et
    public boolean hasIsland(UUID uuid) {
        return dataConfig.contains("players." + uuid.toString() + ".oneblock_loc.world");
    }

    // Oyuncunun OneBlock konumunu kaydet
    public void setOneBlockLocation(UUID uuid, Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        String path = "players." + uuid.toString() + ".oneblock_loc";
        dataConfig.set(path + ".world", loc.getWorld().getName());
        dataConfig.set(path + ".x", loc.getBlockX());
        dataConfig.set(path + ".y", loc.getBlockY());
        dataConfig.set(path + ".z", loc.getBlockZ());
    }

    // Oyuncunun OneBlock konumunu al
    public Location getOneBlockLocation(UUID uuid) {
        String path = "players." + uuid.toString() + ".oneblock_loc";
        if (dataConfig.contains(path)) {
            String worldName = dataConfig.getString(path + ".world");
            if (worldName == null) return null;

            org.bukkit.World world = Bukkit.getWorld(worldName);
            if (world == null && worldName.startsWith("island_")) {
                org.bukkit.WorldCreator wc = new org.bukkit.WorldCreator(worldName);
                wc.generator(new com.grizmtech.oneblock.world.VoidChunkGenerator());
                world = Bukkit.createWorld(wc);
                if (world != null) {
                    plugin.getIslandSettingManager().applyBorder(uuid, world);
                }
            }

            if (world == null) {
                return null;
            }

            int x = dataConfig.getInt(path + ".x");
            int y = dataConfig.getInt(path + ".y");
            int z = dataConfig.getInt(path + ".z");
            return new Location(world, x, y, z);
        }
        return null;
    }

    // Toplam oluşturulan ada sayısını al
    public int getIslandCount() {
        return dataConfig.getInt("server.island_count", 0);
    }

    // Toplam ada sayısını ayarla
    public void setIslandCount(int count) {
        dataConfig.set("server.island_count", count);
    }

    // --- Geliştirme (Upgrade) Verileri ---

    public int getBorderLevel(UUID uuid) {
        return dataConfig.getInt("players." + uuid.toString() + ".border_level", 1);
    }

    public void setBorderLevel(UUID uuid, int level) {
        dataConfig.set("players." + uuid.toString() + ".border_level", level);
    }

    public int getHopperLevel(UUID uuid) {
        return dataConfig.getInt("players." + uuid.toString() + ".hopper_level", 1);
    }

    public void setHopperLevel(UUID uuid, int level) {
        dataConfig.set("players." + uuid.toString() + ".hopper_level", level);
    }

    public int getPlacedHoppers(UUID uuid) {
        return dataConfig.getInt("players." + uuid.toString() + ".placed_hoppers", 0);
    }

    public void setPlacedHoppers(UUID uuid, int amount) {
        dataConfig.set("players." + uuid.toString() + ".placed_hoppers", amount);
    }

    public int getGeneratorLevel(UUID uuid) {
        return dataConfig.getInt("players." + uuid.toString() + ".generator_level", 1);
    }

    public void setGeneratorLevel(UUID uuid, int level) {
        dataConfig.set("players." + uuid.toString() + ".generator_level", level);
    }

    // --- Ada Ayarları (Island Settings) Verileri ---

    public boolean getSettingBoolean(UUID uuid, com.grizmtech.oneblock.models.IslandSetting setting) {
        String path = "players." + uuid.toString() + ".settings." + setting.name();
        boolean def = (boolean) setting.getDefaultValue();
        return dataConfig.getBoolean(path, def);
    }

    public void setSettingBoolean(UUID uuid, com.grizmtech.oneblock.models.IslandSetting setting, boolean value) {
        String path = "players." + uuid.toString() + ".settings." + setting.name();
        dataConfig.set(path, value);
    }

    public String getSettingString(UUID uuid, com.grizmtech.oneblock.models.IslandSetting setting) {
        String path = "players." + uuid.toString() + ".settings." + setting.name();
        String def = (String) setting.getDefaultValue();
        return dataConfig.getString(path, def);
    }

    public void setSettingString(UUID uuid, com.grizmtech.oneblock.models.IslandSetting setting, String value) {
        String path = "players." + uuid.toString() + ".settings." + setting.name();
        dataConfig.set(path, value);
    }

    public String getLockedPhase(UUID uuid) {
        return dataConfig.getString("players." + uuid.toString() + ".locked_phase", null);
    }

    public void setLockedPhase(UUID uuid, String phase) {
        dataConfig.set("players." + uuid.toString() + ".locked_phase", phase);
    }

    // Kayıtlı olan tüm ada dünyalarının isimlerini döndürür
    public List<String> getAllIslandWorlds() {
        List<String> worlds = new ArrayList<>();
        if (dataConfig.contains("players")) {
            org.bukkit.configuration.ConfigurationSection sec = dataConfig.getConfigurationSection("players");
            if (sec != null) {
                for (String uuidStr : sec.getKeys(false)) {
                    String path = "players." + uuidStr + ".oneblock_loc.world";
                    if (dataConfig.contains(path)) {
                        String worldName = dataConfig.getString(path);
                        if (worldName != null && worldName.startsWith("island_") && !worlds.contains(worldName)) {
                            worlds.add(worldName);
                        }
                    }
                }
            }
        }
        return worlds;
    }
}
