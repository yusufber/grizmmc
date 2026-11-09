package com.grizmtech.oneblock.managers;

import com.grizmtech.oneblock.GrizmOneBlock;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HologramManager {

    private final GrizmOneBlock plugin;
    private final Map<UUID, TextDisplay> displays = new ConcurrentHashMap<>();

    public HologramManager(GrizmOneBlock plugin) {
        this.plugin = plugin;
    }

    public void updateHologram(UUID ownerUUID) {
        Location oneBlockLoc = plugin.getDataManager().getOneBlockLocation(ownerUUID);
        if (oneBlockLoc == null || oneBlockLoc.getWorld() == null) return;

        Location holoLoc = oneBlockLoc.clone().add(0.5, 1.85, 0.5);

        TextDisplay display = displays.get(ownerUUID);
        if (display == null || !display.isValid() || display.isDead()) {
            // Varsa eski text display'leri temizle
            for (Entity e : holoLoc.getWorld().getNearbyEntities(holoLoc, 2, 2, 2)) {
                if (e instanceof TextDisplay td && td.getScoreboardTags().contains("grizm_holo_" + ownerUUID)) {
                    td.remove();
                }
            }

            display = (TextDisplay) holoLoc.getWorld().spawnEntity(holoLoc, EntityType.TEXT_DISPLAY);
            display.setBillboard(Display.Billboard.CENTER);
            display.addScoreboardTag("grizm_holo_" + ownerUUID);
            displays.put(ownerUUID, display);
        }

        int blocks = plugin.getDataManager().getBlocksBroken(ownerUUID);
        PhaseManager.Phase phase = plugin.getPhaseManager().getCurrentPhase(blocks);
        String prestigeBadge = plugin.getRebirthManager().getPrestigeBadge(ownerUUID);

        int currentInPhase = Math.max(0, blocks - phase.getRequiredBlocks());
        int neededInPhase = Math.max(1, phase.getEndBlocks() - phase.getRequiredBlocks());
        int remaining = Math.max(0, phase.getEndBlocks() - blocks);

        String progressBar = buildProgressBar(currentInPhase, neededInPhase, 10);

        String text = "§6§l★ GRIZM ONEBLOCK ★ " + prestigeBadge + "\n"
                + "§eAşama: §f" + phase.getName() + " §7(Seviye " + phase.getTier() + "/10)\n"
                + "§bİlerleme: " + progressBar + " §f" + blocks + " / " + phase.getEndBlocks() + "\n"
                + "§7Sonraki Aşamaya: §e" + remaining + " Blok";

        display.text(Component.text(text));
    }

    private String buildProgressBar(int current, int max, int totalBars) {
        float percent = (float) current / max;
        int progressBars = (int) (totalBars * percent);

        StringBuilder sb = new StringBuilder("§a[");
        for (int i = 0; i < totalBars; i++) {
            if (i < progressBars) {
                sb.append("■");
            } else {
                sb.append("§7■");
            }
        }
        sb.append("§a]");
        return sb.toString();
    }

    public void removeAll() {
        for (TextDisplay td : displays.values()) {
            if (td != null && td.isValid()) {
                td.remove();
            }
        }
        displays.clear();
    }
}
