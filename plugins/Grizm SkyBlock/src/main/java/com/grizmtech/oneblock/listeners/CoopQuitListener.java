package com.grizmtech.oneblock.listeners;

import com.grizmtech.oneblock.GrizmOneBlock;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class CoopQuitListener implements Listener {

    private final GrizmOneBlock plugin;

    public CoopQuitListener(GrizmOneBlock plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // 1. Çıkan oyuncunun geçici Co-op yetkilerini temizle
        plugin.getRoleManager().clearCoopsForPlayer(uuid);

        // 2. Eğer çıkan oyuncu bir ada lideriyse, adasındaki tüm geçici Co-op'ları iptal et
        plugin.getRoleManager().clearCoopsForOwner(uuid);

        // Adasında bulunan geçici misafirleri spawna gönder
        World islandWorld = Bukkit.getWorld("island_" + uuid.toString());
        if (islandWorld != null) {
            World mainWorld = Bukkit.getWorlds().get(0);
            for (Player p : islandWorld.getPlayers()) {
                if (!p.getUniqueId().equals(uuid) && !p.hasPermission("grizmoneblock.admin")) {
                    if (mainWorld != null) {
                        p.teleport(mainWorld.getSpawnLocation());
                    }
                    p.sendMessage(Component.text("Ada lideri oyundan ayrıldığı için spawna ışınlandınız!").color(NamedTextColor.YELLOW));
                }
            }
        }
    }
}
