package com.grizmtech.perms;

import com.grizmtech.perms.command.GrizmPermCommand;
import com.grizmtech.perms.command.VipInfoCommand;
import com.grizmtech.perms.engine.PermissionManager;
import com.grizmtech.perms.hook.GrizmPermsExpansion;
import com.grizmtech.perms.hook.VaultHook;
import com.grizmtech.perms.listener.ChatFormatListener;
import com.grizmtech.perms.listener.PlayerPermListener;
import com.grizmtech.perms.listener.SubServerLobbyListener;
import com.grizmtech.perms.sync.WebSyncService;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * GrizmPerms - GrizmMC Network Merkezi Rol, İzin, Süreli VIP & Web Mağaza Eklentisi
 */
public class GrizmPermsPlugin extends JavaPlugin {

    private PermissionManager permissionManager;
    private WebSyncService syncService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.permissionManager = new PermissionManager(this);
        this.permissionManager.initialize();

        this.syncService = new WebSyncService(this, permissionManager);

        // Vault servis kaydı (Essentials, TAB, GrizmOneBlock, GrizmTowny uyumu için)
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            try {
                VaultHook.register(this, permissionManager);
            } catch (Throwable t) {
                getLogger().warning("[GrizmPerms] Vault kancası başlatılamadı: " + t.getMessage());
            }
        }

        // PlaceholderAPI kaydı
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                new GrizmPermsExpansion(this, permissionManager).register();
                getLogger().info("[GrizmPerms] PlaceholderAPI (%grizmperms_...%) entegrasyonu aktif.");
            } catch (Throwable t) {
                getLogger().warning("[GrizmPerms] PlaceholderAPI entegrasyonu başlatılamadı: " + t.getMessage());
            }
        }

        // Dinleyiciler
        Bukkit.getPluginManager().registerEvents(new PlayerPermListener(this, permissionManager, syncService), this);
        Bukkit.getPluginManager().registerEvents(new ChatFormatListener(this, permissionManager), this);
        SubServerLobbyListener lobbyListener = new SubServerLobbyListener(this, permissionManager);
        Bukkit.getPluginManager().registerEvents(lobbyListener, this);
        Bukkit.getScheduler().runTask(this, lobbyListener::getOrLoadLobbyWorld);

        // Komutlar
        GrizmPermCommand adminCmd = new GrizmPermCommand(this, permissionManager, syncService);
        PluginCommand gperm = getCommand("gperm");
        if (gperm != null) {
            gperm.setExecutor(adminCmd);
            gperm.setTabCompleter(adminCmd);
        }

        PluginCommand vipCmd = getCommand("vipbilgi");
        if (vipCmd != null) {
            vipCmd.setExecutor(new VipInfoCommand(this, permissionManager));
        }

        // Sunucuda halihazırda çevrimiçi olan oyuncular varsa (reload durumu) enjekte et
        for (Player online : Bukkit.getOnlinePlayers()) {
            permissionManager.refreshPlayer(online);
        }

        // İlk açılış senkronizasyonu + Periyodik Web senkronizasyonu
        int pollSec = Math.max(5, getConfig().getInt("sync.poll-interval-seconds", 15));
        Bukkit.getScheduler().runTaskTimer(this, () -> syncService.syncAllAsync(false, null), 20L, pollSec * 20L);

        // Süreli VIP sona erme kontrol görevi (her 20 saniyede bir)
        Bukkit.getScheduler().runTaskTimer(this, permissionManager::checkExpiredRoles, 200L, 400L);

        getLogger().info("==================================================");
        getLogger().info(" GrizmPerms v" + getDescription().getVersion() + " aktif! Sunucu Kimliği: " + permissionManager.getServerId());
        getLogger().info(" Yüklü Rol Sayısı: " + permissionManager.getRolesMap().size() + " | Web Sync Aralığı: " + pollSec + "sn");
        getLogger().info("==================================================");
    }

    @Override
    public void onDisable() {
        if (permissionManager != null) {
            permissionManager.saveLocalCache();
            permissionManager.cleanupAll();
        }
    }

    public PermissionManager getPermissionManager() {
        return permissionManager;
    }

    public WebSyncService getSyncService() {
        return syncService;
    }
}

