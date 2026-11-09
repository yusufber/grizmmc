package com.grizmtech.towny.war;

import com.grizmtech.towny.claim.ClaimChunk;
import com.grizmtech.towny.claim.ClaimManager;
import com.grizmtech.towny.core.ConfigManager;
import com.grizmtech.towny.core.MessageUtil;
import com.grizmtech.towny.storage.DatabaseManager;
import com.grizmtech.towny.town.Town;
import com.grizmtech.towny.town.TownManager;
import com.grizmtech.towny.town.TownMember;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WarManager {

    private final JavaPlugin plugin;
    private final DatabaseManager databaseManager;
    private final ConfigManager configManager;
    private final TownManager townManager;
    private final ClaimManager claimManager;
    private final BlockRollbackManager rollbackManager;

    private final NamespacedKey bannerKey;
    private final Map<Integer, SiegeWar> activeWars = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> townActiveWarMap = new ConcurrentHashMap<>();
    private int nextWarId = 1;

    private BukkitTask siegeTask;

    public WarManager(JavaPlugin plugin, DatabaseManager databaseManager, ConfigManager configManager,
                      TownManager townManager, ClaimManager claimManager, BlockRollbackManager rollbackManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.configManager = configManager;
        this.townManager = townManager;
        this.claimManager = claimManager;
        this.rollbackManager = rollbackManager;

        this.bannerKey = new NamespacedKey(plugin, "siege_banner");

        startSiegeTicker();
    }

    private void startSiegeTicker() {
        this.siegeTask = new BukkitRunnable() {
            @Override
            public void run() {
                checkWarsTick();
            }
        }.runTaskTimer(plugin, 20L, 20L); // Her saniye (20 tick)
    }

    private void checkWarsTick() {
        LocalTime now = LocalTime.now();
        int currentHour = now.getHour();
        boolean isPrimeTime = (currentHour >= configManager.getPrimeTimeStartHour() && currentHour < configManager.getPrimeTimeEndHour());

        double radius = configManager.getSiegeBannerRadius();
        int winScore = configManager.getSiegePointsToWin();

        for (SiegeWar war : activeWars.values()) {
            Town attacker = townManager.getTown(war.getAttackerTownUuid());
            Town defender = townManager.getTown(war.getDefenderTownUuid());
            if (attacker == null || defender == null) {
                endWar(war, null);
                continue;
            }

            // Durum makinesi: Gece / Ateşkes Penceresi kontrolü
            if (war.getState() == WarState.ACTIVE_SIEGE && !isPrimeTime) {
                war.setState(WarState.PEACE_WINDOW);
                MessageUtil.broadcast(String.format("<aqua>⚔ <b>%s</b> ile <b>%s</b> arasındaki kuşatmaya gece ateşkesi girdi!</aqua>",
                        attacker.getName(), defender.getName()));
            } else if (war.getState() == WarState.PEACE_WINDOW && isPrimeTime) {
                war.setState(WarState.ACTIVE_SIEGE);
                MessageUtil.broadcast(String.format("<red>⚔ <b>%s</b> ile <b>%s</b> arasındaki kuşatma Prime-Time ile yeniden başladı!</red>",
                        attacker.getName(), defender.getName()));
            }

            // Kuşatma aktifse puanlama ve BossBar güncelleme
            if (war.getState() == WarState.ACTIVE_SIEGE && war.getBannerLocation() != null) {
                Location bannerLoc = war.getBannerLocation();
                int attackersInZone = 0;
                int defendersInZone = 0;

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (war.isInsideSiegeZone(player.getLocation(), radius)) {
                        war.showBossBar(player);
                        Town pTown = townManager.getTownByPlayer(player.getUniqueId());
                        if (pTown != null) {
                            if (pTown.getUuid().equals(attacker.getUuid())) {
                                attackersInZone++;
                            } else if (pTown.getUuid().equals(defender.getUuid())) {
                                defendersInZone++;
                            }
                        }
                    } else {
                        war.hideBossBar(player);
                    }
                }

                // Saniye başı puan ekle
                if (attackersInZone > 0) {
                    war.addScoreAttacker(attackersInZone * configManager.getSiegeTickPoints());
                }
                if (defendersInZone > 0) {
                    war.addScoreDefender(defendersInZone * configManager.getSiegeTickPoints());
                }

                war.updateBossBar(attacker.getName(), defender.getName(), winScore);

                // Zafer kontrolü
                if (war.getScoreAttacker() >= winScore) {
                    endWar(war, attacker);
                } else if (war.getScoreDefender() >= winScore) {
                    endWar(war, defender);
                }
            }
        }
    }

    public boolean declareWar(Player leader, String targetTownName) {
        Town attacker = townManager.getTownByPlayer(leader.getUniqueId());
        if (attacker == null) {
            MessageUtil.sendMessage(leader, "<red>Bir kasabaya üye değilsiniz!");
            return false;
        }

        TownMember member = townManager.getMember(leader.getUniqueId());
        if (member == null || !member.getRole().canDeclareWar()) {
            MessageUtil.sendMessage(leader, "<red>Savaş ilan etme yetkiniz yok!");
            return false;
        }

        Town defender = townManager.getTown(targetTownName);
        if (defender == null) {
            MessageUtil.sendMessage(leader, "<red>Hedef kasaba bulunamadı!");
            return false;
        }

        if (attacker.getUuid().equals(defender.getUuid())) {
            MessageUtil.sendMessage(leader, "<red>Kendi kasabanıza savaş ilan edemezsiniz!");
            return false;
        }

        if (townActiveWarMap.containsKey(attacker.getUuid()) || townActiveWarMap.containsKey(defender.getUuid())) {
            MessageUtil.sendMessage(leader, "<red>Taraflardan birinin aktif bir savaşı zaten mevcut!");
            return false;
        }

        if (townManager.isUnderTruce(defender.getUuid())) {
            MessageUtil.sendMessage(leader, "<red>Bu kasabanın savaş dokunulmazlığı (Truce) devam ediyor!");
            return false;
        }

        double cost = configManager.getWarDeclareCost();
        if (attacker.getBankBalance() < cost) {
            MessageUtil.sendMessage(leader, "<red>Kasaba kasasında savaş ilanı için yeterli bakiye yok! Gerekli: <yellow>" + cost + "$</yellow></red>");
            return false;
        }

        attacker.withdraw(cost);
        databaseManager.saveTownAsync(attacker);

        int warId = nextWarId++;
        long now = System.currentTimeMillis();
        long prepEnd = now + (2 * 3600000L); // 2 saat hazırlık süresi

        SiegeWar war = new SiegeWar(warId, attacker.getUuid(), defender.getUuid(), WarState.DECLARED,
                0, 0, null, now, prepEnd);

        activeWars.put(warId, war);
        townActiveWarMap.put(attacker.getUuid(), warId);
        townActiveWarMap.put(defender.getUuid(), warId);

        // Lidere Kuşatma Sancağı ver
        ItemStack banner = createSiegeBanner(warId, attacker.getName(), defender.getName());
        leader.getInventory().addItem(banner);

        MessageUtil.broadcast(String.format("<gradient:#ff0844:#ffb199><b>⚔ SAVAŞ İLANI! ⚔</b></gradient>\n" +
                "<yellow><b>%s</b></yellow> kasabası, <red><b>%s</b></red> kasabasına savaş ilan etti!\n" +
                "<gray>Saldıran taraf sancağını diktiğinde kuşatma başlayacaktır!</gray>",
                attacker.getName(), defender.getName()));

        MessageUtil.sendMessage(leader, "<green>Kuşatma sancağı envanterinize verildi. Hedef kasaba sınırına bitişik vahşi doğaya dikin!</green>");
        return true;
    }

    public ItemStack createSiegeBanner(int warId, String attackerName, String defenderName) {
        ItemStack item = new ItemStack(Material.RED_BANNER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse("<red><b>Kuşatma Sancağı</b></red>"));
            List<Component> lore = List.of(
                    MessageUtil.parse("<gray>Saldıran: <yellow>" + attackerName + "</yellow></gray>"),
                    MessageUtil.parse("<gray>Savunan: <red>" + defenderName + "</red></gray>"),
                    MessageUtil.parse("<dark_gray>Hedef kasabanın dış sınırına bitişik</dark_gray>"),
                    MessageUtil.parse("<dark_gray>vahşi doğa bloğuna dikin!</dark_gray>")
            );
            meta.lore(lore);
            meta.getPersistentDataContainer().set(bannerKey, PersistentDataType.INTEGER, warId);
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isSiegeBannerItem(ItemStack item) {
        if (item == null || item.getType() != Material.RED_BANNER) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(bannerKey, PersistentDataType.INTEGER);
    }

    public Integer getWarIdFromBanner(ItemStack item) {
        if (!isSiegeBannerItem(item)) return null;
        return item.getItemMeta().getPersistentDataContainer().get(bannerKey, PersistentDataType.INTEGER);
    }

    public boolean placeSiegeBanner(Player player, Location placeLoc, int warId) {
        SiegeWar war = activeWars.get(warId);
        if (war == null) {
            MessageUtil.sendMessage(player, "<red>Geçersiz savaş sancağı!</red>");
            return false;
        }

        // Sancak vahşi doğaya dikilmeli
        ClaimChunk currentClaim = claimManager.getClaim(placeLoc);
        if (currentClaim != null) {
            MessageUtil.sendMessage(player, "<red>Sancak sahipsiz vahşi doğaya dikilmelidir!</red>");
            return false;
        }

        // Hedef kasaba sınırına bitişik mi kontrol et (2 chunk mesafe içinde olmalı)
        Town defender = townManager.getTown(war.getDefenderTownUuid());
        int cx = placeLoc.getBlockX() >> 4;
        int cz = placeLoc.getBlockZ() >> 4;
        boolean adjacentToDefender = false;

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                ClaimChunk c = claimManager.getClaim(placeLoc.getWorld(), cx + dx, cz + dz);
                if (c != null && c.getTownUuid().equals(defender.getUuid())) {
                    adjacentToDefender = true;
                    break;
                }
            }
        }

        if (!adjacentToDefender) {
            MessageUtil.sendMessage(player, "<red>Sancak hedef kasabanın sınırına (en fazla 2 chunk) yakın olmalıdır!</red>");
            return false;
        }

        war.setBannerLocation(placeLoc);
        war.setState(WarState.ACTIVE_SIEGE);

        Town attacker = townManager.getTown(war.getAttackerTownUuid());
        MessageUtil.broadcast(String.format("<gradient:#ff0844:#ffb199><b>⚔ KUŞATMA BAŞLADI! ⚔</b></gradient>\n" +
                "<yellow><b>%s</b></yellow> kasabası, <red><b>%s</b></red> sınırına sancağı dikti!\n" +
                "<gray>24 blok yarıçapındaki alanda kıyasıya mücadele başladı!</gray>",
                attacker.getName(), defender.getName()));

        return true;
    }

    public void handleKillInSiege(Player killer, Player victim) {
        for (SiegeWar war : activeWars.values()) {
            if (war.getState() != WarState.ACTIVE_SIEGE) continue;
            if (war.isInsideSiegeZone(victim.getLocation(), configManager.getSiegeBannerRadius())) {
                Town killerTown = townManager.getTownByPlayer(killer.getUniqueId());
                Town victimTown = townManager.getTownByPlayer(victim.getUniqueId());

                if (killerTown != null && victimTown != null) {
                    if (killerTown.getUuid().equals(war.getAttackerTownUuid()) && victimTown.getUuid().equals(war.getDefenderTownUuid())) {
                        war.addScoreAttacker(configManager.getSiegeKillPoints());
                        MessageUtil.sendMessage(killer, "<red>+25 Kuşatma Puanı kazandınız!</red>");
                    } else if (killerTown.getUuid().equals(war.getDefenderTownUuid()) && victimTown.getUuid().equals(war.getAttackerTownUuid())) {
                        war.addScoreDefender(configManager.getSiegeKillPoints());
                        MessageUtil.sendMessage(killer, "<blue>+25 Savunma Puanı kazandınız!</blue>");
                    }
                }
            }
        }
    }

    public void endWar(SiegeWar war, Town winner) {
        war.setState(WarState.RESOLUTION);
        war.cleanup();

        Town attacker = townManager.getTown(war.getAttackerTownUuid());
        Town defender = townManager.getTown(war.getDefenderTownUuid());

        String winnerName = winner != null ? winner.getName() : "Belirlenemedi";
        MessageUtil.broadcast(String.format("<gold><b>⚔ KUŞATMA SONA ERDİ! ⚔</b></gold>\n" +
                "<green>Kazanan taraf: <b>%s</b></green>! Kaybeden kasabaya 48 saatlik dokunulmazlık verildi.", winnerName));

        // Kaybedene 48 saat truce ver
        if (winner != null) {
            Town loser = winner.getUuid().equals(attacker.getUuid()) ? defender : attacker;
            if (loser != null) {
                townManager.setTruce(loser.getUuid(), configManager.getWarTruceHours());
            }
        }

        // Rollback başlat
        rollbackManager.startRollback(war.getWarId(), () -> {
            plugin.getLogger().info("Kuşatma savaş alanındaki bloklar başarıyla eski haline getirildi (WarId: " + war.getWarId() + ").");
        });

        activeWars.remove(war.getWarId());
        townActiveWarMap.remove(war.getAttackerTownUuid());
        townActiveWarMap.remove(war.getDefenderTownUuid());
    }

    public boolean isBesieged(UUID townUuid) {
        Integer warId = townActiveWarMap.get(townUuid);
        if (warId == null) return false;
        SiegeWar war = activeWars.get(warId);
        return war != null && war.getState() == WarState.ACTIVE_SIEGE;
    }

    public boolean isSiegeActiveInChunk(UUID worldUuid, int chunkX, int chunkZ) {
        for (SiegeWar war : activeWars.values()) {
            if (war.getState() != WarState.ACTIVE_SIEGE || war.getBannerLocation() == null) continue;
            Location banner = war.getBannerLocation();
            if (banner.getWorld().getUID().equals(worldUuid)) {
                int bx = banner.getBlockX() >> 4;
                int bz = banner.getBlockZ() >> 4;
                if (Math.abs(bx - chunkX) <= 1 && Math.abs(bz - chunkZ) <= 1) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isSiegeBannerLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        for (SiegeWar war : activeWars.values()) {
            Location banner = war.getBannerLocation();
            if (banner != null && banner.getWorld().equals(loc.getWorld())) {
                if (banner.getBlockX() == loc.getBlockX() &&
                    banner.getBlockY() == loc.getBlockY() &&
                    banner.getBlockZ() == loc.getBlockZ()) {
                    return true;
                }
            }
        }
        return false;
    }

    public SiegeWar getWarByTown(UUID townUuid) {
        Integer id = townActiveWarMap.get(townUuid);
        return id != null ? activeWars.get(id) : null;
    }

    public void shutdown() {
        if (siegeTask != null) {
            siegeTask.cancel();
        }
        for (SiegeWar war : activeWars.values()) {
            war.cleanup();
        }
        activeWars.clear();
        townActiveWarMap.clear();
    }
}
