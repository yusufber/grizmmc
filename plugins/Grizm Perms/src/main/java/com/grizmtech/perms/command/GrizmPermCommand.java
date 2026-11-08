package com.grizmtech.perms.command;

import com.google.gson.JsonObject;
import com.grizmtech.perms.GrizmPermsPlugin;
import com.grizmtech.perms.engine.PermissionManager;
import com.grizmtech.perms.model.PendingDelivery;
import com.grizmtech.perms.model.Role;
import com.grizmtech.perms.model.RoleAssignment;
import com.grizmtech.perms.model.UserData;
import com.grizmtech.perms.sync.WebSyncService;
import com.grizmtech.perms.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * /gperm yönetim komutu ve otomatik tamamlama (TabCompleter) sistemi.
 */
public class GrizmPermCommand implements CommandExecutor, TabCompleter {

    private final GrizmPermsPlugin plugin;
    private final PermissionManager manager;
    private final WebSyncService syncService;

    public GrizmPermCommand(GrizmPermsPlugin plugin, PermissionManager manager, WebSyncService syncService) {
        this.plugin = plugin;
        this.manager = manager;
        this.syncService = syncService;
    }

    private String prefix() {
        return ColorUtil.colorize(plugin.getConfig().getString("messages.prefix", "&8[&f&lGRIZM&7&lPERMS&8] "));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.isOp() && !sender.hasPermission("grizmperms.admin")) {
            sender.sendMessage(prefix() + ColorUtil.colorize(plugin.getConfig().getString("messages.no-permission", "&cYetkiniz yok.")));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "sync" -> {
                if (args.length >= 2) {
                    String target = args[1];
                    syncService.syncSinglePlayerAsync(target);
                    sender.sendMessage(prefix() + ColorUtil.colorize("&a" + target + " &7oyuncusu için web senkronizasyonu başlatıldı."));
                } else {
                    sender.sendMessage(prefix() + ColorUtil.colorize("&7Web sitesi ile tam senkronizasyon başlatılıyor..."));
                    syncService.syncAllAsync(true, ok -> {
                        if (ok) {
                            sender.sendMessage(prefix() + ColorUtil.colorize("&aSenkronizasyon tamamlandı! &8(Revizyon #" + manager.getCurrentRevision() + ")"));
                        } else {
                            sender.sendMessage(prefix() + ColorUtil.colorize("&eWeb API'ye ulaşılamadı; yerel önbellek aktif."));
                        }
                    });
                }
                return true;
            }

            case "reload" -> {
                plugin.reloadConfig();
                for (Player online : Bukkit.getOnlinePlayers()) {
                    manager.refreshPlayer(online);
                }
                syncService.syncAllAsync(true, null);
                sender.sendMessage(prefix() + ColorUtil.colorize("&aGrizmPerms yapılandırması yeniden yüklendi! &8(Sunucu: " + manager.getServerId() + ")"));
                return true;
            }

            case "roles", "groups" -> {
                sender.sendMessage(ColorUtil.colorize("&8&m--------------------------------------------------"));
                sender.sendMessage(ColorUtil.colorize(" &f&lGRIZMPERMS ROLLERİ &8(&7Toplam: " + manager.getRolesMap().size() + "&8)"));
                for (Role r : manager.getAllRolesSorted()) {
                    String vipTag = r.isVip() ? (" &e[" + (int) r.getVipPrice() + "₺]") : "";
                    sender.sendMessage(ColorUtil.colorize(
                            " &8▪ &f" + r.getId() + " &7(" + r.getName() + ") &8| &7W:&f" + r.getWeight()
                                    + " &8| &7İzin:&f" + r.getPermissions().size() + vipTag + " &8| " + r.getPrefix() + "Örnek"
                    ));
                }
                sender.sendMessage(ColorUtil.colorize("&8&m--------------------------------------------------"));
                return true;
            }

            case "givevip" -> {
                if (args.length < 3) {
                    sender.sendMessage(prefix() + ColorUtil.colorize("&cKullanım: /gperm givevip <oyuncu> <vipRol> [gün]"));
                    return true;
                }
                String target = args[1];
                String roleId = args[2].toLowerCase(Locale.ROOT);
                Role role = manager.getRole(roleId);
                if (role == null) {
                    sender.sendMessage(prefix() + ColorUtil.colorize("&cRol bulunamadı: &f" + roleId));
                    return true;
                }
                int days = role.getVipDurationDays() > 0 ? role.getVipDurationDays() : 30;
                if (args.length >= 4) {
                    days = parseDaysSafe(args[3], days);
                }

                grantLocalRole(target, role, days, "ingame:" + sender.getName(), true);

                JsonObject mut = new JsonObject();
                mut.addProperty("operation", "grant_role");
                mut.addProperty("actor", sender.getName());
                mut.addProperty("username", target);
                mut.addProperty("role_id", role.getId());
                mut.addProperty("duration_days", days);
                mut.addProperty("celebrate", true);
                syncService.pushMutationAsync(mut);

                Player online = Bukkit.getPlayerExact(target);
                if (online != null && online.isOnline()) {
                    syncService.processDeliveriesForPlayer(online);
                }

                sender.sendMessage(prefix() + ColorUtil.colorize("&a" + target + " &7oyuncusuna &e" + days + " günlük " + role.getName() + " &7tanımlandı ve kutlama tetiklendi!"));
                return true;
            }

            case "user" -> {
                handleUserSubcommand(sender, args);
                return true;
            }

            case "role", "group" -> {
                handleRoleSubcommand(sender, args);
                return true;
            }

            default -> {
                sendHelp(sender);
                return true;
            }
        }
    }

    private void handleUserSubcommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(prefix() + ColorUtil.colorize("&cKullanım: /gperm user <oyuncu> <info|setrole|addrole|delrole|addperm|delperm> ..."));
            return;
        }
        String username = args[1];
        String action = args[2].toLowerCase(Locale.ROOT);

        switch (action) {
            case "info" -> {
                Role primary = manager.getPrimaryRole(username);
                UserData u = manager.getUserData(username);
                sender.sendMessage(ColorUtil.colorize("&8&m--------------------------------------------------"));
                sender.sendMessage(ColorUtil.colorize(" &f&lOYUNCU YETKİ BİLGİSİ: &e" + username));
                sender.sendMessage(ColorUtil.colorize(" &8▪ &7Birincil Rol: &f" + primary.getName() + " &8(" + primary.getId() + ")"));
                if (u != null && !u.getRoles().isEmpty()) {
                    String rolesStr = u.getRoles().stream()
                            .map(a -> a.getRoleId() + " (" + ColorUtil.formatRemainingSeconds(a.getExpiresAt()) + ")")
                            .collect(Collectors.joining(", "));
                    sender.sendMessage(ColorUtil.colorize(" &8▪ &7Atanmış Roller: &f" + rolesStr));
                }
                if (u != null && !u.getPermissions().isEmpty()) {
                    sender.sendMessage(ColorUtil.colorize(" &8▪ &7Özel İzinler: &f" + String.join(", ", u.getPermissions())));
                }
                sender.sendMessage(ColorUtil.colorize("&8&m--------------------------------------------------"));
            }

            case "setrole" -> {
                if (args.length < 4) {
                    sender.sendMessage(prefix() + ColorUtil.colorize("&cKullanım: /gperm user <oyuncu> setrole <rol> [gün]"));
                    return;
                }
                Role role = manager.getRole(args[3]);
                if (role == null) {
                    sender.sendMessage(prefix() + ColorUtil.colorize("&cRol bulunamadı: " + args[3]));
                    return;
                }
                int days = args.length >= 5 ? parseDaysSafe(args[4], 0) : 0;
                long now = System.currentTimeMillis() / 1000L;
                long exp = days > 0 ? (now + days * 86400L) : 0L;

                UserData u = manager.getOrCreateUserData(username);
                u.getRoles().clear();
                u.getRoles().add(new RoleAssignment(role.getId(), now, exp, "ingame:" + sender.getName()));
                manager.updateSingleUser(u, manager.getCurrentRevision());

                JsonObject mut = new JsonObject();
                mut.addProperty("operation", "set_role");
                mut.addProperty("actor", sender.getName());
                mut.addProperty("username", username);
                mut.addProperty("role_id", role.getId());
                mut.addProperty("duration_days", days);
                syncService.pushMutationAsync(mut);

                sender.sendMessage(prefix() + ColorUtil.colorize("&a" + username + " &7oyuncusunun rolü &e" + role.getName()
                        + (days > 0 ? " (" + days + " gün)" : " (Kalıcı)") + " &7olarak ayarlandı."));
            }

            case "addrole" -> {
                if (args.length < 4) {
                    sender.sendMessage(prefix() + ColorUtil.colorize("&cKullanım: /gperm user <oyuncu> addrole <rol> [gün]"));
                    return;
                }
                Role role = manager.getRole(args[3]);
                if (role == null) {
                    sender.sendMessage(prefix() + ColorUtil.colorize("&cRol bulunamadı: " + args[3]));
                    return;
                }
                int days = args.length >= 5 ? parseDaysSafe(args[4], 0) : 0;
                grantLocalRole(username, role, days, "ingame:" + sender.getName(), false);

                JsonObject mut = new JsonObject();
                mut.addProperty("operation", "grant_role");
                mut.addProperty("actor", sender.getName());
                mut.addProperty("username", username);
                mut.addProperty("role_id", role.getId());
                mut.addProperty("duration_days", days);
                mut.addProperty("celebrate", false);
                syncService.pushMutationAsync(mut);

                sender.sendMessage(prefix() + ColorUtil.colorize("&a" + username + " &7oyuncusuna &e" + role.getName()
                        + (days > 0 ? " (" + days + " gün)" : " (Kalıcı)") + " &7eklendi."));
            }

            case "delrole", "removerole" -> {
                if (args.length < 4) {
                    sender.sendMessage(prefix() + ColorUtil.colorize("&cKullanım: /gperm user <oyuncu> delrole <rol>"));
                    return;
                }
                String roleId = args[3].toLowerCase(Locale.ROOT);
                UserData u = manager.getUserData(username);
                if (u == null || !u.getRoles().removeIf(a -> a.getRoleId().equalsIgnoreCase(roleId))) {
                    sender.sendMessage(prefix() + ColorUtil.colorize("&cOyuncuda bu rol bulunmuyor."));
                    return;
                }
                manager.updateSingleUser(u, manager.getCurrentRevision());

                JsonObject mut = new JsonObject();
                mut.addProperty("operation", "revoke_role");
                mut.addProperty("actor", sender.getName());
                mut.addProperty("username", username);
                mut.addProperty("role_id", roleId);
                syncService.pushMutationAsync(mut);

                sender.sendMessage(prefix() + ColorUtil.colorize("&a" + username + " &7oyuncusundan &e" + roleId + " &7kaldırıldı."));
            }

            case "addperm" -> {
                if (args.length < 4) {
                    sender.sendMessage(prefix() + ColorUtil.colorize("&cKullanım: /gperm user <oyuncu> addperm <izin> [sunucu]"));
                    return;
                }
                String perm = args[3].trim();
                String srv = args.length >= 5 ? args[4].trim().toLowerCase(Locale.ROOT) : "";
                UserData u = manager.getOrCreateUserData(username);
                if (!srv.isEmpty() && !srv.equals("global")) {
                    u.getServerPermissions().computeIfAbsent(srv, k -> new ArrayList<>());
                    if (!u.getServerPermissions().get(srv).contains(perm)) {
                        u.getServerPermissions().get(srv).add(perm);
                    }
                } else if (!u.getPermissions().contains(perm)) {
                    u.getPermissions().add(perm);
                }
                manager.updateSingleUser(u, manager.getCurrentRevision());

                JsonObject mut = new JsonObject();
                mut.addProperty("operation", "add_user_perm");
                mut.addProperty("actor", sender.getName());
                mut.addProperty("username", username);
                mut.addProperty("permission", perm);
                mut.addProperty("server", srv);
                syncService.pushMutationAsync(mut);

                sender.sendMessage(prefix() + ColorUtil.colorize("&a" + username + " &7oyuncusuna &f" + perm + " &7izni eklendi."));
            }

            case "delperm", "removeperm" -> {
                if (args.length < 4) {
                    sender.sendMessage(prefix() + ColorUtil.colorize("&cKullanım: /gperm user <oyuncu> delperm <izin> [sunucu]"));
                    return;
                }
                String perm = args[3].trim();
                String srv = args.length >= 5 ? args[4].trim().toLowerCase(Locale.ROOT) : "";
                UserData u = manager.getUserData(username);
                if (u != null) {
                    if (!srv.isEmpty() && !srv.equals("global")) {
                        List<String> list = u.getServerPermissions().get(srv);
                        if (list != null) list.removeIf(p -> p.equalsIgnoreCase(perm));
                    } else {
                        u.getPermissions().removeIf(p -> p.equalsIgnoreCase(perm));
                    }
                    manager.updateSingleUser(u, manager.getCurrentRevision());
                }

                JsonObject mut = new JsonObject();
                mut.addProperty("operation", "remove_user_perm");
                mut.addProperty("actor", sender.getName());
                mut.addProperty("username", username);
                mut.addProperty("permission", perm);
                mut.addProperty("server", srv);
                syncService.pushMutationAsync(mut);

                sender.sendMessage(prefix() + ColorUtil.colorize("&a" + username + " &7oyuncusundan &f" + perm + " &7izni silindi."));
            }

            default -> sender.sendMessage(prefix() + ColorUtil.colorize("&cBilinmeyen alt komut: " + action));
        }
    }

    private void handleRoleSubcommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(prefix() + ColorUtil.colorize("&cKullanım: /gperm role <id> <info|create|delete|addperm|delperm|setprefix|setsuffix|setweight|parent> ..."));
            return;
        }
        String roleId = args[1].toLowerCase(Locale.ROOT);
        String action = args[2].toLowerCase(Locale.ROOT);

        if (action.equals("create")) {
            if (manager.getRole(roleId) != null) {
                sender.sendMessage(prefix() + ColorUtil.colorize("&cBu kimlikle bir rol zaten var: " + roleId));
                return;
            }
            String displayName = args.length >= 4 ? args[3] : roleId.toUpperCase(Locale.ROOT);
            int weight = args.length >= 5 ? parseDaysSafe(args[4], 50) : 50;

            Role created = new Role();
            created.setId(roleId);
            created.setName(displayName);
            created.setWeight(weight);
            created.setPrefix("&7[" + displayName + "] &f");
            created.getInheritance().add("default");

            Map<String, Role> updatedRoles = new HashMap<>(manager.getRolesMap());
            updatedRoles.put(roleId, created);
            manager.applySnapshot(updatedRoles, null, manager.getDefaultRoleId(), manager.getCurrentRevision());

            JsonObject mut = new JsonObject();
            mut.addProperty("operation", "create_role");
            mut.addProperty("role_id", roleId);
            mut.addProperty("name", displayName);
            mut.addProperty("weight", weight);
            syncService.pushMutationAsync(mut);

            sender.sendMessage(prefix() + ColorUtil.colorize("&aYeni rol oluşturuldu: &f" + displayName + " (" + roleId + ")"));
            return;
        }

        Role role = manager.getRole(roleId);
        if (role == null) {
            sender.sendMessage(prefix() + ColorUtil.colorize("&cRol bulunamadı: " + roleId));
            return;
        }

        switch (action) {
            case "info" -> {
                sender.sendMessage(ColorUtil.colorize("&8&m--------------------------------------------------"));
                sender.sendMessage(ColorUtil.colorize(" &f&lROL BİLGİSİ: &e" + role.getName() + " &7(" + role.getId() + ")"));
                sender.sendMessage(ColorUtil.colorize(" &8▪ &7Ağırlık: &f" + role.getWeight() + " &8| &7Prefix: " + role.getPrefix() + "Oyuncu"));
                sender.sendMessage(ColorUtil.colorize(" &8▪ &7Miras Alınanlar: &f" + (role.getInheritance().isEmpty() ? "Yok" : String.join(", ", role.getInheritance()))));
                sender.sendMessage(ColorUtil.colorize(" &8▪ &7İzinler (" + role.getPermissions().size() + "): &f" + String.join(", ", role.getPermissions())));
                sender.sendMessage(ColorUtil.colorize("&8&m--------------------------------------------------"));
            }

            case "delete" -> {
                if (role.isDefault() || role.getId().equals("default")) {
                    sender.sendMessage(prefix() + ColorUtil.colorize("&cVarsayılan rol silinemez!"));
                    return;
                }
                Map<String, Role> updatedRoles = new HashMap<>(manager.getRolesMap());
                updatedRoles.remove(role.getId());
                manager.applySnapshot(updatedRoles, null, manager.getDefaultRoleId(), manager.getCurrentRevision());

                JsonObject mut = new JsonObject();
                mut.addProperty("operation", "delete_role");
                mut.addProperty("role_id", role.getId());
                syncService.pushMutationAsync(mut);

                sender.sendMessage(prefix() + ColorUtil.colorize("&aRol silindi: &f" + role.getId()));
            }

            case "addperm" -> {
                if (args.length < 4) {
                    sender.sendMessage(prefix() + ColorUtil.colorize("&cKullanım: /gperm role <id> addperm <izin> [sunucu]"));
                    return;
                }
                String perm = args[3].trim();
                String srv = args.length >= 5 ? args[4].trim().toLowerCase(Locale.ROOT) : "";
                if (!srv.isEmpty() && !srv.equals("global")) {
                    role.getServerPermissions().computeIfAbsent(srv, k -> new ArrayList<>());
                    if (!role.getServerPermissions().get(srv).contains(perm)) {
                        role.getServerPermissions().get(srv).add(perm);
                    }
                } else if (!role.getPermissions().contains(perm)) {
                    role.getPermissions().add(perm);
                }
                refreshAllAfterRoleEdit();

                JsonObject mut = new JsonObject();
                mut.addProperty("operation", "add_role_perm");
                mut.addProperty("role_id", role.getId());
                mut.addProperty("permission", perm);
                mut.addProperty("server", srv);
                syncService.pushMutationAsync(mut);

                sender.sendMessage(prefix() + ColorUtil.colorize("&e" + role.getName() + " &7rolüne &f" + perm + " &7izni eklendi."));
            }

            case "delperm", "removeperm" -> {
                if (args.length < 4) {
                    sender.sendMessage(prefix() + ColorUtil.colorize("&cKullanım: /gperm role <id> delperm <izin> [sunucu]"));
                    return;
                }
                String perm = args[3].trim();
                String srv = args.length >= 5 ? args[4].trim().toLowerCase(Locale.ROOT) : "";
                if (!srv.isEmpty() && !srv.equals("global")) {
                    List<String> list = role.getServerPermissions().get(srv);
                    if (list != null) list.removeIf(p -> p.equalsIgnoreCase(perm));
                } else {
                    role.getPermissions().removeIf(p -> p.equalsIgnoreCase(perm));
                }
                refreshAllAfterRoleEdit();

                JsonObject mut = new JsonObject();
                mut.addProperty("operation", "remove_role_perm");
                mut.addProperty("role_id", role.getId());
                mut.addProperty("permission", perm);
                mut.addProperty("server", srv);
                syncService.pushMutationAsync(mut);

                sender.sendMessage(prefix() + ColorUtil.colorize("&e" + role.getName() + " &7rolünden &f" + perm + " &7izni kaldırıldı."));
            }

            case "setprefix" -> {
                if (args.length < 4) {
                    sender.sendMessage(prefix() + ColorUtil.colorize("&cKullanım: /gperm role <id> setprefix <prefix>"));
                    return;
                }
                String prefixVal = String.join(" ", Arrays.copyOfRange(args, 3, args.length)) + " ";
                role.setPrefix(prefixVal);
                refreshAllAfterRoleEdit();

                JsonObject mut = new JsonObject();
                mut.addProperty("operation", "update_role_meta");
                mut.addProperty("role_id", role.getId());
                mut.addProperty("prefix", prefixVal);
                syncService.pushMutationAsync(mut);

                sender.sendMessage(prefix() + ColorUtil.colorize("&aPrefix güncellendi: " + prefixVal + "Oyuncu"));
            }

            case "setsuffix" -> {
                String suffixVal = args.length >= 4 ? (" " + String.join(" ", Arrays.copyOfRange(args, 3, args.length))) : "";
                role.setSuffix(suffixVal);
                refreshAllAfterRoleEdit();

                JsonObject mut = new JsonObject();
                mut.addProperty("operation", "update_role_meta");
                mut.addProperty("role_id", role.getId());
                mut.addProperty("suffix", suffixVal);
                syncService.pushMutationAsync(mut);

                sender.sendMessage(prefix() + ColorUtil.colorize("&aSuffix güncellendi."));
            }

            case "setweight" -> {
                if (args.length < 4) return;
                int w = parseDaysSafe(args[3], role.getWeight());
                role.setWeight(w);
                refreshAllAfterRoleEdit();

                JsonObject mut = new JsonObject();
                mut.addProperty("operation", "update_role_meta");
                mut.addProperty("role_id", role.getId());
                mut.addProperty("weight", w);
                syncService.pushMutationAsync(mut);

                sender.sendMessage(prefix() + ColorUtil.colorize("&aAğırlık güncellendi: &f" + w));
            }

            case "parent" -> {
                if (args.length < 5) {
                    sender.sendMessage(prefix() + ColorUtil.colorize("&cKullanım: /gperm role <id> parent <add|remove> <ustRol>"));
                    return;
                }
                String mode = args[3].toLowerCase(Locale.ROOT);
                String parentId = args[4].toLowerCase(Locale.ROOT);
                JsonObject mut = new JsonObject();
                mut.addProperty("operation", "update_role_meta");
                mut.addProperty("role_id", role.getId());

                if (mode.equals("add")) {
                    if (!role.getInheritance().contains(parentId) && !parentId.equals(role.getId())) {
                        role.getInheritance().add(parentId);
                    }
                    mut.addProperty("add_parent", parentId);
                } else {
                    role.getInheritance().removeIf(p -> p.equalsIgnoreCase(parentId));
                    mut.addProperty("remove_parent", parentId);
                }
                refreshAllAfterRoleEdit();
                syncService.pushMutationAsync(mut);
                sender.sendMessage(prefix() + ColorUtil.colorize("&aMiras ağacı güncellendi."));
            }

            default -> sender.sendMessage(prefix() + ColorUtil.colorize("&cBilinmeyen rol alt komutu: " + action));
        }
    }

    private void grantLocalRole(String username, Role role, int days, String source, boolean queueCelebration) {
        UserData u = manager.getOrCreateUserData(username);
        long now = System.currentTimeMillis() / 1000L;
        long addSec = days > 0 ? (days * 86400L) : 0L;
        boolean found = false;

        for (RoleAssignment ass : u.getRoles()) {
            if (ass.getRoleId().equalsIgnoreCase(role.getId())) {
                found = true;
                if (addSec == 0L) {
                    ass.setExpiresAt(0L);
                } else if (ass.getExpiresAt() > 0L) {
                    long base = Math.max(now, ass.getExpiresAt());
                    ass.setExpiresAt(base + addSec);
                }
                ass.setSource(source);
                break;
            }
        }

        if (!found) {
            long exp = addSec > 0 ? (now + addSec) : 0L;
            u.getRoles().add(new RoleAssignment(role.getId(), now, exp, source));
        }

        if (queueCelebration) {
            PendingDelivery del = new PendingDelivery();
            del.setId("IG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
            del.setRoleId(role.getId());
            del.setRoleName(role.getName());
            del.setDurationDays(days);
            del.setCreatedAt(now);
            del.setCelebrated(false);
            u.getPendingDeliveries().add(del);
        }

        manager.updateSingleUser(u, manager.getCurrentRevision());
    }

    private void refreshAllAfterRoleEdit() {
        manager.applySnapshot(new HashMap<>(manager.getRolesMap()), null, manager.getDefaultRoleId(), manager.getCurrentRevision());
    }

    private int parseDaysSafe(String raw, int fallback) {
        try {
            return Math.max(0, Integer.parseInt(raw.replaceAll("[^0-9]", "")));
        } catch (Exception e) {
            return fallback;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ColorUtil.colorize("&8&m--------------------------------------------------"));
        sender.sendMessage(ColorUtil.colorize(" &f&lGRIZM&7&lPERMS &8» &7Komut Rehberi &8(&fSunucu: " + manager.getServerId() + "&8)"));
        sender.sendMessage(ColorUtil.colorize(" &f/gperm roles &8- &7Tüm rolleri ve ağırlıklarını listeler"));
        sender.sendMessage(ColorUtil.colorize(" &f/gperm givevip <oyuncu> <vipRol> [gün] &8- &7Süreli VIP + kutlama verir"));
        sender.sendMessage(ColorUtil.colorize(" &f/gperm user <oyuncu> <info|setrole|addrole|delrole|addperm|delperm>"));
        sender.sendMessage(ColorUtil.colorize(" &f/gperm role <id> <info|create|delete|addperm|delperm|setprefix|setweight>"));
        sender.sendMessage(ColorUtil.colorize(" &f/gperm sync [oyuncu] &8- &7Web sitesi ile anında eşitler"));
        sender.sendMessage(ColorUtil.colorize(" &f/gperm reload &8- &7Yapılandırmayı yeniden yükler"));
        sender.sendMessage(ColorUtil.colorize("&8&m--------------------------------------------------"));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.isOp() && !sender.hasPermission("grizmperms.admin")) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return filterPrefix(Arrays.asList("help", "roles", "givevip", "user", "role", "sync", "reload"), args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("givevip")) {
            if (args.length == 2) return onlinePlayerNames(args[1]);
            if (args.length == 3) {
                return filterPrefix(manager.getAllRolesSorted().stream().map(Role::getId).toList(), args[2]);
            }
            if (args.length == 4) return filterPrefix(Arrays.asList("7", "15", "30", "60", "90"), args[3]);
        }
        if (sub.equals("user")) {
            if (args.length == 2) return onlinePlayerNames(args[1]);
            if (args.length == 3) {
                return filterPrefix(Arrays.asList("info", "setrole", "addrole", "delrole", "addperm", "delperm"), args[2]);
            }
            if (args.length == 4 && (args[2].equalsIgnoreCase("setrole") || args[2].equalsIgnoreCase("addrole") || args[2].equalsIgnoreCase("delrole"))) {
                return filterPrefix(manager.getAllRolesSorted().stream().map(Role::getId).toList(), args[3]);
            }
            if (args.length == 5 && (args[2].equalsIgnoreCase("addperm") || args[2].equalsIgnoreCase("delperm"))) {
                return filterPrefix(Arrays.asList("global", "lobby", "skyblock", "smp", "pvp"), args[4]);
            }
        }
        if (sub.equals("role") || sub.equals("group")) {
            if (args.length == 2) {
                return filterPrefix(manager.getAllRolesSorted().stream().map(Role::getId).toList(), args[1]);
            }
            if (args.length == 3) {
                return filterPrefix(Arrays.asList("info", "create", "delete", "addperm", "delperm", "setprefix", "setsuffix", "setweight", "parent"), args[2]);
            }
            if (args.length == 5 && (args[2].equalsIgnoreCase("addperm") || args[2].equalsIgnoreCase("delperm"))) {
                return filterPrefix(Arrays.asList("global", "lobby", "skyblock", "smp", "pvp"), args[4]);
            }
        }
        if (sub.equals("sync") && args.length == 2) {
            return onlinePlayerNames(args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> onlinePlayerNames(String prefix) {
        return filterPrefix(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), prefix);
    }

    private List<String> filterPrefix(Collection<String> options, String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(opt -> opt.toLowerCase(Locale.ROOT).startsWith(lower))
                .collect(Collectors.toList());
    }
}

