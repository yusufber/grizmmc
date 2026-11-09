package com.grizmtech.towny.war;

import com.grizmtech.towny.core.MessageUtil;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SiegeWar {

    private int warId;
    private final UUID attackerTownUuid;
    private final UUID defenderTownUuid;
    private WarState state;
    private int scoreAttacker;
    private int scoreDefender;
    private Location bannerLocation;
    private final long startedAt;
    private long phaseEndAt;

    private BossBar bossBar;
    private final Set<UUID> viewers = new HashSet<>();

    public SiegeWar(int warId, UUID attackerTownUuid, UUID defenderTownUuid, WarState state,
                    int scoreAttacker, int scoreDefender, Location bannerLocation, long startedAt, long phaseEndAt) {
        this.warId = warId;
        this.attackerTownUuid = attackerTownUuid;
        this.defenderTownUuid = defenderTownUuid;
        this.state = state != null ? state : WarState.DECLARED;
        this.scoreAttacker = scoreAttacker;
        this.scoreDefender = scoreDefender;
        this.bannerLocation = bannerLocation;
        this.startedAt = startedAt;
        this.phaseEndAt = phaseEndAt;

        initBossBar();
    }

    private void initBossBar() {
        this.bossBar = BossBar.bossBar(
                Component.text("Kuşatma Savaş Alanı"),
                0.5f,
                BossBar.Color.RED,
                BossBar.Overlay.PROGRESS
        );
        updateBossBar(null, null, 500);
    }

    public void updateBossBar(String attackerName, String defenderName, int targetScore) {
        if (bossBar == null) return;

        String aName = attackerName != null ? attackerName : "Saldıran";
        String dName = defenderName != null ? defenderName : "Savunan";

        int total = Math.max(1, scoreAttacker + scoreDefender);
        float progress = Math.min(1.0f, Math.max(0.0f, (float) scoreAttacker / Math.max(1, targetScore)));

        String title = String.format("<red><b>%s</b> [%d]</red> <gray>⚔ Kuşatma ⚔</gray> <blue><b>%s</b> [%d]</blue>",
                aName, scoreAttacker, dName, scoreDefender);

        bossBar.name(MessageUtil.parse(title));
        bossBar.progress(progress);

        if (scoreAttacker > scoreDefender) {
            bossBar.color(BossBar.Color.RED);
        } else if (scoreDefender > scoreAttacker) {
            bossBar.color(BossBar.Color.BLUE);
        } else {
            bossBar.color(BossBar.Color.YELLOW);
        }
    }

    public boolean isInsideSiegeZone(Location loc, double radius) {
        if (bannerLocation == null || loc == null || loc.getWorld() == null) return false;
        if (!bannerLocation.getWorld().equals(loc.getWorld())) return false;
        return bannerLocation.distanceSquared(loc) <= (radius * radius);
    }

    public void showBossBar(Player player) {
        if (bossBar != null && player != null) {
            player.showBossBar(bossBar);
            viewers.add(player.getUniqueId());
        }
    }

    public void hideBossBar(Player player) {
        if (bossBar != null && player != null) {
            player.hideBossBar(bossBar);
            viewers.remove(player.getUniqueId());
        }
    }

    public void cleanup() {
        if (bossBar != null) {
            for (UUID uuid : new HashSet<>(viewers)) {
                Player p = org.bukkit.Bukkit.getPlayer(uuid);
                if (p != null) {
                    p.hideBossBar(bossBar);
                }
            }
            viewers.clear();
        }
    }

    public int getWarId() { return warId; }
    public void setWarId(int warId) { this.warId = warId; }
    public UUID getAttackerTownUuid() { return attackerTownUuid; }
    public UUID getDefenderTownUuid() { return defenderTownUuid; }
    public WarState getState() { return state; }
    public void setState(WarState state) { this.state = state; }
    public int getScoreAttacker() { return scoreAttacker; }
    public void addScoreAttacker(int pts) { this.scoreAttacker += pts; }
    public int getScoreDefender() { return scoreDefender; }
    public void addScoreDefender(int pts) { this.scoreDefender += pts; }
    public Location getBannerLocation() { return bannerLocation; }
    public void setBannerLocation(Location bannerLocation) { this.bannerLocation = bannerLocation; }
    public long getStartedAt() { return startedAt; }
    public long getPhaseEndAt() { return phaseEndAt; }
    public void setPhaseEndAt(long phaseEndAt) { this.phaseEndAt = phaseEndAt; }
    public BossBar getBossBar() { return bossBar; }
}
