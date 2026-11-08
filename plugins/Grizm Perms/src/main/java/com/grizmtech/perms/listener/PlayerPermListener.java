package com.grizmtech.perms.listener;

import com.grizmtech.perms.GrizmPermsPlugin;
import com.grizmtech.perms.engine.PermissionManager;
import com.grizmtech.perms.sync.WebSyncService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;

public class PlayerPermListener implements Listener {

    private final GrizmPermsPlugin plugin;
    private final PermissionManager manager;
    private final WebSyncService syncService;

    public PlayerPermListener(GrizmPermsPlugin plugin, PermissionManager manager, WebSyncService syncService) {
        this.plugin = plugin;
        this.manager = manager;
        this.syncService = syncService;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        manager.refreshPlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        manager.refreshPlayer(player);

        // Girişte web üzerindeki son bakiye/VIP/teslimat verisini asenkron çek
        syncService.syncSinglePlayerAsync(player.getName());

        // TAB eklentisi ve özel Scoreboard'lar yüklendikten sonra baş üstü (3D Nametag) & TAB listesini tekrar zorla
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                manager.getNametagAndTabManager().updatePlayer(player);
            }
        }, 12L);

        // Dünya yüklemesi tamamlandıktan 1.5 saniye sonra bekleyen VIP kutlaması varsa tetikle ve TAB'ı tazele
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                manager.getNametagAndTabManager().updateAllOnlinePlayers();
                syncService.processDeliveriesForPlayer(player);
            }
        }, 30L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        manager.cleanupPlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFireworkDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Firework fw) {
            if (fw.getPersistentDataContainer().has(syncService.getFireworkKey(), PersistentDataType.BYTE)) {
                event.setCancelled(true);
            }
        }
    }
}

