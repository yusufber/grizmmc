package com.prizmarin.crates;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CrateRegistry {

    private final CratesPlugin plugin;
    private final File file;
    private FileConfiguration config;
    private final Map<String, CrateLocation> cratesById = new HashMap<>();

    public CrateRegistry(CratesPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "crates.yml");
    }

    public void load() {
        cratesById.clear();
        if (!file.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("crates.yml dosyası oluşturulamadı: " + e.getMessage());
            }
        }

        config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            String tierStr = config.getString(key + ".tier");
            CrateTier tier = CrateTier.fromId(tierStr);
            String world = config.getString(key + ".world");
            int x = config.getInt(key + ".x");
            int y = config.getInt(key + ".y");
            int z = config.getInt(key + ".z");

            if (tier != null && world != null) {
                CrateLocation cl = new CrateLocation(key, tier, world, x, y, z);
                cratesById.put(key, cl);

                // Update or recreate hologram
                Location loc = cl.toLocation();
                if (loc != null) {
                    plugin.getHologramManager().createOrUpdateHologram(cl.getId(), loc, tier);
                }
            }
        }
    }

    public void save() {
        if (config == null) {
            config = new YamlConfiguration();
        }
        // Clear old keys
        for (String key : config.getKeys(false)) {
            config.set(key, null);
        }

        for (CrateLocation cl : cratesById.values()) {
            config.set(cl.getId() + ".tier", cl.getTier().getId());
            config.set(cl.getId() + ".world", cl.getWorldName());
            config.set(cl.getId() + ".x", cl.getX());
            config.set(cl.getId() + ".y", cl.getY());
            config.set(cl.getId() + ".z", cl.getZ());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("crates.yml kaydedilirken hata: " + e.getMessage());
        }
    }

    public CrateLocation getCrateAt(Location loc) {
        if (loc == null) return null;
        for (CrateLocation cl : cratesById.values()) {
            if (cl.matches(loc)) {
                return cl;
            }
        }
        return null;
    }

    public CrateLocation getCrateAt(Block block) {
        if (block == null) return null;
        return getCrateAt(block.getLocation());
    }

    public void registerCrate(CrateTier tier, Location loc) {
        String id = "crate_" + (cratesById.size() + 1);
        while (cratesById.containsKey(id)) {
            id = "crate_" + UUID.randomUUID().toString().substring(0, 8);
        }

        CrateLocation cl = new CrateLocation(id, tier, loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        cratesById.put(id, cl);
        save();

        plugin.getHologramManager().createOrUpdateHologram(id, loc, tier);
    }

    public boolean removeCrate(CrateLocation cl) {
        if (cl == null) return false;
        cratesById.remove(cl.getId());
        save();
        plugin.getHologramManager().removeHologram(cl.getId());
        return true;
    }

    public Collection<CrateLocation> getAllCrates() {
        return Collections.unmodifiableCollection(cratesById.values());
    }
}
