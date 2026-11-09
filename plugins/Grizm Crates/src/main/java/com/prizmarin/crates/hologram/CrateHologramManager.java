package com.prizmarin.crates.hologram;

import com.prizmarin.crates.CrateTier;
import com.prizmarin.crates.CratesPlugin;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public class CrateHologramManager {

    private final CratesPlugin plugin;
    private final boolean decentHologramsEnabled;

    public CrateHologramManager(CratesPlugin plugin) {
        this.plugin = plugin;
        this.decentHologramsEnabled = Bukkit.getPluginManager().isPluginEnabled("DecentHolograms");
    }

    public boolean isDecentHologramsEnabled() {
        return decentHologramsEnabled;
    }

    public void createOrUpdateHologram(String crateId, Location blockLocation, CrateTier tier) {
        if (!decentHologramsEnabled) {
            return;
        }

        String holoName = "crate_" + crateId;
        Location holoLoc = blockLocation.clone().add(0.5, 1.8, 0.5);

        List<String> lines = getHologramLines(tier);

        try {
            Hologram existing = DHAPI.getHologram(holoName);
            if (existing != null) {
                DHAPI.removeHologram(holoName);
            }
            DHAPI.createHologram(holoName, holoLoc, lines);
        } catch (Exception e) {
            plugin.getLogger().warning("Hologram oluşturulurken hata (" + holoName + "): " + e.getMessage());
        }
    }

    public void removeHologram(String crateId) {
        if (!decentHologramsEnabled) {
            return;
        }

        String holoName = "crate_" + crateId;
        try {
            Hologram existing = DHAPI.getHologram(holoName);
            if (existing != null) {
                DHAPI.removeHologram(holoName);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Hologram silinirken hata (" + holoName + "): " + e.getMessage());
        }
    }

    private List<String> getHologramLines(CrateTier tier) {
        ConfigurationSection holoSec = plugin.getConfig().getConfigurationSection("tiers." + tier.getId() + ".hologram");
        if (holoSec != null && holoSec.contains("lines")) {
            return holoSec.getStringList("lines");
        }

        // Fallback lines
        List<String> fallback = new ArrayList<>();
        fallback.add("#ICON: " + tier.getIconMaterial().name());
        fallback.add(tier.getDisplayName());
        fallback.add("<yellow>Sağ Tık:</yellow> <green>Aç</green> <dark_gray>|</dark_gray> <yellow>Sol Tık:</yellow> <aqua>Önizle</aqua>");
        return fallback;
    }
}
