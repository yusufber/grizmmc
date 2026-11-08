package com.grizmtech.perms.engine;

import com.grizmtech.perms.GrizmPermsPlugin;
import com.grizmtech.perms.model.Role;
import com.grizmtech.perms.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Tüm sunucularda (Lobby, SkyBlock, Towny SMP, PvP) oyuncuların:
 * 1) TAB listesindeki isim, rol ve izin (perm) etiketlerini,
 * 2) Karakter üstü (3D Nametag / Overhead) isim, rol ve izin etiketlerini
 * hem doğrudan TAB API (me.neznamy.tab.api.TabAPI) üzerinden hem de
 * yerleşik Bukkit Scoreboard Teams & PlayerListName üzerinden senkronize eder.
 */
public class NametagAndTabManager {

    private final GrizmPermsPlugin plugin;
    private final PermissionManager manager;

    public NametagAndTabManager(GrizmPermsPlugin plugin, PermissionManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public void updateAllOnlinePlayers() {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, this::updateAllOnlinePlayers);
            return;
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            updatePlayer(online);
        }
    }

    @SuppressWarnings("deprecation")
    public void updatePlayer(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> updatePlayer(player));
            return;
        }

        Role primary = manager.getPrimaryRole(player.getName());
        String rawPrefix = manager.getDisplayPrefix(player.getName());
        String rawSuffix = manager.getDisplaySuffix(player.getName());

        String colorizedPrefix = ColorUtil.colorize(rawPrefix);
        String colorizedSuffix = ColorUtil.colorize(rawSuffix);
        ChatColor nameColor = ColorUtil.extractLastChatColor(rawPrefix);
        String teamName = buildSortedTeamName(primary);

        boolean tabHandledNametag = false;
        boolean tabHandledTablist = false;

        // 1. TAB Eklentisi (TAB v4/v5/v6) yüklüyse doğrudan TabAPI üzerinden Baş Üstü & TAB formatla
        if (Bukkit.getPluginManager().isPluginEnabled("TAB")) {
            TabHookResult res = applyViaTabApi(player.getUniqueId(), rawPrefix, rawSuffix, teamName);
            tabHandledNametag = res.nametagUpdated;
            tabHandledTablist = res.tablistUpdated;
        }

        // 2. TAB tablist'i devralmadıysa Bukkit PlayerListName'i doğrudan güncelle
        if (!tabHandledTablist) {
            try {
                player.setPlayerListName(colorizedPrefix + player.getName() + colorizedSuffix);
            } catch (Throwable ignored) {
            }
        }

        // 3. TAB nametag'i devralmadıysa Bukkit Scoreboard Team ile 3D Karakter Üstü İsmi güncelle
        if (!tabHandledNametag) {
            applyBukkitScoreboardTeam(player, teamName, colorizedPrefix, colorizedSuffix, nameColor);
        }
    }

    @SuppressWarnings("deprecation")
    private void applyBukkitScoreboardTeam(Player target, String teamName, String colorizedPrefix, String colorizedSuffix, ChatColor nameColor) {
        ScoreboardManager sbManager = Bukkit.getScoreboardManager();
        if (sbManager == null) {
            return;
        }

        Set<Scoreboard> boards = new HashSet<>();
        boards.add(sbManager.getMainScoreboard());
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.getScoreboard() != null) {
                boards.add(viewer.getScoreboard());
            }
        }

        String entry = target.getName();
        for (Scoreboard board : boards) {
            try {
                // Oyuncuyu eski gp_ takımlarından çıkar
                for (Team existing : board.getTeams()) {
                    if (existing.getName().startsWith("gp_") && !existing.getName().equals(teamName)) {
                        if (existing.hasEntry(entry)) {
                            existing.removeEntry(entry);
                        }
                    }
                }

                Team team = board.getTeam(teamName);
                if (team == null) {
                    team = board.registerNewTeam(teamName);
                }
                team.setPrefix(colorizedPrefix);
                team.setSuffix(colorizedSuffix);
                if (nameColor != null) {
                    team.setColor(nameColor);
                }
                if (!team.hasEntry(entry)) {
                    team.addEntry(entry);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    public void removePlayerFromTeams(Player player) {
        if (player == null) return;
        ScoreboardManager sbManager = Bukkit.getScoreboardManager();
        if (sbManager == null) return;
        String entry = player.getName();
        try {
            Scoreboard main = sbManager.getMainScoreboard();
            for (Team t : main.getTeams()) {
                if (t.getName().startsWith("gp_") && t.hasEntry(entry)) {
                    t.removeEntry(entry);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private String buildSortedTeamName(Role role) {
        int weight = role != null ? role.getWeight() : 10;
        int order = Math.max(0, 9999 - Math.min(9999, weight));
        String cleanId = (role != null ? role.getId() : "default")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
        if (cleanId.length() > 7) {
            cleanId = cleanId.substring(0, 7);
        }
        return String.format(Locale.ROOT, "gp_%04d_%s", order, cleanId);
    }

    private static final class TabHookResult {
        final boolean nametagUpdated;
        final boolean tablistUpdated;

        TabHookResult(boolean nametagUpdated, boolean tablistUpdated) {
            this.nametagUpdated = nametagUpdated;
            this.tablistUpdated = tablistUpdated;
        }
    }

    private TabHookResult applyViaTabApi(UUID playerUuid, String rawPrefix, String rawSuffix, String sortTeamName) {
        boolean nametagOk = false;
        boolean tablistOk = false;
        try {
            Class<?> tabApiClass = Class.forName("me.neznamy.tab.api.TabAPI");
            Method getInstance = tabApiClass.getMethod("getInstance");
            Object api = getInstance.invoke(null);
            if (api == null) {
                return new TabHookResult(false, false);
            }

            Method getPlayer = tabApiClass.getMethod("getPlayer", UUID.class);
            Object tabPlayer = getPlayer.invoke(api, playerUuid);
            if (tabPlayer == null) {
                return new TabHookResult(false, false);
            }

            Class<?> tabPlayerClass = Class.forName("me.neznamy.tab.api.TabPlayer");

            // 1) Karakter Üstü 3D İsim (NameTagManager)
            try {
                Method getNameTagManager = tabApiClass.getMethod("getNameTagManager");
                Object nameTagManager = getNameTagManager.invoke(api);
                if (nameTagManager != null) {
                    Method setPrefix = nameTagManager.getClass().getMethod("setPrefix", tabPlayerClass, String.class);
                    Method setSuffix = nameTagManager.getClass().getMethod("setSuffix", tabPlayerClass, String.class);
                    setPrefix.invoke(nameTagManager, tabPlayer, rawPrefix);
                    setSuffix.invoke(nameTagManager, tabPlayer, rawSuffix);
                    nametagOk = true;
                }
            } catch (Throwable ignored) {
            }

            // 2) TAB Listesi (TabListFormatManager)
            try {
                Method getTabListFormatManager = tabApiClass.getMethod("getTabListFormatManager");
                Object tabListManager = getTabListFormatManager.invoke(api);
                if (tabListManager != null) {
                    Method setPrefix = tabListManager.getClass().getMethod("setPrefix", tabPlayerClass, String.class);
                    Method setSuffix = tabListManager.getClass().getMethod("setSuffix", tabPlayerClass, String.class);
                    setPrefix.invoke(tabListManager, tabPlayer, rawPrefix);
                    setSuffix.invoke(tabListManager, tabPlayer, rawSuffix);
                    tablistOk = true;
                }
            } catch (Throwable ignored) {
            }

            // 3) TAB Sıralaması (SortingManager)
            try {
                Method getSortingManager = tabApiClass.getMethod("getSortingManager");
                Object sortingManager = getSortingManager.invoke(api);
                if (sortingManager != null) {
                    Method forceTeamName = sortingManager.getClass().getMethod("forceTeamName", tabPlayerClass, String.class);
                    forceTeamName.invoke(sortingManager, tabPlayer, sortTeamName);
                }
            } catch (Throwable ignored) {
            }
        } catch (Throwable ignored) {
        }
        return new TabHookResult(nametagOk, tablistOk);
    }
}

