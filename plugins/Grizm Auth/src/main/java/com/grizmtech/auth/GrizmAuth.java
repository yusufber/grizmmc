package com.grizmtech.auth;

import com.grizmtech.auth.api.AuthApiClient;
import com.grizmtech.auth.commands.GrizmAuthCommand;
import com.grizmtech.auth.commands.LoginCommand;
import com.grizmtech.auth.config.Settings;
import com.grizmtech.auth.listeners.PlayerJoinQuitListener;
import com.grizmtech.auth.listeners.PlayerRestrictionListener;
import com.grizmtech.auth.listeners.PreLoginListener;
import com.grizmtech.auth.session.SessionManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class GrizmAuth extends JavaPlugin {

    private static GrizmAuth instance;
    private Settings settings;
    private AuthApiClient apiClient;
    private SessionManager sessionManager;

    @Override
    public void onEnable() {
        instance = this;

        // Banner Log
        getLogger().info("========================================");
        getLogger().info("  GRIZMMC NETWORK - GRIZM AUTH v" + getDescription().getVersion());
        getLogger().info("  Launcher & Web API Kimlik Dogrulama");
        getLogger().info("========================================");

        // 1. Yapılandırma
        saveDefaultConfig();
        settings = new Settings();
        settings.load(getConfig());

        // 2. Modüller
        apiClient = new AuthApiClient(this, settings);
        sessionManager = new SessionManager();

        // 3. Olay Dinleyicileri (Listeners)
        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new PreLoginListener(this), this);
        pm.registerEvents(new PlayerJoinQuitListener(this), this);
        pm.registerEvents(new PlayerRestrictionListener(this), this);

        // 4. Komutlar
        GrizmAuthCommand authCmd = new GrizmAuthCommand(this);
        LoginCommand loginCmd = new LoginCommand(this);

        if (getCommand("grizmauth") != null) {
            getCommand("grizmauth").setExecutor(authCmd);
            getCommand("grizmauth").setTabCompleter(authCmd);
        }

        if (getCommand("login") != null) {
            getCommand("login").setExecutor(loginCmd);
        }

        if (getCommand("link") != null) {
            getCommand("link").setExecutor(loginCmd);
        }

        // 5. Başlangıç Web API Kontrolü
        apiClient.checkHealthAsync().thenAccept(online -> {
            if (online) {
                getLogger().info("[GrizmAuth] Web API baglantisi basarili: " + settings.apiUrl);
            } else {
                getLogger().warning("[GrizmAuth] Web API sunucusuna su an erisilemiyor: " + settings.apiUrl);
            }
        });

        getLogger().info("[GrizmAuth] Eklenti aktif! Launcher zorunlulugu: " + (settings.requireLauncher ? "EVET" : "HAYIR"));
    }

    @Override
    public void onDisable() {
        getLogger().info("[GrizmAuth] Eklenti devredisi birakildi.");
        instance = null;
    }

    public static GrizmAuth getInstance() {
        return instance;
    }

    public Settings getSettings() {
        return settings;
    }

    public AuthApiClient getApiClient() {
        return apiClient;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }
}
