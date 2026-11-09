package com.grizmtech.auth.session;

import com.grizmtech.auth.model.UserProfileData;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    private final Map<UUID, UserProfileData> authenticatedPlayers = new ConcurrentHashMap<>();
    private final Map<String, UserProfileData> preAuthenticatedProfiles = new ConcurrentHashMap<>();
    private final Map<UUID, Long> pendingLoginTimers = new ConcurrentHashMap<>();

    /**
     * AsyncPreLogin sırasında başarıyla doğrulanan profili geçici olarak saklar.
     */
    public void cachePreAuth(String username, UserProfileData profile) {
        preAuthenticatedProfiles.put(username.toLowerCase(), profile);
    }

    /**
     * Oyuncu oyuna girdiğinde (PlayerJoinEvent) önceden doğrulanan profili alır.
     */
    public UserProfileData claimPreAuth(String username) {
        return preAuthenticatedProfiles.remove(username.toLowerCase());
    }

    /**
     * Oyuncuyu doğrulanmış olarak işaretler.
     */
    public void authenticate(Player player, UserProfileData profile) {
        authenticatedPlayers.put(player.getUniqueId(), profile);
        pendingLoginTimers.remove(player.getUniqueId());
    }

    public boolean isAuthenticated(Player player) {
        return isAuthenticated(player.getUniqueId());
    }

    public boolean isAuthenticated(UUID uuid) {
        return authenticatedPlayers.containsKey(uuid);
    }

    public UserProfileData getProfile(UUID uuid) {
        return authenticatedPlayers.get(uuid);
    }

    public void removePlayer(UUID uuid) {
        authenticatedPlayers.remove(uuid);
        pendingLoginTimers.remove(uuid);
    }

    public void startPendingLogin(UUID uuid) {
        pendingLoginTimers.put(uuid, System.currentTimeMillis());
    }

    public boolean isPending(UUID uuid) {
        return pendingLoginTimers.containsKey(uuid);
    }

    public Long getPendingJoinTime(UUID uuid) {
        return pendingLoginTimers.get(uuid);
    }

    public int getAuthenticatedCount() {
        return authenticatedPlayers.size();
    }
}
