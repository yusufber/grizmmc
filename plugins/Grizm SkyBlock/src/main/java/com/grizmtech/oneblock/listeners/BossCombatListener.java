package com.grizmtech.oneblock.listeners;

import com.grizmtech.oneblock.GrizmOneBlock;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.UUID;

public class BossCombatListener implements Listener {

    private final GrizmOneBlock plugin;

    public BossCombatListener(GrizmOneBlock plugin) {
        this.plugin = plugin;
    }

    private UUID getIslandOwner(LivingEntity entity) {
        if (entity.getWorld() == null) return null;
        String name = entity.getWorld().getName();
        if (name.startsWith("island_")) {
            try {
                return UUID.fromString(name.replace("island_", ""));
            } catch (Exception ignored) {}
        }
        return null;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBossDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        UUID owner = getIslandOwner(entity);
        if (owner == null) return;

        if (plugin.getBossManager().isBossActive(owner)) {
            double maxHealth = 100.0;
            if (entity.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
                maxHealth = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
            }
            double current = Math.max(0, entity.getHealth() - event.getFinalDamage());
            plugin.getBossManager().updateBossHealth(owner, current, maxHealth);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBossDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        UUID owner = getIslandOwner(entity);
        if (owner == null) return;

        if (plugin.getBossManager().isBossActive(owner)) {
            Player killer = entity.getKiller();
            plugin.getBossManager().onBossKilled(owner, killer);
            plugin.getHologramManager().updateHologram(owner);
        }
    }
}
