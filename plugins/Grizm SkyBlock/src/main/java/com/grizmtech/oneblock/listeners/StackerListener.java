package com.grizmtech.oneblock.listeners;

import com.grizmtech.oneblock.GrizmOneBlock;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class StackerListener implements Listener {

    private final GrizmOneBlock plugin;

    public StackerListener(GrizmOneBlock plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player || entity instanceof ArmorStand || entity.getType() == EntityType.ARMOR_STAND) return;
        Location loc = entity.getLocation();
        if (loc.getWorld() == null || !loc.getWorld().getName().startsWith("island_")) return;

        // Yakındaki aynı türden yaratığı ara (Yarıçap: 4 blok)
        for (Entity nearby : loc.getWorld().getNearbyEntities(loc, 4, 4, 4)) {
            if (nearby instanceof LivingEntity target && !target.equals(entity)) {
                if (target instanceof Player || target instanceof ArmorStand || target.getType() == EntityType.ARMOR_STAND) continue;
                if (target.getType() == entity.getType() && target.isValid() && !target.isDead()) {
                    int currentStack = plugin.getStackerManager().getStack(target);
                    if (currentStack < 100) { // Maksimum 100x istif
                        event.setCancelled(true);
                        plugin.getStackerManager().setStack(target, currentStack + 1);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        if (entity instanceof Player || entity instanceof ArmorStand || entity.getType() == EntityType.ARMOR_STAND) return;
        Location loc = entity.getLocation();
        if (loc.getWorld() == null || !loc.getWorld().getName().startsWith("island_")) return;

        int stack = plugin.getStackerManager().getStack(entity);
        if (stack <= 1) return;

        // Ölümcül hasar kontrolü
        if (entity.getHealth() - event.getFinalDamage() <= 0) {
            event.setDamage(0);
            plugin.getStackerManager().setStack(entity, stack - 1);

            double maxHealth = 20.0;
            if (entity.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
                maxHealth = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
            }
            entity.setHealth(maxHealth);

            // Ölen tekil yaratığın ganimetini ve XP'sini bırak
            loc.getWorld().spawn(loc, org.bukkit.entity.ExperienceOrb.class, orb -> orb.setExperience(3));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpawnerPlace(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType() != Material.SPAWNER) return;
        if (event.getBlockAgainst().getType() == Material.SPAWNER) {
            // Spawner istifleme
            event.setCancelled(true);
            Player player = event.getPlayer();
            player.getInventory().getItem(event.getHand()).subtract(1);
            player.sendMessage(Component.text("Spawner başarıyla istiflendi!").color(NamedTextColor.GREEN));
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_USE, 0.6f, 1.4f);
        }
    }
}
