package com.grizmtech.perms.engine;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.grizmtech.perms.GrizmPermsPlugin;
import com.grizmtech.perms.model.Role;
import com.grizmtech.perms.model.RoleAssignment;
import com.grizmtech.perms.model.UserData;
import com.grizmtech.perms.util.ColorUtil;
import com.grizmtech.perms.util.JsonHelper;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;

import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Tüm rollerin, oyuncu verilerinin, süreli VIP kontrollerinin ve Bukkit izin senkronizasyonunun merkezi yöneticisi.
 */
public class PermissionManager {

    private final GrizmPermsPlugin plugin;
    private final Gson gson = JsonHelper.GSON;

    private final Map<String, Role> roles = new ConcurrentHashMap<>();
    private final Map<String, UserData> users = new ConcurrentHashMap<>();
    private final Map<String, EffectivePermissions> compiledCache = new ConcurrentHashMap<>();
    private final Map<UUID, PermissionAttachment> attachments = new ConcurrentHashMap<>();

    private volatile String defaultRoleId = "default";
    private volatile long currentRevision = 0L;
    private final NametagAndTabManager nametagAndTabManager;

    public PermissionManager(GrizmPermsPlugin plugin) {
        this.plugin = plugin;
        this.nametagAndTabManager = new NametagAndTabManager(plugin, this);
    }

    public NametagAndTabManager getNametagAndTabManager() {
        return nametagAndTabManager;
    }

    public void initialize() {
        loadLocalCache();
        if (roles.isEmpty()) {
            seedBuiltInFallbackRoles();
            saveLocalCache();
        }
    }

    public String getServerId() {
        return plugin.getConfig().getString("server-id", "lobby").trim().toLowerCase();
    }

    public long getCurrentRevision() {
        return currentRevision;
    }

    public void setCurrentRevision(long revision) {
        this.currentRevision = revision;
    }

    public String getDefaultRoleId() {
        return defaultRoleId;
    }

    // =========================================================================
    // ROLLER & KULLANICILAR ERİŞİM
    // =========================================================================

    public Collection<Role> getAllRolesSorted() {
        List<Role> list = new ArrayList<>(roles.values());
        list.sort(Comparator.comparingInt(Role::getWeight).reversed());
        return list;
    }

    public Map<String, Role> getRolesMap() {
        return Collections.unmodifiableMap(roles);
    }

    public Role getRole(String roleId) {
        if (roleId == null) return null;
        return roles.get(roleId.trim().toLowerCase());
    }

    public Role getDefaultRole() {
        Role def = roles.get(defaultRoleId);
        if (def != null) return def;
        def = roles.get("default");
        if (def != null) return def;
        return roles.values().stream()
                .min(Comparator.comparingInt(Role::getWeight))
                .orElseGet(this::createEmergencyDefaultRole);
    }

    public UserData getUserData(String username) {
        if (username == null || username.isEmpty()) return null;
        return users.get(username.trim().toLowerCase());
    }

    public UserData getOrCreateUserData(String username) {
        String key = username.trim().toLowerCase();
        return users.computeIfAbsent(key, k -> new UserData(username.trim()));
    }

    public Map<String, UserData> getUsersMap() {
        return Collections.unmodifiableMap(users);
    }

