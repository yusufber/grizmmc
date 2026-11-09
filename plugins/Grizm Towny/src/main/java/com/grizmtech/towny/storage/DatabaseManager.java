package com.grizmtech.towny.storage;

import com.grizmtech.towny.claim.ClaimChunk;
import com.grizmtech.towny.claim.PlotType;
import com.grizmtech.towny.core.ConfigManager;
import com.grizmtech.towny.teleport.Warp;
import com.grizmtech.towny.town.Town;
import com.grizmtech.towny.town.TownMember;
import com.grizmtech.towny.town.TownRole;
import com.grizmtech.towny.war.SiegeWar;
import com.grizmtech.towny.war.WarState;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseManager {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private HikariDataSource dataSource;
    private final ExecutorService asyncExecutor = Executors.newFixedThreadPool(3);

    public DatabaseManager(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        initPool();
        initTables();
    }

    private void initPool() {
        HikariConfig hikari = new HikariConfig();
        String type = configManager.getDbType();

        if ("MYSQL".equalsIgnoreCase(type)) {
            hikari.setDriverClassName("com.mysql.cj.jdbc.Driver");
            hikari.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8",
                    configManager.getDbHost(), configManager.getDbPort(), configManager.getDbDatabase()));
            hikari.setUsername(configManager.getDbUsername());
            hikari.setPassword(configManager.getDbPassword());
            hikari.setMaximumPoolSize(10);
            hikari.setMinimumIdle(2);
        } else {
            File dbFile = new File(plugin.getDataFolder(), "towny.db");
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            hikari.setDriverClassName("org.sqlite.JDBC");
            hikari.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            hikari.setMaximumPoolSize(1);
        }

        hikari.setPoolName("GrizmTowny-Pool");
        hikari.setConnectionTimeout(30000);
        hikari.setMaxLifetime(1800000);

        this.dataSource = new HikariDataSource(hikari);
    }

    private void initTables() {
        try (Connection con = dataSource.getConnection(); Statement stmt = con.createStatement()) {
            // Towns
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS towns (
                    town_uuid VARCHAR(36) PRIMARY KEY,
                    name VARCHAR(32) NOT NULL UNIQUE,
                    leader_uuid VARCHAR(36) NOT NULL,
                    nation_uuid VARCHAR(36),
                    bank_balance DOUBLE DEFAULT 0.0,
                    level INT DEFAULT 1,
                    home_world VARCHAR(64),
                    home_x DOUBLE DEFAULT 0.0,
                    home_y DOUBLE DEFAULT 0.0,
                    home_z DOUBLE DEFAULT 0.0,
                    home_yaw FLOAT DEFAULT 0.0,
                    home_pitch FLOAT DEFAULT 0.0,
                    spawn_cost DOUBLE DEFAULT 0.0,
                    upkeep_warning_time BIGINT DEFAULT 0,
                    created_at BIGINT NOT NULL
                );
            """);

            // Members
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS town_members (
                    player_uuid VARCHAR(36) PRIMARY KEY,
                    town_uuid VARCHAR(36) NOT NULL,
                    role VARCHAR(20) NOT NULL,
                    joined_at BIGINT NOT NULL,
                    contributed_money DOUBLE DEFAULT 0.0
                );
            """);

            // Claims
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS claims (
                    world_uuid VARCHAR(36) NOT NULL,
                    chunk_x INT NOT NULL,
                    chunk_z INT NOT NULL,
                    town_uuid VARCHAR(36) NOT NULL,
                    plot_type VARCHAR(20) NOT NULL,
                    owner_uuid VARCHAR(36),
                    price DOUBLE DEFAULT 0.0,
                    flags_bitset BIGINT DEFAULT 0,
                    is_outpost BOOLEAN DEFAULT FALSE,
                    PRIMARY KEY (world_uuid, chunk_x, chunk_z)
                );
            """);

            // Wars
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS wars (
                    war_id INTEGER PRIMARY KEY,
                    attacker_town_uuid VARCHAR(36) NOT NULL,
                    defender_town_uuid VARCHAR(36) NOT NULL,
                    state VARCHAR(20) NOT NULL,
                    score_attacker INT DEFAULT 0,
                    score_defender INT DEFAULT 0,
                    banner_world VARCHAR(64),
                    banner_x DOUBLE DEFAULT 0.0,
                    banner_y DOUBLE DEFAULT 0.0,
                    banner_z DOUBLE DEFAULT 0.0,
                    started_at BIGINT NOT NULL,
                    phase_end_at BIGINT NOT NULL
                );
            """);

            // Warps
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS warps (
                    warp_id VARCHAR(64) PRIMARY KEY,
                    town_uuid VARCHAR(36),
                    world_uuid VARCHAR(36) NOT NULL,
                    x DOUBLE NOT NULL,
                    y DOUBLE NOT NULL,
                    z DOUBLE NOT NULL,
                    yaw FLOAT NOT NULL,
                    pitch FLOAT NOT NULL,
                    visit_cost DOUBLE DEFAULT 0.0,
                    permission VARCHAR(64)
                );
            """);

            // Cooldowns
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS player_cooldowns (
                    player_uuid VARCHAR(36) PRIMARY KEY,
                    last_rtp_time BIGINT DEFAULT 0,
                    last_town_teleport BIGINT DEFAULT 0,
                    combat_until BIGINT DEFAULT 0
                );
            """);

            // Truces
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS town_truces (
                    town_uuid VARCHAR(36) PRIMARY KEY,
                    immune_until BIGINT NOT NULL
                );
            """);

        } catch (SQLException e) {
            plugin.getLogger().severe("Veritabanı tabloları oluşturulurken hata: " + e.getMessage());
        }
    }

    // --- ASYNC SAVE METHODS ---

    public void saveTownAsync(Town town) {
        asyncExecutor.submit(() -> {
            String sql = """
                INSERT INTO towns (town_uuid, name, leader_uuid, nation_uuid, bank_balance, level,
                                  home_world, home_x, home_y, home_z, home_yaw, home_pitch,
                                  spawn_cost, upkeep_warning_time, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(town_uuid) DO UPDATE SET
                    name = excluded.name,
                    leader_uuid = excluded.leader_uuid,
                    nation_uuid = excluded.nation_uuid,
                    bank_balance = excluded.bank_balance,
                    level = excluded.level,
                    home_world = excluded.home_world,
                    home_x = excluded.home_x,
                    home_y = excluded.home_y,
                    home_z = excluded.home_z,
                    home_yaw = excluded.home_yaw,
                    home_pitch = excluded.home_pitch,
                    spawn_cost = excluded.spawn_cost,
                    upkeep_warning_time = excluded.upkeep_warning_time;
            """;
            try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, town.getUuid().toString());
                ps.setString(2, town.getName());
                ps.setString(3, town.getLeaderUuid().toString());
                ps.setString(4, town.getNationUuid() != null ? town.getNationUuid().toString() : null);
                ps.setDouble(5, town.getBankBalance());
                ps.setInt(6, town.getLevel());

                Location home = town.getHomeLocation();
                if (home != null && home.getWorld() != null) {
                    ps.setString(7, home.getWorld().getName());
                    ps.setDouble(8, home.getX());
                    ps.setDouble(9, home.getY());
                    ps.setDouble(10, home.getZ());
                    ps.setFloat(11, home.getYaw());
                    ps.setFloat(12, home.getPitch());
                } else {
                    ps.setString(7, null);
                    ps.setDouble(8, 0);
                    ps.setDouble(9, 0);
                    ps.setDouble(10, 0);
                    ps.setFloat(11, 0);
                    ps.setFloat(12, 0);
                }

                ps.setDouble(13, town.getSpawnCost());
                ps.setLong(14, town.getUpkeepWarningTime());
                ps.setLong(15, town.getCreatedAt());

                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("Kasaba kaydedilemedi (" + town.getName() + "): " + e.getMessage());
            }
        });
    }

    public void deleteTownAsync(UUID townUuid) {
        asyncExecutor.submit(() -> {
            try (Connection con = dataSource.getConnection()) {
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM towns WHERE town_uuid = ?")) {
                    ps.setString(1, townUuid.toString());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM town_members WHERE town_uuid = ?")) {
                    ps.setString(1, townUuid.toString());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM claims WHERE town_uuid = ?")) {
                    ps.setString(1, townUuid.toString());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM warps WHERE town_uuid = ?")) {
                    ps.setString(1, townUuid.toString());
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Kasaba silinirken hata: " + e.getMessage());
            }
        });
    }

    public void saveMemberAsync(TownMember member) {
        asyncExecutor.submit(() -> {
            String sql = """
                INSERT INTO town_members (player_uuid, town_uuid, role, joined_at, contributed_money)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(player_uuid) DO UPDATE SET
                    town_uuid = excluded.town_uuid,
                    role = excluded.role,
                    contributed_money = excluded.contributed_money;
            """;
            try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, member.getPlayerUuid().toString());
                ps.setString(2, member.getTownUuid().toString());
                ps.setString(3, member.getRole().name());
                ps.setLong(4, member.getJoinedAt());
                ps.setDouble(5, member.getContributedMoney());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("Üye kaydedilemedi: " + e.getMessage());
            }
        });
    }

    public void deleteMemberAsync(UUID playerUuid) {
        asyncExecutor.submit(() -> {
            try (Connection con = dataSource.getConnection();
                 PreparedStatement ps = con.prepareStatement("DELETE FROM town_members WHERE player_uuid = ?")) {
                ps.setString(1, playerUuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("Üye silinemedi: " + e.getMessage());
            }
        });
    }

    public void saveClaimAsync(ClaimChunk claim) {
        asyncExecutor.submit(() -> {
            String sql = """
                INSERT INTO claims (world_uuid, chunk_x, chunk_z, town_uuid, plot_type, owner_uuid, price, flags_bitset, is_outpost)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(world_uuid, chunk_x, chunk_z) DO UPDATE SET
                    town_uuid = excluded.town_uuid,
                    plot_type = excluded.plot_type,
                    owner_uuid = excluded.owner_uuid,
                    price = excluded.price,
                    flags_bitset = excluded.flags_bitset,
                    is_outpost = excluded.is_outpost;
            """;
            try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, claim.getWorldUuid().toString());
                ps.setInt(2, claim.getChunkX());
                ps.setInt(3, claim.getChunkZ());
                ps.setString(4, claim.getTownUuid().toString());
                ps.setString(5, claim.getPlotType().name());
                ps.setString(6, claim.getPlotOwnerUuid() != null ? claim.getPlotOwnerUuid().toString() : null);
                ps.setDouble(7, claim.getPrice());
                ps.setLong(8, claim.getFlagsBitset());
                ps.setBoolean(9, claim.isOutpost());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("Claim kaydedilemedi: " + e.getMessage());
            }
        });
    }

    public void deleteClaimAsync(UUID worldUuid, int chunkX, int chunkZ) {
        asyncExecutor.submit(() -> {
            String sql = "DELETE FROM claims WHERE world_uuid = ? AND chunk_x = ? AND chunk_z = ?";
            try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, worldUuid.toString());
                ps.setInt(2, chunkX);
                ps.setInt(3, chunkZ);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("Claim silinemedi: " + e.getMessage());
            }
        });
    }

    public void saveWarpAsync(Warp warp) {
        asyncExecutor.submit(() -> {
            String sql = """
                INSERT INTO warps (warp_id, town_uuid, world_uuid, x, y, z, yaw, pitch, visit_cost, permission)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(warp_id) DO UPDATE SET
                    town_uuid = excluded.town_uuid,
                    world_uuid = excluded.world_uuid,
                    x = excluded.x,
                    y = excluded.y,
                    z = excluded.z,
                    yaw = excluded.yaw,
                    pitch = excluded.pitch,
                    visit_cost = excluded.visit_cost,
                    permission = excluded.permission;
            """;
            try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, warp.getWarpId().toLowerCase());
                ps.setString(2, warp.getTownUuid() != null ? warp.getTownUuid().toString() : null);
                ps.setString(3, warp.getWorldUuid().toString());
                ps.setDouble(4, warp.getX());
                ps.setDouble(5, warp.getY());
                ps.setDouble(6, warp.getZ());
                ps.setFloat(7, warp.getYaw());
                ps.setFloat(8, warp.getPitch());
                ps.setDouble(9, warp.getVisitCost());
                ps.setString(10, warp.getPermission());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("Warp kaydedilemedi: " + e.getMessage());
            }
        });
    }

    public void deleteWarpAsync(String warpId) {
        asyncExecutor.submit(() -> {
            String sql = "DELETE FROM warps WHERE warp_id = ?";
            try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, warpId.toLowerCase());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("Warp silinemedi: " + e.getMessage());
            }
        });
    }

    public void saveTruceAsync(UUID townUuid, long immuneUntil) {
        asyncExecutor.submit(() -> {
            String sql = """
                INSERT INTO town_truces (town_uuid, immune_until)
                VALUES (?, ?)
                ON CONFLICT(town_uuid) DO UPDATE SET immune_until = excluded.immune_until;
            """;
            try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, townUuid.toString());
                ps.setLong(2, immuneUntil);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("Barış süresi kaydedilemedi: " + e.getMessage());
            }
        });
    }

    // --- SYNC LOADING METHODS ON STARTUP ---

    public Map<UUID, Town> loadTowns() {
        Map<UUID, Town> map = new HashMap<>();
        String sql = "SELECT * FROM towns";
        try (Connection con = dataSource.getConnection(); Statement stmt = con.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("town_uuid"));
                String name = rs.getString("name");
                UUID leaderUuid = UUID.fromString(rs.getString("leader_uuid"));
                String nationStr = rs.getString("nation_uuid");
                UUID nationUuid = nationStr != null ? UUID.fromString(nationStr) : null;
                double bank = rs.getDouble("bank_balance");
                int level = rs.getInt("level");

                String worldName = rs.getString("home_world");
                Location home = null;
                if (worldName != null) {
                    World w = Bukkit.getWorld(worldName);
                    if (w != null) {
                        home = new Location(w, rs.getDouble("home_x"), rs.getDouble("home_y"), rs.getDouble("home_z"),
                                rs.getFloat("home_yaw"), rs.getFloat("home_pitch"));
                    }
                }

                double spawnCost = rs.getDouble("spawn_cost");
                long upkeepWarning = rs.getLong("upkeep_warning_time");
                long created = rs.getLong("created_at");

                Town town = new Town(uuid, name, leaderUuid, nationUuid, bank, level, home, spawnCost, upkeepWarning, created);
                map.put(uuid, town);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Kasabalar yüklenirken hata: " + e.getMessage());
        }
        return map;
    }

    public Map<UUID, TownMember> loadMembers() {
        Map<UUID, TownMember> map = new HashMap<>();
        String sql = "SELECT * FROM town_members";
        try (Connection con = dataSource.getConnection(); Statement stmt = con.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                UUID playerUuid = UUID.fromString(rs.getString("player_uuid"));
                UUID townUuid = UUID.fromString(rs.getString("town_uuid"));
                TownRole role = TownRole.valueOf(rs.getString("role"));
                long joined = rs.getLong("joined_at");
                double contrib = rs.getDouble("contributed_money");

                map.put(playerUuid, new TownMember(playerUuid, townUuid, role, joined, contrib));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Kasaba üyeleri yüklenirken hata: " + e.getMessage());
        }
        return map;
    }

    public List<ClaimChunk> loadClaims() {
        List<ClaimChunk> list = new ArrayList<>();
        String sql = "SELECT * FROM claims";
        try (Connection con = dataSource.getConnection(); Statement stmt = con.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                UUID worldUuid = UUID.fromString(rs.getString("world_uuid"));
                int chunkX = rs.getInt("chunk_x");
                int chunkZ = rs.getInt("chunk_z");
                UUID townUuid = UUID.fromString(rs.getString("town_uuid"));
                PlotType plotType = PlotType.valueOf(rs.getString("plot_type"));

                String ownerStr = rs.getString("owner_uuid");
                UUID ownerUuid = ownerStr != null ? UUID.fromString(ownerStr) : null;

                double price = rs.getDouble("price");
                long flags = rs.getLong("flags_bitset");
                boolean outpost = rs.getBoolean("is_outpost");

                list.add(new ClaimChunk(worldUuid, chunkX, chunkZ, townUuid, plotType, ownerUuid, price, flags, outpost));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Araziler yüklenirken hata: " + e.getMessage());
        }
        return list;
    }

    public Map<String, Warp> loadWarps() {
        Map<String, Warp> map = new HashMap<>();
        String sql = "SELECT * FROM warps";
        try (Connection con = dataSource.getConnection(); Statement stmt = con.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String warpId = rs.getString("warp_id");
                String townStr = rs.getString("town_uuid");
                UUID townUuid = townStr != null ? UUID.fromString(townStr) : null;
                UUID worldUuid = UUID.fromString(rs.getString("world_uuid"));

                double x = rs.getDouble("x");
                double y = rs.getDouble("y");
                double z = rs.getDouble("z");
                float yaw = rs.getFloat("yaw");
                float pitch = rs.getFloat("pitch");
                double cost = rs.getDouble("visit_cost");
                String perm = rs.getString("permission");

                map.put(warpId.toLowerCase(), new Warp(warpId, townUuid, worldUuid, x, y, z, yaw, pitch, cost, perm));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Warplar yüklenirken hata: " + e.getMessage());
        }
        return map;
    }

    public Map<UUID, Long> loadTruces() {
        Map<UUID, Long> map = new HashMap<>();
        String sql = "SELECT * FROM town_truces";
        try (Connection con = dataSource.getConnection(); Statement stmt = con.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                UUID townUuid = UUID.fromString(rs.getString("town_uuid"));
                long immuneUntil = rs.getLong("immune_until");
                if (System.currentTimeMillis() < immuneUntil) {
                    map.put(townUuid, immuneUntil);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Barış süreleri yüklenirken hata: " + e.getMessage());
        }
        return map;
    }

    public void shutdown() {
        asyncExecutor.shutdown();
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
