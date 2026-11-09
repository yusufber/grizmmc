package com.grizmtech.oneblock.listeners;

import com.grizmtech.oneblock.GrizmOneBlock;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.UUID;

public class SkillListener implements Listener {

    private final GrizmOneBlock plugin;

    public SkillListener(GrizmOneBlock plugin) {
        this.plugin = plugin;
    }

    // 1. Otomatik Ekin Yeniden Ekme (Auto-Replant) & Kazma Dayanıklılığı
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCropHarvest(BlockBreakEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Block block = event.getBlock();

        // Kazma Dayanıklılığı Tasarrufu
        int duraLevel = plugin.getSkillManager().getSkillLevel(uuid, "durability");
        if (duraLevel > 0 && java.util.concurrent.ThreadLocalRandom.current().nextInt(100) < (duraLevel * 15)) {
            ItemStack tool = player.getInventory().getItemInMainHand();
            if (tool.getItemMeta() instanceof Damageable dmg) {
                dmg.setDamage(Math.max(0, dmg.getDamage() - 1));
                tool.setItemMeta(dmg);
            }
        }

        // Otomatik Ekin Ekme
        if (plugin.getSkillManager().hasSkill(uuid, "auto_replant")) {
            if (block.getBlockData() instanceof Ageable ageable) {
                if (ageable.getAge() == ageable.getMaximumAge()) {
                    Material plantMat = block.getType();
                    event.setDropItems(true);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        block.setType(plantMat);
                        Ageable newAge = (Ageable) block.getBlockData();
                        newAge.setAge(0);
                        block.setBlockData(newAge);
                    }, 1L);
                }
            }
        }
    }

    // 2. Yaratıklara Ekstra Hasar (Savaşçı Dalı)
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();

        int mobDmgLevel = plugin.getSkillManager().getSkillLevel(uuid, "mob_damage");
        if (mobDmgLevel > 0) {
            double bonus = 1.0 + (mobDmgLevel * 0.15); // +%15, +%30, +%45
            event.setDamage(event.getDamage() * bonus);
        }
    }

    // 3. Yaratıklardan Ekstra XP (Savaşçı Dalı)
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        int xpLevel = plugin.getSkillManager().getSkillLevel(killer.getUniqueId(), "extra_xp");
        if (xpLevel > 0) {
            int currentExp = event.getDroppedExp();
            event.setDroppedExp((int) (currentExp * (1.0 + (xpLevel * 0.25))));
        }
    }

    // 4. Ada İçi Uçuş Yetkisi (Mimar Dalı)
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();

        if (worldName.equals("island_" + player.getUniqueId())) {
            if (plugin.getSkillManager().hasSkill(player.getUniqueId(), "fly")) {
                player.setAllowFlight(true);
            }
        } else {
            if (!player.hasPermission("grizmoneblock.admin")) {
                player.setAllowFlight(false);
                player.setFlying(false);
            }
        }
    }
}
