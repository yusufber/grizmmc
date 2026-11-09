package com.grizmtech.auth.listeners;

import com.grizmtech.auth.GrizmAuth;
import com.grizmtech.auth.api.AuthApiClient;
import com.grizmtech.auth.api.AuthVerifyResult;
import com.grizmtech.auth.config.Settings;
import com.grizmtech.auth.session.SessionManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

public class PreLoginListener implements Listener {

    private final GrizmAuth plugin;
    private final Settings settings;
    private final AuthApiClient apiClient;
    private final SessionManager sessionManager;

    public PreLoginListener(GrizmAuth plugin) {
        this.plugin = plugin;
        this.settings = plugin.getSettings();
        this.apiClient = plugin.getApiClient();
        this.sessionManager = plugin.getSessionManager();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        String username = event.getName();
        String ip = event.getAddress().getHostAddress();

        // Whitelist / Bypass kontrolü
        if (settings.whitelistBypass.contains(username.toLowerCase())) {
            plugin.getLogger().info("[GrizmAuth] Bypass listesindeki yetkili dogrulandi: " + username);
            return;
        }

        try {
            // Web API sorgusunu asenkron olarak tamamla (AsyncPreLogin thread'i bloklanabilir)
            AuthVerifyResult result = apiClient.verifyPlayerJoinAsync(username, ip).join();

            if (result != null && result.isSuccess() && result.isAuthenticated()) {
                // Launcher oturumu basarili!
                sessionManager.cachePreAuth(username, result.getProfile());
                plugin.getLogger().info("[GrizmAuth] " + username + " Launcher oturumu basariyla dogrulandi.");
                return;
            }

            // Launcher zorunlu ise girisi engelle (Kick)
            if (settings.requireLauncher) {
                String kickMsg;
                if (result != null && !result.isRegisteredOnWeb()) {
                    kickMsg = settings.kickNotRegistered;
                } else {
                    kickMsg = settings.kickLauncherRequired;
                }

                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, kickMsg);
                plugin.getLogger().warning("[GrizmAuth] " + username + " Launcher oturumu olmadan baglanmaya calisti (Reddedildi).");
            } else {
                // Fallback mod: Oyuncunun girmesine izin ver, join eventinde sifre/kod iste
                plugin.getLogger().info("[GrizmAuth] " + username + " Launcher olmadan baglandi, oyun ici dogrulama bekleniyor.");
            }
        } catch (Exception ex) {
            plugin.getLogger().severe("[GrizmAuth] PreLogin dogrulama hatasi (" + username + "): " + ex.getMessage());
            if (settings.requireLauncher) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "§cKimlik dogrulama sunucusuna baglanilamadi. Lutfen biraz sonra tekrar deneyin.");
            }
        }
    }
}
