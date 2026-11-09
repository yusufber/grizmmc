package com.grizmtech.towny.combat;

import com.grizmtech.towny.core.MessageUtil;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CombatTagManager {

    private final JavaPlugin plugin;
    private final Map<UUID, Long> combatEndTimes = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar> bossBars = new ConcurrentHashMap<>();
    private BukkitTask tickerTask;

    public CombatTagManager(JavaPlugin plugin) {
        this.plugin = plugin;
        startTicker();
    }

    private void startTicker() {
        this.tickerTask = new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                for (Map.Entry<UUID, Long> entry : combatEndTimes.entrySet()) {
                    UUID uuid = entry.getKey();
                    long endTime = entry.getValue();
                    Player player = Bukkit.getPlayer(uuid);

                    if (now >= endTime || player == null || !player.isOnline()) {
                        combatEndTimes.remove(uuid);
                        removeBossBar(uuid, player);
                        if (player != null && player.isOnline()) {
                            MessageUtil.sendMessage(player, "<green>Savaş modundan çıktınız. Artık ışınlanabilirsiniz.</green>");
                        }
                    } else {
                        // BossBar güncelle
                        int remainingSec = (int) Math.ceil((endTime - now) / 1000.0);
                        float progress = Math.min(1.0f, Math.max(0.0f, (float) (endTime - now) / 15000f));
                        BossBar bar = bossBars.computeIfAbsent(uuid, k -> {
                            BossBar b = BossBar.bossBar(
                                    MessageUtil.parse("<red>⚔ Savaş Modu: <white>" + remainingSec + "s</white> <gray>(Kaçış Kilitli)</gray>"),
                                    progress,
                                    BossBar.Color.RED,
                                    BossBar.Overlay.PROGRESS
                            );
                            player.showBossBar(b);
                            return b;
                        });
                        bar.name(MessageUtil.parse("<red>⚔ Savaş Modu: <white>" + remainingSec + "s</white> <gray>(Kaçış Kilitli)</gray>"));
                        bar.progress(progress);
                    }
                }
            }
        }.runTaskTimer(plugin, 10L, 10L);
    }

    public void tagPlayer(Player player, int seconds) {
        if (player == null || !player.isOnline()) return;
        UUID uuid = player.getUniqueId();
        long endTime = System.currentTimeMillis() + (seconds * 1000L);
        boolean wasTagged = combatEndTimes.containsKey(uuid);

        combatEndTimes.put(uuid, endTime);

        if (!wasTagged) {
            MessageUtil.sendMessage(player, "<red><b>Savaşa girdiniz!</b> <yellow>" + seconds + " saniye</yellow> boyunca güvenli alana kaçamaz veya ışınlanamazsınız!");
        }
    }

    public boolean isTagged(UUID uuid) {
        Long end = combatEndTimes.get(uuid);
        return end != null && System.currentTimeMillis() < end;
    }

    public int getRemainingSeconds(UUID uuid) {
        Long end = combatEndTimes.get(uuid);
        if (end == null) return 0;
        long diff = end - System.currentTimeMillis();
        return diff > 0 ? (int) Math.ceil(diff / 1000.0) : 0;
    }

    public void removeTag(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        combatEndTimes.remove(uuid);
        removeBossBar(uuid, player);
    }

    private void removeBossBar(UUID uuid, Player player) {
        BossBar bar = bossBars.remove(uuid);
        if (bar != null && player != null) {
            player.hideBossBar(bar);
        }
    }

    public void shutdown() {
        if (tickerTask != null) {
            tickerTask.cancel();
        }
        for (Map.Entry<UUID, BossBar> entry : bossBars.entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p != null) {
                p.hideBossBar(entry.getValue());
            }
        }
        bossBars.clear();
        combatEndTimes.clear();
    }
}
