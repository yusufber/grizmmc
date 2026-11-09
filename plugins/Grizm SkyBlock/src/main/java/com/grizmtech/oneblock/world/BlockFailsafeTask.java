package com.grizmtech.oneblock.world;

import com.grizmtech.oneblock.GrizmOneBlock;
import com.grizmtech.oneblock.managers.PhaseManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class BlockFailsafeTask extends BukkitRunnable {

    private final GrizmOneBlock plugin;

    public BlockFailsafeTask(GrizmOneBlock plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (!plugin.getDataManager().getConfig().contains("players")) return;

        for (String uuidStr : plugin.getDataManager().getConfig().getConfigurationSection("players").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                Location loc = plugin.getDataManager().getOneBlockLocation(uuid);
                if (loc != null && loc.getWorld() != null && loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                    // Merkez blok hava olmuşsa veya yok edilmişse otomatik onar
                    if (loc.getBlock().getType() == Material.AIR || loc.getBlock().getType() == Material.VOID_AIR) {
                        int blocks = plugin.getDataManager().getBlocksBroken(uuid);
                        PhaseManager.Phase phase = plugin.getPhaseManager().getCurrentPhase(blocks);
                        Material newBlock = plugin.getPhaseManager().getRandomBlock(phase, 1.0);
                        loc.getBlock().setType(newBlock);

                        // Bedrock tabanı da kontrol et
                        Location bedrockLoc = loc.clone().subtract(0, 1, 0);
                        if (bedrockLoc.getBlock().getType() != Material.BEDROCK) {
                            bedrockLoc.getBlock().setType(Material.BEDROCK);
                        }

                        // Hologramı güncelle
                        plugin.getHologramManager().updateHologram(uuid);
                    }
                }
            } catch (Exception ignored) {}
        }
    }
}
