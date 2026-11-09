package com.grizmtech.towny.core;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class ConfigManager {

    private final JavaPlugin plugin;

    // Database
    private String dbType;
    private String dbHost;
    private int dbPort;
    private String dbDatabase;
    private String dbUsername;
    private String dbPassword;

    // Town
    private double townCreateCost;
    private double claimCost;
    private double outpostMultiplier;
    private double upkeepPerChunk;
    private long ruinDurationHours;
    private int baseMaxClaims;
    private int claimsPerLevel;
    private int baseMaxMembers;
    private int membersPerLevel;

    // War / Siege
    private double warDeclareCost;
    private int siegeBannerRadius;
    private int siegePointsToWin;
    private int siegeKillPoints;
    private int siegeTickPoints;
    private int siegeRespawnDelaySeconds;
    private long warTruceHours;
    private int primeTimeStartHour;
    private int primeTimeEndHour;

    // RTP
    private int rtpMinRadius;
    private int rtpMaxRadius;
    private int rtpMaxAttempts;
    private long rtpCooldownSeconds;
    private int rtpClaimSafeRadiusChunks;

    // Combat Tag
    private int combatTagSeconds;
    private List<String> combatBlockedCommands;

    // TPA
    private int tpaTimeoutSeconds;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        String prefixStr = config.getString("messages.prefix", "<gradient:#ff8a00:#e52e71><b>GRIZM TOWNY</b></gradient> <dark_gray>»</dark_gray> ");
        MessageUtil.setPrefix(prefixStr);

        // Database
        this.dbType = config.getString("database.type", "SQLITE").toUpperCase();
        this.dbHost = config.getString("database.host", "localhost");
        this.dbPort = config.getInt("database.port", 3306);
        this.dbDatabase = config.getString("database.database", "towny");
        this.dbUsername = config.getString("database.username", "root");
        this.dbPassword = config.getString("database.password", "");

        // Town
        this.townCreateCost = config.getDouble("town.create-cost", 1000.0);
        this.claimCost = config.getDouble("town.claim-cost", 100.0);
        this.outpostMultiplier = config.getDouble("town.outpost-multiplier", 2.5);
        this.upkeepPerChunk = config.getDouble("town.upkeep-per-chunk", 10.0);
        this.ruinDurationHours = config.getLong("town.ruin-duration-hours", 24L);
        this.baseMaxClaims = config.getInt("town.base-max-claims", 16);
        this.claimsPerLevel = config.getInt("town.claims-per-level", 8);
        this.baseMaxMembers = config.getInt("town.base-max-members", 10);
        this.membersPerLevel = config.getInt("town.members-per-level", 5);

        // War
        this.warDeclareCost = config.getDouble("war.declare-cost", 5000.0);
        this.siegeBannerRadius = config.getInt("war.banner-radius", 24);
        this.siegePointsToWin = config.getInt("war.points-to-win", 500);
        this.siegeKillPoints = config.getInt("war.kill-points", 25);
        this.siegeTickPoints = config.getInt("war.tick-points", 1);
        this.siegeRespawnDelaySeconds = config.getInt("war.respawn-delay-seconds", 15);
        this.warTruceHours = config.getLong("war.truce-hours", 48L);
        this.primeTimeStartHour = config.getInt("war.prime-time-start-hour", 18);
        this.primeTimeEndHour = config.getInt("war.prime-time-end-hour", 23);

        // RTP
        this.rtpMinRadius = config.getInt("rtp.min-radius", 500);
        this.rtpMaxRadius = config.getInt("rtp.max-radius", 7500);
        this.rtpMaxAttempts = config.getInt("rtp.max-attempts", 10);
        this.rtpCooldownSeconds = config.getLong("rtp.cooldown-seconds", 60L);
        this.rtpClaimSafeRadiusChunks = config.getInt("rtp.claim-safe-radius-chunks", 6);

        // Combat Tag
        this.combatTagSeconds = config.getInt("combat.tag-seconds", 15);
        this.combatBlockedCommands = config.getStringList("combat.blocked-commands");
        if (this.combatBlockedCommands.isEmpty()) {
            this.combatBlockedCommands = List.of("tpa", "tpahere", "tpaccept", "rtp", "warp", "spawn", "home", "town spawn");
        }

        // TPA
        this.tpaTimeoutSeconds = config.getInt("tpa.timeout-seconds", 60);
    }

    public String getDbType() { return dbType; }
    public String getDbHost() { return dbHost; }
    public int getDbPort() { return dbPort; }
    public String getDbDatabase() { return dbDatabase; }
    public String getDbUsername() { return dbUsername; }
    public String getDbPassword() { return dbPassword; }

    public double getTownCreateCost() { return townCreateCost; }
    public double getClaimCost() { return claimCost; }
    public double getOutpostMultiplier() { return outpostMultiplier; }
    public double getUpkeepPerChunk() { return upkeepPerChunk; }
    public long getRuinDurationHours() { return ruinDurationHours; }
    public int getBaseMaxClaims() { return baseMaxClaims; }
    public int getClaimsPerLevel() { return claimsPerLevel; }
    public int getBaseMaxMembers() { return baseMaxMembers; }
    public int getMembersPerLevel() { return membersPerLevel; }

    public double getWarDeclareCost() { return warDeclareCost; }
    public int getSiegeBannerRadius() { return siegeBannerRadius; }
    public int getSiegePointsToWin() { return siegePointsToWin; }
    public int getSiegeKillPoints() { return siegeKillPoints; }
    public int getSiegeTickPoints() { return siegeTickPoints; }
    public int getSiegeRespawnDelaySeconds() { return siegeRespawnDelaySeconds; }
    public long getWarTruceHours() { return warTruceHours; }
    public int getPrimeTimeStartHour() { return primeTimeStartHour; }
    public int getPrimeTimeEndHour() { return primeTimeEndHour; }

    public int getRtpMinRadius() { return rtpMinRadius; }
    public int getRtpMaxRadius() { return rtpMaxRadius; }
    public int getRtpMaxAttempts() { return rtpMaxAttempts; }
    public long getRtpCooldownSeconds() { return rtpCooldownSeconds; }
    public int getRtpClaimSafeRadiusChunks() { return rtpClaimSafeRadiusChunks; }

    public int getCombatTagSeconds() { return combatTagSeconds; }
    public List<String> getCombatBlockedCommands() { return combatBlockedCommands; }

    public int getTpaTimeoutSeconds() { return tpaTimeoutSeconds; }
}
