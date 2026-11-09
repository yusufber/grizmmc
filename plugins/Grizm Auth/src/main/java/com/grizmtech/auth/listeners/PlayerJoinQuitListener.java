package com.grizmtech.auth.listeners;

import com.grizmtech.auth.GrizmAuth;
import com.grizmtech.auth.config.Settings;
import com.grizmtech.auth.model.UserProfileData;
import com.grizmtech.auth.session.SessionManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PlayerJoinQuitListener implements Listener {

    private final GrizmAuth plugin;
    private final Settings settings;
    private final SessionManager sessionManager;

    public PlayerJoinQuitListener(GrizmAuth plugin) {
        this.plugin = plugin;
        this.settings = plugin.getSettings();
        this.sessionManager = plugin.getSessionManager();
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String username = player.getName();

        // 1. PreLogin'de doğrulanmış Launcher oturumunu kontrol et
        UserProfileData profile = sessionManager.claimPreAuth(username);

        if (profile != null) {
            // Launcher oturumu onaylandı!
            sessionManager.authenticate(player, profile);

            // Başarı Ses Efekti
            playSound(player, settings.soundSuccess);

            // Başlık & Alt Başlık Animasyonu (10 fade-in, 60 stay, 20 fade-out)
            player.sendTitle(settings.welcomeTitle, settings.welcomeSubtitle, 10, 60, 20);

            // Aksiyon Çubuğu (Action Bar)
            if (settings.actionbarVerified != null && !settings.actionbarVerified.isEmpty()) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(settings.actionbarVerified));
            }
            return;
        }

        // 2. Bypass kontrolü
        if (player.hasPermission("grizmauth.bypass") || settings.whitelistBypass.contains(username.toLowerCase())) {
            UserProfileData bypassProfile = new UserProfileData(username, "", username, "", "BYPASS");
            sessionManager.authenticate(player, bypassProfile);
            player.sendMessage(settings.prefix + "§7Yetkili erisimi: Launcher dogrulamasi atlandi.");
            return;
        }

        // 3. Fallback: Oyun içi kod doğrulama modu
        sessionManager.startPendingLogin(player.getUniqueId());

        if (settings.blindness) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * settings.loginTimeoutSeconds, 1, false, false));
        }

        player.sendMessage(settings.codePrompt);
        playSound(player, settings.soundPrompt);

        // Zaman aşımı kontrolü (Süre dolunca kick at)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && !sessionManager.isAuthenticated(player)) {
                player.kickPlayer(settings.timeoutKick);
            }
        }, 20L * settings.loginTimeoutSeconds);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        sessionManager.removePlayer(player.getUniqueId());
        player.removePotionEffect(PotionEffectType.BLINDNESS);

        // Web API'ye çıkış bildir
        plugin.getApiClient().notifyPlayerQuitAsync(player.getName());
    }

    private void playSound(Player player, String soundName) {
        if (soundName == null || soundName.isEmpty()) return;
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (Exception ignored) {
        }
    }
}
