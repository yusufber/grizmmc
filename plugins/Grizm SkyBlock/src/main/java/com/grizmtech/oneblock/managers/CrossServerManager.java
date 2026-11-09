package com.grizmtech.oneblock.managers;

import com.grizmtech.oneblock.GrizmOneBlock;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class CrossServerManager implements Listener {

    private final GrizmOneBlock plugin;

    public CrossServerManager(GrizmOneBlock plugin) {
        this.plugin = plugin;
    }

    public void broadcastNetworkMessage(String channel, String message) {
        // Multi-server BungeeCord / Velocity Plugin Messaging kanalı hazır yapısı
        try {
            if (!Bukkit.getOnlinePlayers().isEmpty()) {
                Player p = Bukkit.getOnlinePlayers().iterator().next();
                p.sendPluginMessage(plugin, "BungeeCord", message.getBytes());
            }
        } catch (Exception ignored) {}
    }

    /**
     * RAM Optimizasyonu: Bir adada hiç oyuncu kalmadığında dünyayı güvenle kaydeder.
     */
    @EventHandler
    public void onPlayerLeaveWorld(PlayerQuitEvent event) {
        World world = event.getPlayer().getWorld();
        if (world.getName().startsWith("island_")) {
            // Eğer dünyada başka kimse kalmadıysa chunk'ları boşalt ve dünyayı kaydet
            if (world.getPlayers().size() <= 1) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (world.getPlayers().isEmpty()) {
                        world.save();
                    }
                }, 100L); // 5 saniye sonra güvenle kaydet
            }
        }
    }
}
