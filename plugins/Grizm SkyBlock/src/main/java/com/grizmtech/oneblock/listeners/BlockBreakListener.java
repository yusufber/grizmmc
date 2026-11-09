package com.grizmtech.oneblock.listeners;

import com.grizmtech.oneblock.GrizmOneBlock;
import com.grizmtech.oneblock.managers.BlockEventManager;
import com.grizmtech.oneblock.managers.PhaseManager;
import com.grizmtech.oneblock.models.IslandPermission;
import com.grizmtech.oneblock.models.IslandSetting;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class BlockBreakListener implements Listener {

    private final GrizmOneBlock plugin;

    public BlockBreakListener(GrizmOneBlock plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        String worldName = block.getWorld().getName();

        // Sadece ada dünyalarında çalış
        if (!worldName.startsWith("island_")) return;

        UUID ownerUUID;
        try {
            ownerUUID = UUID.fromString(worldName.replace("island_", ""));
        } catch (Exception e) {
            return;
        }

        // Adanın OneBlock konumunu al
        Location oneBlockLoc = plugin.getDataManager().getOneBlockLocation(ownerUUID);
        if (oneBlockLoc == null || oneBlockLoc.getWorld() == null) return;

        // Kırılan blok tam olarak OneBlock mu?
        if (block.getWorld().equals(oneBlockLoc.getWorld())
                && block.getX() == oneBlockLoc.getBlockX()
                && block.getY() == oneBlockLoc.getBlockY()
                && block.getZ() == oneBlockLoc.getBlockZ()) {

            // 0. Akıllı Anti-Makro ve AutoClicker Kontrolü
            if (!plugin.getAntiMacroManager().checkPlayer(player)) {
                event.setCancelled(true);
                return;
            }

            // 1. Aşama Boss'u Hayatta mı?
            if (plugin.getBossManager().isBossActive(ownerUUID)) {
                event.setCancelled(true);
                player.sendMessage(Component.text("Aşama Boss'unu kesmeden Tek Bloğu kıramazsınız!").color(NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
                return;
            }

            // 2. Yetki Kontrolü (RBAC)
            if (!player.getUniqueId().equals(ownerUUID) && !player.hasPermission("grizmoneblock.admin")) {
                boolean hasMinePerm = plugin.getRoleManager().hasPermission(ownerUUID, player.getUniqueId(), IslandPermission.MINE_ONEBLOCK);
                if (!hasMinePerm) {
                    event.setCancelled(true);
                    player.sendActionBar(Component.text("Bu adanın Tek Bloğunu kırma yetkiniz yok!").color(NamedTextColor.RED));
                    return;
                }
            }

            // 3. Üstte Duranı İtme Mekanizması (Entity Push-Up)
            Location topLoc = block.getLocation().clone().add(0.5, 1.0, 0.5);
            for (Entity nearby : block.getWorld().getNearbyEntities(topLoc, 0.6, 1.0, 0.6)) {
                if (nearby instanceof LivingEntity living) {
                    living.setVelocity(new Vector(0, 0.45, 0));
                }
            }

            // Altındaki bloğun kırılmaz Bedrock olduğundan emin ol
            Location under = block.getLocation().clone().subtract(0, 1, 0);
            if (under.getBlock().getType() != Material.BEDROCK) {
                under.getBlock().setType(Material.BEDROCK);
            }

            // Dinamik Mikro-Etkinlik / Anomali Tetikleme (%0.5 şans)
            if (ThreadLocalRandom.current().nextInt(200) == 0) {
                plugin.getBlockEventManager().triggerRandomEvent(ownerUUID, player);
            }

            // Görev İlerlemesini Kaydet
            plugin.getMissionManager().addProgress(ownerUUID, block.getType());

            // Telekinesis: Doğrudan oyuncunun envanterine ver
            event.setDropItems(false);

            java.util.Collection<org.bukkit.inventory.ItemStack> drops = block.getDrops(player.getInventory().getItemInMainHand(), player);

            // Eğer sandıksa içindekileri ver
            if (block.getState() instanceof org.bukkit.block.Container container) {
                for (org.bukkit.inventory.ItemStack containerItem : container.getInventory().getContents()) {
                    if (containerItem != null && containerItem.getType() != Material.AIR) {
                        java.util.HashMap<Integer, org.bukkit.inventory.ItemStack> leftover = player.getInventory().addItem(containerItem);
                        for (org.bukkit.inventory.ItemStack drop : leftover.values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), drop);
                        }
                    }
                }
                container.getInventory().clear();
            }

            boolean rushHour = plugin.getBlockEventManager().isEventActive(ownerUUID, BlockEventManager.EventType.RUSH_HOUR);
            int doubleDropLvl = plugin.getSkillManager().getSkillLevel(player.getUniqueId(), "double_drop");
            boolean triggerDouble = doubleDropLvl > 0 && ThreadLocalRandom.current().nextInt(100) < (doubleDropLvl * 5);

            for (org.bukkit.inventory.ItemStack drop : drops) {
                if (rushHour && isOre(drop.getType())) {
                    drop.setAmount(drop.getAmount() * 3);
                } else if (triggerDouble) {
                    drop.setAmount(drop.getAmount() * 2);
                }
                java.util.HashMap<Integer, org.bukkit.inventory.ItemStack> leftover = player.getInventory().addItem(drop);
                for (org.bukkit.inventory.ItemStack dropItem : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), dropItem);
                }
            }

            // XP ve XP Fırtınası Anomali Kontrolü
            int exp = event.getExpToDrop();
            if (plugin.getBlockEventManager().isEventActive(ownerUUID, BlockEventManager.EventType.XP_BURST)) {
                exp *= 5;
            }
            if (exp > 0) {
                player.giveExp(exp);
                event.setExpToDrop(0);
            }

            // Efekt ve Ses Kontrolü (CORE_PARTICLES)
            boolean particlesEnabled = plugin.getIslandSettingManager().getBoolean(ownerUUID, IslandSetting.CORE_PARTICLES);
            if (particlesEnabled) {
                try {
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.35f, 1.4f);
                    block.getWorld().spawnParticle(Particle.BLOCK, block.getLocation().clone().add(0.5, 0.5, 0.5), 10, 0.2, 0.2, 0.2, block.getBlockData());
                } catch (Exception ignored) {}
            }

            int currentBlocks = plugin.getDataManager().getBlocksBroken(ownerUUID);
            PhaseManager.Phase oldPhase = plugin.getPhaseManager().getCurrentPhase(currentBlocks);

            int newBlocks = currentBlocks + 1;
            plugin.getDataManager().setBlocksBroken(ownerUUID, newBlocks);

            // Yetenek Puanı Ödülü (Her 250 blokta 1 puan)
            if (newBlocks % 250 == 0) {
                plugin.getSkillManager().addSkillPoints(player.getUniqueId(), 1);
                player.sendMessage(Component.text("★ Tebrikler! 1 Yetenek Puanı kazandınız! (/ada beceri)").color(NamedTextColor.GOLD));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.4f);
            }

            if (newBlocks % 10 == 0) {
                plugin.getDataManager().saveConfig();
            }

            // Aşama Sonu Boss Kontrolü
            if (plugin.getPhaseManager().isPhaseEnd(currentBlocks)) {
                plugin.getBossManager().spawnBoss(ownerUUID, block.getLocation(), oldPhase);
            }

            // Aşama Kilidi (PHASE_LOCK) kontrolü
            boolean phaseLocked = plugin.getIslandSettingManager().getBoolean(ownerUUID, IslandSetting.PHASE_LOCK);
            PhaseManager.Phase activePhase;

            if (phaseLocked) {
                String lockedPhaseStr = plugin.getDataManager().getLockedPhase(ownerUUID);
                if (lockedPhaseStr != null) {
                    try {
                        activePhase = PhaseManager.Phase.valueOf(lockedPhaseStr);
                    } catch (Exception e) {
                        activePhase = oldPhase;
                    }
                } else {
                    activePhase = oldPhase;
                    plugin.getDataManager().setLockedPhase(ownerUUID, activePhase.name());
                }
            } else {
                PhaseManager.Phase newPhase = plugin.getPhaseManager().getCurrentPhase(newBlocks);
                activePhase = newPhase;

                if (newPhase != oldPhase) {
                    Title title = Title.title(
                            Component.text("★ Seviye Atladın! ★").color(NamedTextColor.GREEN),
                            Component.text("Yeni Biyom: " + newPhase.getName() + " (Seviye " + newPhase.getTier() + ")").color(NamedTextColor.GOLD),
                            Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofSeconds(1))
                    );
                    player.showTitle(title);
                }
            }

            final PhaseManager.Phase finalPhase = activePhase;

            double oreMultiplier = plugin.getRebirthManager().getOreMultiplier(ownerUUID);
            Material nextBlock = plugin.getPhaseManager().getRandomBlock(finalPhase, oreMultiplier);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (block.getWorld().isChunkLoaded(block.getChunk())) {
                    block.setType(nextBlock);

                    if (nextBlock == Material.CHEST && block.getState() instanceof org.bukkit.block.Chest chest) {
                        plugin.getPhaseManager().fillChestLoot(chest.getBlockInventory(), newBlocks, finalPhase);
                    }
                }
            }, 1L);

            boolean mobSpawnEnabled = plugin.getIslandSettingManager().getBoolean(ownerUUID, IslandSetting.CORE_MOB_SPAWN);
            if (mobSpawnEnabled && ThreadLocalRandom.current().nextInt(100) < 2) {
                EntityType mobType = plugin.getPhaseManager().getRandomMob(finalPhase);
                try {
                    block.getWorld().spawnEntity(block.getLocation().clone().add(0.5, 1, 0.5), mobType);
                } catch (Exception ignored) {}
            }

            // Hologramı Canlı Güncelle
            plugin.getHologramManager().updateHologram(ownerUUID);
        }
    }

    private boolean isOre(Material mat) {
        String name = mat.name();
        return name.contains("ORE") || name.equals("ANCIENT_DEBRIS") || name.contains("RAW_");
    }
}
