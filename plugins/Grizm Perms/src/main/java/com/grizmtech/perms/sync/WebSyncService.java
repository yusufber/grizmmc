package com.grizmtech.perms.sync;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.grizmtech.perms.GrizmPermsPlugin;
import com.grizmtech.perms.engine.PermissionManager;
import com.grizmtech.perms.model.PendingDelivery;
import com.grizmtech.perms.model.RewardCommand;
import com.grizmtech.perms.model.Role;
import com.grizmtech.perms.model.UserData;
import com.grizmtech.perms.util.ColorUtil;
import com.grizmtech.perms.util.JsonHelper;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Web Sitesi (perms.php API veya paylaşımlı web/data klasörü) ile çift yönlü senkronizasyon
 * ve VIP satın alım kutlama / ödül dağıtım servisi.
 */
public class WebSyncService {

    public static final String FIREWORK_PDC_KEY = "grizmperms_celebration_fw";

    private final GrizmPermsPlugin plugin;
    private final PermissionManager manager;
    private final HttpClient httpClient;
    private final NamespacedKey fireworkKey;

    private final Set<String> celebratedIds = ConcurrentHashMap.newKeySet();
    private final Set<String> executedDeliveryServerKeys = ConcurrentHashMap.newKeySet();

    public WebSyncService(GrizmPermsPlugin plugin, PermissionManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.fireworkKey = new NamespacedKey(plugin, FIREWORK_PDC_KEY);
        int timeoutSec = Math.max(2, plugin.getConfig().getInt("sync.timeout-seconds", 5));
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSec))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public NamespacedKey getFireworkKey() {
        return fireworkKey;
    }

    private String getMode() {
        return plugin.getConfig().getString("sync.mode", "auto").trim().toLowerCase();
    }

    private String getServerKey() {
        return plugin.getConfig().getString("sync.server-key", "").trim();
    }

    private File resolveLocalWebDataDir() {
        String rel = plugin.getConfig().getString("sync.local-web-data-dir", "../../../web/data");
        if (rel == null || rel.isBlank()) return null;
        File dir = new File(rel);
        if (!dir.isAbsolute()) {
            // Sunucu çalışma dizinine göre çöz
            dir = new File(System.getProperty("user.dir"), rel);
        }
        try {
            dir = dir.getCanonicalFile();
        } catch (Exception ignored) {
        }
        return (dir.exists() && dir.isDirectory()) ? dir : null;
    }

    // =========================================================================
    // 1. TAM VEYA REVİZYON BAZLI SENKRONİZASYON
    // =========================================================================

    public void syncAllAsync(boolean forceFull, Consumer<Boolean> callbackMainThread) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean ok = performSyncAllBlocking(forceFull);
            Bukkit.getScheduler().runTask(plugin, () -> {
                processDeliveriesForOnlinePlayers();
                if (callbackMainThread != null) {
                    callbackMainThread.accept(ok);
                }
            });
        });
    }

    public void syncSinglePlayerAsync(String username) {
        if (username == null || username.isBlank()) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String mode = getMode();
            boolean handledViaHttp = false;

            if (!mode.equals("local")) {
                String encoded = URLEncoder.encode(username.trim(), StandardCharsets.UTF_8);
                JsonObject resp = sendGetJson("action=get_player&username=" + encoded);
                if (resp != null && "success".equalsIgnoreCase(getString(resp, "status"))) {
                    handledViaHttp = true;
                    long rev = resp.has("revision") ? resp.get("revision").getAsLong() : manager.getCurrentRevision();
                    if (resp.has("user") && resp.get("user").isJsonObject()) {
                        UserData u = JsonHelper.GSON.fromJson(resp.get("user"), UserData.class);
                        if (u != null) {
                            manager.updateSingleUser(u, rev);
                        }
                    }
                }
            }

            if (!handledViaHttp && !mode.equals("http")) {
                performLocalFileSync(false);
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                Player online = Bukkit.getPlayerExact(username);
                if (online != null && online.isOnline()) {
                    manager.refreshPlayer(online);
                    processDeliveriesForPlayer(online);
                }
            });
        });
    }

    public boolean performSyncAllBlocking(boolean forceFull) {
        String mode = getMode();
        long sinceRev = forceFull ? 0L : manager.getCurrentRevision();

        if (!mode.equals("local")) {
            JsonObject resp = sendGetJson("action=sync_all&since_revision=" + sinceRev);
            if (resp != null) {
                String status = getString(resp, "status");
                if ("up_to_date".equalsIgnoreCase(status)) {
                    return true;
                }
                if ("success".equalsIgnoreCase(status)) {
                    long rev = resp.has("revision") ? resp.get("revision").getAsLong() : 1L;
                    String defId = getString(resp, "default_role_id");
                    Map<String, Role> parsedRoles = parseRolesElement(resp.get("roles"));
                    Map<String, UserData> parsedUsers = parseUsersElement(resp.get("users"));
                    manager.applySnapshot(parsedRoles, parsedUsers, defId, rev);
                    return true;
                }
            }
        }

        if (!mode.equals("http")) {
            return performLocalFileSync(forceFull);
        }

        return false;
    }

    private boolean performLocalFileSync(boolean forceFull) {
        File dataDir = resolveLocalWebDataDir();
        if (dataDir == null) return false;

        File rolesFile = new File(dataDir, "roles.json");
        if (!rolesFile.exists()) return false;

        try {
            long diskRev = 1L;
            File metaFile = new File(dataDir, "sync_meta.json");
            if (metaFile.exists()) {
                try (Reader r = Files.newBufferedReader(metaFile.toPath(), StandardCharsets.UTF_8)) {
                    JsonObject meta = JsonParser.parseReader(r).getAsJsonObject();
                    if (meta.has("revision")) {
                        diskRev = meta.get("revision").getAsLong();
                    }
                }
            }

            if (!forceFull && diskRev > 0 && diskRev == manager.getCurrentRevision()) {
                return true;
            }

            Map<String, Role> parsedRoles;
            try (Reader r = Files.newBufferedReader(rolesFile.toPath(), StandardCharsets.UTF_8)) {
                parsedRoles = parseRolesElement(JsonParser.parseReader(r));
            }

            Map<String, UserData> parsedUsers = new HashMap<>();
            File usersFile = new File(dataDir, "user_perms.json");
            if (usersFile.exists()) {
                try (Reader r = Files.newBufferedReader(usersFile.toPath(), StandardCharsets.UTF_8)) {
                    parsedUsers = parseUsersElement(JsonParser.parseReader(r));
                }
            }

            manager.applySnapshot(parsedRoles, parsedUsers, null, diskRev);
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[GrizmPerms] Yerel web/data senkronizasyon hatası: " + e.getMessage());
            return false;
        }
    }

    // =========================================================================
    // 2. OYUN İÇİ /GPERM DEĞİŞİKLİKLERİNİ WEB'E YAZMA (ÇİFT YÖNLÜ SENKRON)
    // =========================================================================

    public void pushMutationAsync(JsonObject mutationPayload) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            mutationPayload.addProperty("action", "game_mutation");
            String mode = getMode();
            boolean sentHttp = false;

            if (!mode.equals("local")) {
                JsonObject resp = sendPostJson("action=game_mutation", mutationPayload);
                if (resp != null && "success".equalsIgnoreCase(getString(resp, "status"))) {
                    sentHttp = true;
                    if (resp.has("revision")) {
                        manager.setCurrentRevision(resp.get("revision").getAsLong());
                        manager.saveLocalCache();
                    }
                }
            }

            if (!sentHttp && !mode.equals("http")) {
                writeSnapshotToLocalWebDir();
            }
        });
    }

    private synchronized void writeSnapshotToLocalWebDir() {
        File dataDir = resolveLocalWebDataDir();
        if (dataDir == null) return;
        try {
            long nextRev = manager.getCurrentRevision() + 1L;
            manager.setCurrentRevision(nextRev);

            File rolesFile = new File(dataDir, "roles.json");
            try (Writer w = Files.newBufferedWriter(rolesFile.toPath(), StandardCharsets.UTF_8)) {
                JsonHelper.GSON.toJson(manager.getRolesMap(), w);
            }

            File usersFile = new File(dataDir, "user_perms.json");
            try (Writer w = Files.newBufferedWriter(usersFile.toPath(), StandardCharsets.UTF_8)) {
                JsonHelper.GSON.toJson(manager.getUsersMap(), w);
            }

            File metaFile = new File(dataDir, "sync_meta.json");
            JsonObject meta = new JsonObject();
            meta.addProperty("revision", nextRev);
            meta.addProperty("updated_at", System.currentTimeMillis() / 1000L);
            meta.addProperty("last_reason", "ingame_sync_" + manager.getServerId());
            try (Writer w = Files.newBufferedWriter(metaFile.toPath(), StandardCharsets.UTF_8)) {
                JsonHelper.GSON.toJson(meta, w);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[GrizmPerms] Yerel web/data yazma hatası: " + e.getMessage());
        }
    }

    // =========================================================================
    // 3. VIP SATIN ALIM OYUN İÇİ KUTLAMA VE ÖDÜL TESLİMAT MOTORU
    // =========================================================================

    public void processDeliveriesForOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            processDeliveriesForPlayer(player);
        }
    }

    public void processDeliveriesForPlayer(Player player) {
        if (player == null || !player.isOnline()) return;
        UserData user = manager.getUserData(player.getName());
        if (user == null || user.getPendingDeliveries().isEmpty()) return;

        String srvId = manager.getServerId();
        List<PendingDelivery> snapshot = new ArrayList<>(user.getPendingDeliveries());

        for (PendingDelivery del : snapshot) {
            if (del.getId().isEmpty()) continue;

            Role role = manager.getRole(del.getRoleId());
            String roleDisplay = role != null ? role.getName() : del.getRoleName();

            boolean didCelebrateNow = false;
            boolean didRunCommandsNow = false;

            // 1. Kutlama (Title, Ses, Havai Fişek, Sunucu Duyurusu) -> Yalnızca 1 kez (Eğer rol teslimatı ise)
            if (role != null && !del.isCelebrated() && celebratedIds.add(del.getId())) {
                del.setCelebrated(true);
                didCelebrateNow = true;
                triggerVipCelebration(player, roleDisplay);
            } else if (role == null && !del.isCelebrated()) {
                del.setCelebrated(true);
            }

            // 2. Sunucuya Özel Ödül Komutları VEYA Doğrudan Teslimat Komutları
            String execTrackKey = del.getId() + ":" + srvId;
            List<RewardCommand> candidateCmds = new ArrayList<>();
            if (role != null && !role.getVipRewardCommands().isEmpty()) {
                candidateCmds.addAll(role.getVipRewardCommands());
            }
            if (del.getCommands() != null && !del.getCommands().isEmpty()) {
                candidateCmds.addAll(del.getCommands());
            }

            if (!candidateCmds.isEmpty()
                    && !del.getExecutedServers().contains(srvId)
                    && executedDeliveryServerKeys.add(execTrackKey)) {

                for (RewardCommand rc : candidateCmds) {
                    String targetSrv = rc.getServer();
                    boolean shouldRun = false;

                    if ("first".equalsIgnoreCase(targetSrv) && !del.getExecutedServers().contains("first")) {
                        shouldRun = true;
                        del.getExecutedServers().add("first");
                    } else if ("all".equalsIgnoreCase(targetSrv) || srvId.equalsIgnoreCase(targetSrv)) {
                        shouldRun = true;
                    }

                    if (shouldRun && !rc.getCommand().isEmpty()) {
                        String finalCmd = rc.getCommand().replace("{player}", player.getName());
                        if (finalCmd.startsWith("/")) {
                            finalCmd = finalCmd.substring(1);
                        }
                        try {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
                            didRunCommandsNow = true;
                        } catch (Exception ex) {
                            plugin.getLogger().log(Level.WARNING, "[GrizmPerms] Teslimat komutu hatası: " + finalCmd, ex);
                        }
                    }
                }

                if (didRunCommandsNow && !del.getExecutedServers().contains(srvId)) {
                    del.getExecutedServers().add(srvId);
                }
            }

            if (didCelebrateNow || didRunCommandsNow) {
                acknowledgeDeliveryAsync(player.getName(), del.getId(), srvId, del.isCelebrated());
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void triggerVipCelebration(Player player, String roleDisplay) {
        String title = ColorUtil.colorize(
                plugin.getConfig().getString("vip-delivery.title", "&6&l✦ VIP AKTİF ✦")
                        .replace("{role}", roleDisplay)
                        .replace("{player}", player.getName())
        );
        String subtitle = ColorUtil.colorize(
                plugin.getConfig().getString("vip-delivery.subtitle", "&f{role} &7paketiniz hesabınıza tanımlandı!")
                        .replace("{role}", roleDisplay)
                        .replace("{player}", player.getName())
        );

        player.sendTitle(title, subtitle, 10, 80, 20);
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.9f, 1.0f);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.1f);

        if (plugin.getConfig().getBoolean("vip-delivery.broadcast-enabled", true)) {
            String bc = ColorUtil.colorize(
                    plugin.getConfig().getString(
                                    "vip-delivery.broadcast-message",
                                    "&8[&f&lGRIZM&7&lMC&8] &f&l{player} &7mağazadan &e&l{role} &7satın aldı! Desteği için teşekkürler! &6✦"
                            )
                            .replace("{player}", player.getName())
                            .replace("{role}", roleDisplay)
            );
            Bukkit.broadcastMessage("");
            Bukkit.broadcastMessage(bc);
            Bukkit.broadcastMessage("");
        }

        if (plugin.getConfig().getBoolean("vip-delivery.firework-enabled", true)) {
            try {
                Location loc = player.getLocation().clone().add(0, 1.2, 0);
                Firework fw = loc.getWorld().spawn(loc, Firework.class);
                fw.getPersistentDataContainer().set(fireworkKey, PersistentDataType.BYTE, (byte) 1);
                FireworkMeta meta = fw.getFireworkMeta();
                meta.setPower(0);
                meta.addEffect(FireworkEffect.builder()
                        .with(FireworkEffect.Type.BALL_LARGE)
                        .withColor(Color.WHITE, Color.SILVER, Color.YELLOW)
                        .withFade(Color.GRAY)
                        .flicker(true)
                        .trail(true)
                        .build());
                fw.setFireworkMeta(meta);
                Bukkit.getScheduler().runTaskLater(plugin, fw::detonate, 2L);
            } catch (Throwable ignored) {
            }
        }
    }

    public void acknowledgeDeliveryAsync(String username, String deliveryId, String serverId, boolean celebrated) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            JsonObject body = new JsonObject();
            body.addProperty("action", "ack_delivery");
            body.addProperty("username", username);
            body.addProperty("delivery_id", deliveryId);
            body.addProperty("server_id", serverId);
            body.addProperty("celebrated", celebrated);

            String mode = getMode();
            boolean ackedHttp = false;
            if (!mode.equals("local")) {
                JsonObject resp = sendPostJson("action=ack_delivery", body);
                if (resp != null && "success".equalsIgnoreCase(getString(resp, "status"))) {
                    ackedHttp = true;
                }
            }

            if (!ackedHttp) {
                // Yerel listeden temizle
                UserData u = manager.getUserData(username);
                if (u != null) {
                    u.getPendingDeliveries().removeIf(d -> deliveryId.equals(d.getId()) && d.isCelebrated());
                    manager.saveLocalCache();
                    if (!mode.equals("http")) {
                        writeSnapshotToLocalWebDir();
                    }
                }
            }
        });
    }

    // =========================================================================
    // 4. DÜŞÜK SEVİYELİ HTTP & JSON YARDIMCILARI
    // =========================================================================

    private List<String> getCandidateApiUrls() {
        List<String> urls = new ArrayList<>();
        String primary = plugin.getConfig().getString("sync.api-url", "").trim();
        String fallback = plugin.getConfig().getString("sync.fallback-api-url", "").trim();
        if (!primary.isEmpty()) urls.add(primary);
        if (!fallback.isEmpty() && !fallback.equals(primary)) urls.add(fallback);
        return urls;
    }

    private JsonObject sendGetJson(String queryString) {
        int timeoutSec = Math.max(2, plugin.getConfig().getInt("sync.timeout-seconds", 5));
        for (String baseUrl : getCandidateApiUrls()) {
            try {
                String sep = baseUrl.contains("?") ? "&" : "?";
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + sep + queryString))
                        .timeout(Duration.ofSeconds(timeoutSec))
                        .header("Accept", "application/json")
                        .header("User-Agent", "GrizmPerms/1.0.0")
                        .header("X-Server-Key", getServerKey())
                        .GET()
                        .build();

                HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (resp.statusCode() >= 200 && resp.statusCode() < 300 && resp.body() != null) {
                    JsonElement parsed = JsonParser.parseString(resp.body());
                    if (parsed.isJsonObject()) {
                        return parsed.getAsJsonObject();
                    }
                }
            } catch (Exception ignored) {
                // Yedek URL veya yerel mod denenecek
            }
        }
        return null;
    }

    private JsonObject sendPostJson(String queryString, JsonObject payload) {
        int timeoutSec = Math.max(2, plugin.getConfig().getInt("sync.timeout-seconds", 5));
        String jsonBody = JsonHelper.GSON.toJson(payload);
        for (String baseUrl : getCandidateApiUrls()) {
            try {
                String sep = baseUrl.contains("?") ? "&" : "?";
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + sep + queryString))
                        .timeout(Duration.ofSeconds(timeoutSec))
                        .header("Content-Type", "application/json; charset=utf-8")
                        .header("Accept", "application/json")
                        .header("User-Agent", "GrizmPerms/1.0.0")
                        .header("X-Server-Key", getServerKey())
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                        .build();

                HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (resp.statusCode() >= 200 && resp.statusCode() < 300 && resp.body() != null) {
                    JsonElement parsed = JsonParser.parseString(resp.body());
                    if (parsed.isJsonObject()) {
                        return parsed.getAsJsonObject();
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private Map<String, Role> parseRolesElement(JsonElement el) {
        Map<String, Role> map = new HashMap<>();
        if (el == null || el.isJsonNull() || el.isJsonArray()) {
            return map;
        }
        if (el.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : el.getAsJsonObject().entrySet()) {
                Role r = JsonHelper.GSON.fromJson(entry.getValue(), Role.class);
                if (r != null) {
                    if (r.getId().isEmpty()) r.setId(entry.getKey());
                    map.put(entry.getKey().toLowerCase(), r);
                }
            }
        }
        return map;
    }

    private Map<String, UserData> parseUsersElement(JsonElement el) {
        Map<String, UserData> map = new HashMap<>();
        if (el == null || el.isJsonNull() || el.isJsonArray()) {
            return map;
        }
        if (el.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : el.getAsJsonObject().entrySet()) {
                UserData u = JsonHelper.GSON.fromJson(entry.getValue(), UserData.class);
                if (u != null) {
                    if (u.getUsername().isEmpty()) u.setUsername(entry.getKey());
                    map.put(entry.getKey().toLowerCase(), u);
                }
            }
        }
        return map;
    }

    private static String getString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return "";
        return obj.get(key).getAsString();
    }
}
