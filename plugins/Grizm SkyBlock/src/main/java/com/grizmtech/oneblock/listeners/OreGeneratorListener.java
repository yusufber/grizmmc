package com.grizmtech.oneblock.listeners;

import com.grizmtech.oneblock.GrizmOneBlock;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFormEvent;

import java.util.UUID;

public class OreGeneratorListener implements Listener {

    private final GrizmOneBlock plugin;

    public OreGeneratorListener(GrizmOneBlock plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockForm(BlockFormEvent event) {
        Block block = event.getBlock();
        String worldName = block.getWorld().getName();

        // Sadece ada dünyalarında çalış
        if (!worldName.startsWith("island_")) return;

        Material formedType = event.getNewState().getType();

        // Su ve lav etkileşimiyle kırıktaş (cobblestone), taş (stone) veya bazalt oluşumu
        if (formedType == Material.COBBLESTONE || formedType == Material.STONE || formedType == Material.BASALT) {
            UUID ownerUUID;
            try {
                ownerUUID = UUID.fromString(worldName.replace("island_", ""));
            } catch (Exception e) {
                return;
            }

            int level = plugin.getDataManager().getGeneratorLevel(ownerUUID);
            Material oreMaterial = plugin.getUpgradeManager().getRandomGeneratorBlock(level);

            event.getNewState().setType(oreMaterial);

            // Değerli maden çıktığında efekt ve ses ver
            if (isRareOre(oreMaterial)) {
                try {
                    block.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, block.getLocation().add(0.5, 0.8, 0.5), 6, 0.2, 0.2, 0.2);
                    block.getWorld().playSound(block.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.3f);
                } catch (Exception ignored) {}
            }
        }
    }

    private boolean isRareOre(Material m) {
        return m == Material.DIAMOND_ORE || m == Material.DEEPSLATE_DIAMOND_ORE
                || m == Material.EMERALD_ORE || m == Material.DEEPSLATE_EMERALD_ORE
                || m == Material.ANCIENT_DEBRIS || m == Material.GOLD_ORE || m == Material.DEEPSLATE_GOLD_ORE;
    }
}
