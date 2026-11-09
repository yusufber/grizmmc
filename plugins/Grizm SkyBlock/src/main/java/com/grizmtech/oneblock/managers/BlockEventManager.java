package com.grizmtech.oneblock.managers;

import com.grizmtech.oneblock.GrizmOneBlock;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BlockEventManager {

    public enum EventType {
        RUSH_HOUR("⚡ Maden Bereketi (3x Maden!)", BossBar.Color.YELLOW),
        HASTE_BURST("⚡ Hız Patlaması (Acele III)", BossBar.Color.GREEN),
        XP_BURST("⚡ XP Fırtınası (5x Deneyim!)", BossBar.Color.PURPLE);

        private final String displayName;
        private final BossBar.Color barColor;

        EventType(String displayName, BossBar.Color barColor) {
            this.displayName = displayName;
            this.barColor = barColor;
        }

        public String getDisplayName() { return displayName; }
        public BossBar.Color getBarColor() { return barColor; }
    }

    private final GrizmOneBlock plugin;
    private final Map<UUID, EventType> activeEvents = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar> activeBossBars = new ConcurrentHashMap<>();

    public BlockEventManager(GrizmOneBlock plugin) {
        this.plugin = plugin;
    }

    public boolean isEventActive(UUID ownerUUID, EventType type) {
        return activeEvents.get(ownerUUID) == type;
    }

    public boolean hasAnyEvent(UUID ownerUUID) {
        return activeEvents.containsKey(ownerUUID);
    }

    public void triggerRandomEvent(UUID ownerUUID, Player breaker) {
        if (hasAnyEvent(ownerUUID)) return;

        EventType[] values = EventType.values();
        EventType chosen = values[java.util.concurrent.ThreadLocalRandom.current().nextInt(values.length)];

        activeEvents.put(ownerUUID, chosen);

        BossBar bar = BossBar.bossBar(
                Component.text(chosen.getDisplayName()).color(NamedTextColor.GOLD),
                1.0f,
                chosen.getBarColor(),
                BossBar.Overlay.PROGRESS
        );
        activeBossBars.put(ownerUUID, bar);

        World world = Bukkit.getWorld("island_" + ownerUUID.toString());
        if (world != null) {
            for (Player p : world.getPlayers()) {
                p.showBossBar(bar);
                p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.8f);
            }
        }

        if (chosen == EventType.HASTE_BURST && breaker != null) {
            breaker.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 600, 2));
            breaker.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 600, 1));
        }

        // 30 saniyelik geri sayım sayacı
        new BukkitRunnable() {
            int secondsLeft = 30;

            @Override
            public void run() {
                secondsLeft--;
                if (secondsLeft <= 0 || !activeEvents.containsKey(ownerUUID)) {
                    stopEvent(ownerUUID);
                    cancel();
                    return;
                }

                bar.progress((float) secondsLeft / 30.0f);
                bar.name(Component.text(chosen.getDisplayName() + " [" + secondsLeft + "s]").color(NamedTextColor.GOLD));
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public void stopEvent(UUID ownerUUID) {
        activeEvents.remove(ownerUUID);
        BossBar bar = activeBossBars.remove(ownerUUID);
        if (bar != null) {
            World world = Bukkit.getWorld("island_" + ownerUUID.toString());
            if (world != null) {
                for (Player p : world.getPlayers()) {
                    p.hideBossBar(bar);
                }
            }
        }
    }
}