    /**
     * Web senkronizasyonundan gelen tam snapshot'ı uygular
     */
    public synchronized void applySnapshot(Map<String, Role> newRoles,
                                           Map<String, UserData> newUsers,
                                           String newDefaultRoleId,
                                           long revision) {
        if (newRoles != null && !newRoles.isEmpty()) {
            roles.clear();
            for (Map.Entry<String, Role> e : newRoles.entrySet()) {
                Role r = e.getValue();
                if (r != null) {
                    if (r.getId().isEmpty()) r.setId(e.getKey());
                    roles.put(r.getId().toLowerCase(), r);
                }
            }
        }

        if (newUsers != null) {
            users.clear();
            for (Map.Entry<String, UserData> e : newUsers.entrySet()) {
                UserData u = e.getValue();
                if (u != null) {
                    if (u.getUsername().isEmpty()) u.setUsername(e.getKey());
                    users.put(e.getKey().toLowerCase(), u);
                }
            }
        }

        if (newDefaultRoleId != null && !newDefaultRoleId.isEmpty() && roles.containsKey(newDefaultRoleId.toLowerCase())) {
            this.defaultRoleId = newDefaultRoleId.toLowerCase();
        } else {
            for (Role r : roles.values()) {
                if (r.isDefault()) {
                    this.defaultRoleId = r.getId();
                    break;
                }
            }
        }

        this.currentRevision = revision;
        this.compiledCache.clear();
        saveLocalCache();

        // Çevrimiçi tüm oyuncuların izinlerini ana iş parçacığında yenile
        Runnable refreshTask = () -> {
            for (Player online : Bukkit.getOnlinePlayers()) {
                refreshPlayer(online);
            }
        };

        if (Bukkit.isPrimaryThread()) {
            refreshTask.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, refreshTask);
        }
    }

    public void updateSingleUser(UserData updatedUser, long revision) {
        if (updatedUser == null || updatedUser.getUsername().isEmpty()) return;
        String key = updatedUser.getUsername().toLowerCase();
        users.put(key, updatedUser);
        compiledCache.remove(key);
        if (revision > this.currentRevision) {
            this.currentRevision = revision;
        }
        saveLocalCache();

        Runnable task = () -> {
            Player online = Bukkit.getPlayerExact(updatedUser.getUsername());
            if (online != null && online.isOnline()) {
                refreshPlayer(online);
            }
        };
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    // =========================================================================
    // OYUNCU ROL & İZİN HESAPLAMA MOTORU
    // =========================================================================

    public Role getPrimaryRole(String username) {
        UserData user = getUserData(username);
        Role best = getDefaultRole();
        int bestWeight = best.getWeight();
        long now = System.currentTimeMillis() / 1000L;

        if (user != null) {
            for (RoleAssignment ass : user.getRoles()) {
                if (ass.isExpired(now)) continue;
                Role r = getRole(ass.getRoleId());
                if (r != null && r.getWeight() >= bestWeight) {
                    bestWeight = r.getWeight();
                    best = r;
                }
            }
        }
        return best;
    }

    /**
     * Oyuncunun TAB listesinde, karakter üstü (3D Nametag) isminde ve sohbette
     * görünecek birincil rol prefixini döndürür.
     */
    public String getDisplayPrefix(String username) {
        Role primary = getPrimaryRole(username);
        String prefix = primary.getPrefix();
        if (prefix == null || prefix.trim().isEmpty()) {
            prefix = "&7[" + primary.getName() + "] &7";
        }
        return ColorUtil.ensurePrefixSpacing(prefix);
    }

    /**
     * Oyuncunun TAB listesinde, karakter üstü (3D Nametag) isminde ve sohbette
     * görünecek suffix + ikincil VIP + özel izin (perm) etiketini döndürür.
     */
    public String getDisplaySuffix(String username) {
        Role primary = getPrimaryRole(username);
        StringBuilder sb = new StringBuilder();
        if (primary.getSuffix() != null && !primary.getSuffix().isEmpty()) {
            sb.append(primary.getSuffix());
        }

        // Eğer oyuncunun birincil rolü yetkili rolüyse ve aynı zamanda aktif VIP'si varsa VIP rozetini de göster
        RoleAssignment vipAss = getActiveVipAssignment(username);
        if (vipAss != null && !vipAss.getRoleId().equalsIgnoreCase(primary.getId())) {
            Role vipRole = getRole(vipAss.getRoleId());
            if (vipRole != null) {
                sb.append(" &d[").append(vipRole.getName()).append("]");
            }
        }

        // Oyuncuya doğrudan tanımlanmış özel bireysel izinler (permler) varsa etikette göster
        if (plugin.getConfig().getBoolean("display.show-custom-perms-in-tag", true)) {
            UserData user = getUserData(username);
            if (user != null) {
                List<String> positiveCustomPerms = new ArrayList<>();
                for (String p : user.getPermissions()) {
                    if (p != null && !p.trim().isEmpty() && !p.trim().startsWith("-")) {
                        positiveCustomPerms.add(p.trim());
                    }
                }
                List<String> srvCustom = user.getServerPermissions().get(getServerId());
                if (srvCustom != null) {
                    for (String p : srvCustom) {
                        if (p != null && !p.trim().isEmpty() && !p.trim().startsWith("-")) {
                            positiveCustomPerms.add(p.trim());
                        }
                    }
                }

                if (!positiveCustomPerms.isEmpty()) {
                    if (positiveCustomPerms.contains("*")) {
                        sb.append(" &6[★ *]");
                    } else if (positiveCustomPerms.size() == 1) {
                        String single = positiveCustomPerms.get(0);
                        int dotIdx = single.lastIndexOf('.');
                        String shortPerm = (dotIdx > 0 && dotIdx + 1 < single.length())
                                ? single.substring(dotIdx + 1)
                                : single;
                        sb.append(" &b[").append(shortPerm).append("]");
                    } else {
                        sb.append(" &b[+").append(positiveCustomPerms.size()).append(" Perm]");
                    }
                }
            }
        }
        return sb.toString();
    }

    public RoleAssignment getActiveVipAssignment(String username) {
        UserData user = getUserData(username);
        if (user == null) return null;
        long now = System.currentTimeMillis() / 1000L;

        RoleAssignment bestAss = null;
        int bestWeight = -1;
        for (RoleAssignment ass : user.getRoles()) {
            if (ass.isExpired(now)) continue;
            Role r = getRole(ass.getRoleId());
            if (r != null && r.isVip() && r.getWeight() > bestWeight) {
                bestWeight = r.getWeight();
                bestAss = ass;
            }
        }
        return bestAss;
    }

    public List<String> getPlayerActiveGroupIds(String username) {
        Set<String> groupIds = new LinkedHashSet<>();
        UserData user = getUserData(username);
        long now = System.currentTimeMillis() / 1000L;

        if (user != null) {
            List<RoleAssignment> active = new ArrayList<>();
            for (RoleAssignment ass : user.getRoles()) {
                if (!ass.isExpired(now) && getRole(ass.getRoleId()) != null) {
                    active.add(ass);
                }
            }
            active.sort((a, b) -> {
                Role ra = getRole(a.getRoleId());
                Role rb = getRole(b.getRoleId());
                int wa = ra != null ? ra.getWeight() : 0;
                int wb = rb != null ? rb.getWeight() : 0;
                return Integer.compare(wb, wa);
            });
            for (RoleAssignment ass : active) {
                groupIds.add(ass.getRoleId());
            }
        }

        groupIds.add(getDefaultRole().getId());
        return new ArrayList<>(groupIds);
    }

    public boolean isPlayerInGroup(String username, String groupId) {
        if (groupId == null) return false;
        String target = groupId.trim().toLowerCase();
        Set<String> expanded = new HashSet<>();
        for (String gid : getPlayerActiveGroupIds(username)) {
            collectInheritedRoles(gid, expanded);
        }
        return expanded.contains(target);
    }

    private void collectInheritedRoles(String roleId, Set<String> visited) {
        if (roleId == null) return;
        String key = roleId.toLowerCase();
        if (!visited.add(key)) return;
        Role role = getRole(key);
        if (role == null) return;
        for (String parent : role.getInheritance()) {
            collectInheritedRoles(parent, visited);
        }
    }

    public EffectivePermissions getEffectivePermissions(String username) {
        if (username == null || username.isEmpty()) {
            return EffectivePermissions.EMPTY;
        }
        String key = username.toLowerCase();
        return compiledCache.computeIfAbsent(key, this::compileForUser);
    }

    private EffectivePermissions compileForUser(String lowerUsername) {
        Set<String> inheritedRoleIds = new HashSet<>();
        collectInheritedRoles(getDefaultRole().getId(), inheritedRoleIds);

        UserData user = users.get(lowerUsername);
        long now = System.currentTimeMillis() / 1000L;
        if (user != null) {
            for (RoleAssignment ass : user.getRoles()) {
                if (!ass.isExpired(now)) {
                    collectInheritedRoles(ass.getRoleId(), inheritedRoleIds);
                }
            }
        }

        // Düşük ağırlıktan yüksek ağırlığa doğru sırala (yüksek rol alt rolü ezer)
        List<Role> orderedRoles = new ArrayList<>();
        for (String rid : inheritedRoleIds) {
            Role r = getRole(rid);
            if (r != null) orderedRoles.add(r);
        }
        orderedRoles.sort(Comparator.comparingInt(Role::getWeight));

        String srvId = getServerId();
        List<String> orderedRules = new ArrayList<>();

        for (Role r : orderedRoles) {
            orderedRules.addAll(r.getPermissions());
            List<String> srvPerms = r.getServerPermissions().get(srvId);
            if (srvPerms != null) {
                orderedRules.addAll(srvPerms);
            }
        }

        if (user != null) {
            orderedRules.addAll(user.getPermissions());
            List<String> userSrvPerms = user.getServerPermissions().get(srvId);
            if (userSrvPerms != null) {
                orderedRules.addAll(userSrvPerms);
            }
        }

        return EffectivePermissions.compile(orderedRules);
    }

    public boolean hasPermission(String username, String permission) {
        Boolean res = getEffectivePermissions(username).resolve(permission);
        return res != null && res;
    }

    // =========================================================================
    // BUKKIT OYUNCU İZİN ENJEKSİYONU & YENİLEME
    // =========================================================================

    public void refreshPlayer(Player player) {
        if (player == null) return;

        PermissibleInjector.inject(player, this, plugin.getLogger());
        String key = player.getName().toLowerCase();
        compiledCache.remove(key);
        EffectivePermissions eff = getEffectivePermissions(player.getName());

        PermissionAttachment attachment = attachments.computeIfAbsent(
                player.getUniqueId(),
                uuid -> player.addAttachment(plugin)
        );

        // Eski izinleri temizle
        for (String existingPerm : new ArrayList<>(attachment.getPermissions().keySet())) {
            attachment.unsetPermission(existingPerm);
        }

        // Doğrudan düğümleri ata
        for (Map.Entry<String, Boolean> entry : eff.getExactNodes().entrySet()) {
            attachment.setPermission(entry.getKey(), entry.getValue());
        }

        // Kayıtlı Bukkit izinlerini wildcard kurallarına göre doldur (Brigadier komut ağacı için)
        for (Permission registeredPerm : Bukkit.getPluginManager().getPermissions()) {
            String pName = registeredPerm.getName().toLowerCase();
            if (!eff.getExactNodes().containsKey(pName)) {
                Boolean resolved = eff.resolve(pName);
                if (resolved != null) {
                    attachment.setPermission(registeredPerm.getName(), resolved);
                }
            }
        }

        player.recalculatePermissions();
        try {
            player.updateCommands();
        } catch (Throwable ignored) {
        }

        // Eğer uçuş yetkisi (essentials.fly) negasyon ile kaldırıldıysa (örn: PvP sunucusu) güvenli kontrol
        if (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE) {
            if (!player.isOp() && !player.hasPermission("essentials.fly") && player.getAllowFlight()) {
                player.setFlying(false);
                player.setAllowFlight(false);
            }
        }

        // TAB listesi ve 3D Karakter Üstü İsim (Nametag) görünümünü anında güncelle
        nametagAndTabManager.updatePlayer(player);
    }

    public void cleanupPlayer(Player player) {
        if (player == null) return;
        nametagAndTabManager.removePlayerFromTeams(player);
        PermissionAttachment att = attachments.remove(player.getUniqueId());
        if (att != null) {
            try {
                player.removeAttachment(att);
            } catch (Throwable ignored) {
            }
        }
        compiledCache.remove(player.getName().toLowerCase());
    }

    public void cleanupAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            cleanupPlayer(p);
        }
        attachments.clear();
        compiledCache.clear();
    }

    // =========================================================================
    // SÜRELİ VIP OTOMATİK SONA ERME KONTROLÜ
    // =========================================================================

    public void checkExpiredRoles() {
        long now = System.currentTimeMillis() / 1000L;
        boolean anyChanged = false;

        for (UserData user : users.values()) {
            List<RoleAssignment> expiredList = new ArrayList<>();
            Iterator<RoleAssignment> it = user.getRoles().iterator();
            while (it.hasNext()) {
                RoleAssignment ass = it.next();
                if (ass.isExpired(now)) {
                    expiredList.add(ass);
                    it.remove();
                    anyChanged = true;
                }
            }

            if (!expiredList.isEmpty()) {
                compiledCache.remove(user.getUsername().toLowerCase());
                Player online = Bukkit.getPlayerExact(user.getUsername());
                if (online != null && online.isOnline()) {
                    refreshPlayer(online);
                    for (RoleAssignment expAss : expiredList) {
                        Role r = getRole(expAss.getRoleId());
                        String roleDisplay = r != null ? r.getName() : expAss.getRoleId();
                        String msg = plugin.getConfig().getString(
                                "vip-delivery.expired-message",
                                "&8[&f&lGRIZM&7&lMC&8] &e{role} &7üyeliğinizin süresi sona erdi."
                        ).replace("{role}", roleDisplay);
                        online.sendMessage(ColorUtil.colorize(msg));
                    }
                }
            }
        }

        if (anyChanged) {
            saveLocalCache();
        }
    }

    // =========================================================================
    // YEREL CACHE.JSON OKUMA / YAZMA VE YEDEK ROLLER
    // =========================================================================

    public synchronized void saveLocalCache() {
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            File file = new File(plugin.getDataFolder(), "cache.json");
            JsonObject root = new JsonObject();
            root.addProperty("revision", currentRevision);
            root.addProperty("default_role_id", defaultRoleId);
            root.add("roles", gson.toJsonTree(roles));
            root.add("users", gson.toJsonTree(users));

            try (Writer writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
                gson.toJson(root, writer);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[GrizmPerms] Yerel cache kaydedilemedi: " + e.getMessage());
        }
    }

    public synchronized void loadLocalCache() {
        File file = new File(plugin.getDataFolder(), "cache.json");
        if (!file.exists()) return;

        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (root.has("revision")) {
                this.currentRevision = root.get("revision").getAsLong();
            }
            if (root.has("default_role_id")) {
                this.defaultRoleId = root.get("default_role_id").getAsString();
            }
            if (root.has("roles") && root.get("roles").isJsonObject()) {
                roles.clear();
                JsonObject rObj = root.getAsJsonObject("roles");
                for (Map.Entry<String, JsonElement> entry : rObj.entrySet()) {
                    Role role = gson.fromJson(entry.getValue(), Role.class);
                    if (role != null) {
                        roles.put(entry.getKey().toLowerCase(), role);
                    }
                }
            }
            if (root.has("users") && root.get("users").isJsonObject()) {
                users.clear();
                JsonObject uObj = root.getAsJsonObject("users");
                for (Map.Entry<String, JsonElement> entry : uObj.entrySet()) {
                    UserData u = gson.fromJson(entry.getValue(), UserData.class);
                    if (u != null) {
                        users.put(entry.getKey().toLowerCase(), u);
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[GrizmPerms] Yerel cache okunamadı: " + e.getMessage());
        }
    }

    private Role createEmergencyDefaultRole() {
        Role def = new Role();
        def.setId("default");
        def.setName("Oyuncu");
        def.setWeight(10);
        def.setPrefix("&8[Oyuncu] &7");
        def.setChatColor("&7");
        def.setDefault(true);
        return def;
    }

    private void seedBuiltInFallbackRoles() {
        Role def = createEmergencyDefaultRole();
        def.getPermissions().addAll(Arrays.asList(
                "essentials.spawn", "essentials.tpa", "essentials.tpaccept", "essentials.tpdeny",
                "essentials.home", "essentials.sethome", "essentials.delhome", "essentials.sethome.multiple",
                "essentials.warp", "essentials.msg", "essentials.reply", "essentials.balance", "essentials.pay"
        ));
        roles.put("default", def);

        Role iron = new Role();
        iron.setId("iron-vip");
        iron.setName("IRON VIP");
        iron.setWeight(100);
        iron.setPrefix("&7&l[IronVIP] &f");
        iron.setChatColor("&f");
        iron.setVip(true);
        iron.setVipPrice(75);
        iron.getInheritance().add("default");
        iron.getPermissions().addAll(Arrays.asList("essentials.joinfullserver", "essentials.sethome.multiple.iron", "essentials.chat.color", "essentials.feed"));
        roles.put("iron-vip", iron);

        Role gold = new Role();
        gold.setId("gold-vip");
        gold.setName("GOLD VIP");
        gold.setWeight(200);
        gold.setPrefix("&6&l[GoldVIP] &e");
        gold.setSuffix(" &e★");
        gold.setChatColor("&f");
        gold.setVip(true);
        gold.setVipPrice(140);
        gold.getInheritance().add("iron-vip");
        gold.getPermissions().addAll(Arrays.asList("essentials.sethome.multiple.gold", "essentials.workbench", "essentials.repair"));
        roles.put("gold-vip", gold);

        Role diamond = new Role();
        diamond.setId("diamond-vip");
        diamond.setName("DIAMOND VIP");
        diamond.setWeight(300);
        diamond.setPrefix("&b&l[DiamondVIP] &b");
        diamond.setSuffix(" &b❖");
        diamond.setChatColor("&f");
        diamond.setVip(true);
        diamond.setVipPrice(220);
        diamond.getInheritance().add("gold-vip");
        diamond.getPermissions().addAll(Arrays.asList("essentials.sethome.multiple.diamond", "essentials.enderchest", "essentials.fly", "essentials.heal"));
        roles.put("diamond-vip", diamond);

        Role netherite = new Role();
        netherite.setId("netherite-vip");
        netherite.setName("NETHERITE VIP");
        netherite.setWeight(400);
        netherite.setPrefix("&5&l[NetheriteVIP] &d");
        netherite.setSuffix(" &d✦");
        netherite.setChatColor("&f");
        netherite.setVip(true);
        netherite.setVipPrice(350);
        netherite.getInheritance().add("diamond-vip");
        netherite.getPermissions().addAll(Arrays.asList("essentials.sethome.multiple.unlimited", "essentials.repair.all", "essentials.hat", "essentials.nick"));
        roles.put("netherite-vip", netherite);

        Role owner = new Role();
        owner.setId("owner");
        owner.setName("Kurucu");
        owner.setWeight(1000);
        owner.setPrefix("&4&l[KURUCU] &c");
        owner.setSuffix(" &4✦");
        owner.setChatColor("&f");
        owner.getPermissions().add("*");
        roles.put("owner", owner);
    }
}
