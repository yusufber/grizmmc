package com.grizmtech.towny.visual;

import com.grizmtech.towny.claim.ClaimManager;
import com.grizmtech.towny.core.ConfigManager;
import com.grizmtech.towny.core.MessageUtil;
import com.grizmtech.towny.storage.DatabaseManager;
import com.grizmtech.towny.town.Town;
import com.grizmtech.towny.town.TownManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.LocalDate;

public class UpkeepTask {

    private final JavaPlugin plugin;
    private final TownManager townManager;
    private final ClaimManager claimManager;
    private final DatabaseManager databaseManager;
    private final ConfigManager configManager;

    private LocalDate lastUpkeepDate = LocalDate.now();
    private BukkitTask task;

    public UpkeepTask(JavaPlugin plugin, TownManager townManager, ClaimManager claimManager,
                      DatabaseManager databaseManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.townManager = townManager;
        this.claimManager = claimManager;
        this.databaseManager = databaseManager;
        this.configManager = configManager;

        start();
    }

    private void start() {
        // Her 5 dakikada bir kontrol et
        this.task = new BukkitRunnable() {
            @Override
            public void run() {
                checkDailyUpkeep();
                checkRuinedTowns();
            }
        }.runTaskTimer(plugin, 6000L, 6000L);
    }

    private void checkDailyUpkeep() {
        LocalDate today = LocalDate.now();
        if (today.isAfter(lastUpkeepDate)) {
            lastUpkeepDate = today;
            plugin.getLogger().info("Günlük kasaba vergi (upkeep) tahsilatı başlatılıyor...");

            double perChunk = configManager.getUpkeepPerChunk();

            for (Town town : townManager.getAllTowns()) {
                int claims = claimManager.getTownClaimCount(town.getUuid());
                if (claims <= 0) continue;

                double totalCost = claims * perChunk;

                if (town.getBankBalance() >= totalCost) {
                    town.withdraw(totalCost);
                    databaseManager.saveTownAsync(town);
                    townManager.broadcastToTown(town.getUuid(),
                            "<gray>Günlük kasaba bakım vergisi (<yellow>" + totalCost + "$</yellow>) hazineden tahsil edildi.</gray>");
                } else {
                    // Yetersiz bakiye -> Harabe koruma modunu başlat
                    if (town.getUpkeepWarningTime() <= 0) {
                        town.setUpkeepWarningTime(System.currentTimeMillis());
                        databaseManager.saveTownAsync(town);
                        townManager.broadcastToTown(town.getUuid(),
                                "<red><b>DİKKAT!</b> Kasaba hazinesinde günlük vergi için yeterli bakiye yok! " +
                                        "<yellow>24 saatlik Harabe Koruması</yellow> başladı. Hazineye para yatırılmazsa kasaba yıkılacaktır!</red>");
                    }
                }
            }
        }
    }

    private void checkRuinedTowns() {
        long ruinDurationMillis = configManager.getRuinDurationHours() * 3600000L;

        for (Town town : townManager.getAllTowns()) {
            if (town.isInRuinWarning(ruinDurationMillis)) {
                // Hâlâ uyarı aşamasında
                long remainingMillis = town.getRemainingRuinMillis(ruinDurationMillis);
                long hours = remainingMillis / 3600000L;
                long minutes = (remainingMillis % 3600000L) / 60000L;
                // Bilgilendir
            } else if (town.isRuined(ruinDurationMillis)) {
                // Harabeye döndü
                // Bir defalık duyuru yap
                MessageUtil.broadcast(String.format(
                        "<red><b>⚠ HARABE DUYURUSU ⚠</b></red>\n" +
                        "<yellow><b>%s</b></yellow> kasabası borçlarını ödeyemediği için <red><b>HARABEYE</b></red> dönüştü!\n" +
                        "<gray>Tüm arazi korumaları kalktı ve sandıkları yağmaya açıldı!</gray>", town.getName()));
            }
        }
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
        }
    }
}
