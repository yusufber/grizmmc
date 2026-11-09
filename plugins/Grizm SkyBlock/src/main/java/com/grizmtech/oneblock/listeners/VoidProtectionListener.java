package com.grizmtech.oneblock.listeners;

import com.grizmtech.oneblock.GrizmOneBlock;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

public class VoidProtectionListener implements Listener {

    private final GrizmOneBlock plugin;

    public VoidProtectionListener(GrizmOneBlock plugin) {
        this.plugin = plugin;
    }

    private UUID getIslandOwner(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        String name = loc.getWorld().getName();
        if (name.startsWith("island_")) {
            try {
                return UUID.fromString(name.replace("island_", ""));
            } catch (Exception ignored) {}
        }
        return null;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        Player player = event.getPlayer();
        Location to = event.getTo();
        if (to.getWorld() == null) return;

        if (to.getWorld().getName().startsWith("island_") && to.getY() < -10) {
            UUID owner = getIslandOwner(to);
            Location oneBlock = plugin.getDataManager().getOneBlockLocation(owner != null ? owner : player.getUniqueId());
            if (oneBlock != null && oneBlock.getWorld() != null) {
                Location safeTp = oneBlock.clone().add(0.5, 1.2, 0.5);
                player.teleport(safeTp);
                player.setFallDistance(0);

                // Yavaş Düşüş ve efekt ver
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 60, 0));
                player.damage(2.0); // 1 kalp hasar
                player.playSound(safeTp, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.2f);
                safeTp.getWorld().spawnParticle(Particle.PORTAL, safeTp, 25, 0.5, 0.5, 0.5, 0.1);

                player.sendActionBar(Component.text("Boşluğa düştüğünüz için adaya geri ışınlandınız!").color(NamedTextColor.YELLOW));
            }
        }
    }

    @EventHandler
    public void onEntityVoidDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.VOID) return;
        Entity entity = event.getEntity();
        if (entity instanceof Player) return; // Oyuncu PlayerMoveEvent'te yakalanıyor

        Location loc = entity.getLocation();
        if (loc.getWorld() == null || !loc.getWorld().getName().startsWith("island_")) return;

        UUID owner = getIslandOwner(loc);
        if (owner == null) return;

        Location oneBlock = plugin.getDataManager().getOneBlockLocation(owner);
        if (oneBlock != null && oneBlock.getWorld() != null) {
            event.setCancelled(true);
            Location safe = oneBlock.clone().add(0.5, 1.2, 0.5);
            entity.teleport(safe);
            entity.setFallDistance(0);
        }
    }
}
