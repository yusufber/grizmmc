package com.grizmtech.towny.claim;

import com.grizmtech.towny.core.ChunkKey;
import com.grizmtech.towny.core.ConfigManager;
import com.grizmtech.towny.core.MessageUtil;
import com.grizmtech.towny.hooks.VaultHook;
import com.grizmtech.towny.storage.DatabaseManager;
import com.grizmtech.towny.town.Town;
import com.grizmtech.towny.town.TownManager;
import com.grizmtech.towny.town.TownMember;
import com.grizmtech.towny.town.TownRole;
import com.grizmtech.towny.war.WarManager;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ClaimManager {

    private final JavaPlugin plugin;
    private final DatabaseManager databaseManager;
    private final ConfigManager configManager;
    private final TownManager townManager;
    private final VaultHook vaultHook;
    private WarManager warManager; // Setter ile bağlanır (çapraz bağımlılık önleme)

    // worldUuid -> chunkKey -> ClaimChunk
    private final Map<UUID, ConcurrentHashMap<Long, ClaimChunk>> claimsByWorld = new ConcurrentHashMap<>();
    // townUuid -> Set<ChunkKey>
    private final Map<UUID, Set<Long>> townClaims = new ConcurrentHashMap<>();

    public ClaimManager(JavaPlugin plugin, DatabaseManager databaseManager, ConfigManager configManager,
                        TownManager townManager, VaultHook vaultHook) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.configManager = configManager;
        this.townManager = townManager;
        this.vaultHook = vaultHook;

        loadAll();
    }

    public void setWarManager(WarManager warManager) {
        this.warManager = warManager;
    }

    private void loadAll() {
        List<ClaimChunk> claims = databaseManager.loadClaims();
        for (ClaimChunk claim : claims) {
            claimsByWorld.computeIfAbsent(claim.getWorldUuid(), k -> new ConcurrentHashMap<>())
                    .put(claim.getChunkKey(), claim);
            townClaims.computeIfAbsent(claim.getTownUuid(), k -> ConcurrentHashMap.newKeySet())
                    .add(claim.getChunkKey());
        }
        plugin.getLogger().info("Yüklendi: " + claims.size() + " chunk arazisi.");
    }

    public ClaimChunk getClaim(World world, int chunkX, int chunkZ) {
        if (world == null) return null;
        Map<Long, ClaimChunk> worldMap = claimsByWorld.get(world.getUID());
        if (worldMap == null) return null;
        return worldMap.get(ChunkKey.pack(chunkX, chunkZ));
    }

    public ClaimChunk getClaim(Location location) {
        if (location == null || location.getWorld() == null) return null;
        return getClaim(location.getWorld(), location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    public boolean isClaimed(World world, int chunkX, int chunkZ) {
        return getClaim(world, chunkX, chunkZ) != null;
    }

    public Set<Long> getTownClaims(UUID townUuid) {
        return townClaims.getOrDefault(townUuid, Collections.emptySet());
    }

    public int getTownClaimCount(UUID townUuid) {
        Set<Long> set = townClaims.get(townUuid);
        return set != null ? set.size() : 0;
    }

    public boolean isAdjacent(Town town, World world, int chunkX, int chunkZ) {
        Map<Long, ClaimChunk> worldMap = claimsByWorld.get(world.getUID());
        if (worldMap == null) return false;

        int[][] neighbors = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] offset : neighbors) {
            long key = ChunkKey.pack(chunkX + offset[0], chunkZ + offset[1]);
            ClaimChunk neighbor = worldMap.get(key);
            if (neighbor != null && neighbor.getTownUuid().equals(town.getUuid())) {
                return true;
            }
        }
        return false;
    }

    public boolean claim(Player player, boolean isOutpost) {
        if (player.getWorld().getName().equalsIgnoreCase("lobby")) {
            MessageUtil.sendMessage(player, "<red>Lobi dünyasında arazi alamazsınız!");
            return false;
        }

        Town town = townManager.getTownByPlayer(player.getUniqueId());
        if (town == null) {
            MessageUtil.sendMessage(player, "<red>Bir kasabaya üye değilsiniz!");
            return false;
        }

        TownMember member = townManager.getMember(player.getUniqueId());
        if (member == null || !member.getRole().canClaim()) {
            MessageUtil.sendMessage(player, "<red>Kasaba arazisi genişletme yetkiniz yok!");
            return false;
        }

        Location loc = player.getLocation();
        World world = loc.getWorld();
        int chunkX = loc.getBlockX() >> 4;
        int chunkZ = loc.getBlockZ() >> 4;
        long key = ChunkKey.pack(chunkX, chunkZ);

        if (isClaimed(world, chunkX, chunkZ)) {
            MessageUtil.sendMessage(player, "<red>Bu arazi zaten sahiplenilmiş!");
            return false;
        }

        int maxClaims = town.getMaxClaims(configManager.getBaseMaxClaims(), configManager.getClaimsPerLevel());
        int currentClaims = getTownClaimCount(town.getUuid());

        if (currentClaims >= maxClaims) {
            MessageUtil.sendMessage(player, "<red>Kasabanız maksimum arazi sınırına ulaştı (" + maxClaims + ")! Seviye atlayarak sınırı yükseltin.");
            return false;
        }

        // Bitişiklik kontrolü (Homeblock dışında ilk claim hariç)
        if (currentClaims > 0 && !isOutpost) {
            if (!isAdjacent(town, world, chunkX, chunkZ)) {
                MessageUtil.sendMessage(player, "<red>Yeni araziniz mevcut kasaba topraklarına bitişik olmalıdır! Uzak bir karakol için: <yellow>/t claim outpost</yellow></red>");
                return false;
            }
        }

        double cost = configManager.getClaimCost();
        if (isOutpost) {
            cost *= configManager.getOutpostMultiplier();
        }

        if (town.getBankBalance() < cost) {
            MessageUtil.sendMessage(player, "<red>Kasaba hazinesinde yeterli bakiye yok! Gerekli: <yellow>" + cost + "$</yellow></red>");
            return false;
        }

        town.withdraw(cost);
        databaseManager.saveTownAsync(town);

        ClaimChunk newClaim = new ClaimChunk(world.getUID(), chunkX, chunkZ, town.getUuid(),
                PlotType.DEFAULT, null, 0.0, 0L, isOutpost);

        claimsByWorld.computeIfAbsent(world.getUID(), k -> new ConcurrentHashMap<>()).put(key, newClaim);
        townClaims.computeIfAbsent(town.getUuid(), k -> ConcurrentHashMap.newKeySet()).add(key);

        databaseManager.saveClaimAsync(newClaim);

        MessageUtil.sendMessage(player, String.format("<green>Bu chunk başarıyla kasabanıza katıldı! (%d/%d)</green>", currentClaims + 1, maxClaims));
        return true;
    }

    public boolean unclaim(Player player) {
        Town town = townManager.getTownByPlayer(player.getUniqueId());
        if (town == null) {
            MessageUtil.sendMessage(player, "<red>Bir kasabaya üye değilsiniz!");
            return false;
        }

        TownMember member = townManager.getMember(player.getUniqueId());
        if (member == null || !member.getRole().canUnclaim()) {
            MessageUtil.sendMessage(player, "<red>Kasaba arazisini terk etme yetkiniz yok!");
            return false;
        }

        Location loc = player.getLocation();
        World world = loc.getWorld();
        int chunkX = loc.getBlockX() >> 4;
        int chunkZ = loc.getBlockZ() >> 4;
        long key = ChunkKey.pack(chunkX, chunkZ);

        ClaimChunk claim = getClaim(world, chunkX, chunkZ);
        if (claim == null || !claim.getTownUuid().equals(town.getUuid())) {
            MessageUtil.sendMessage(player, "<red>Bu arazi kasabanıza ait değil!");
            return false;
        }

        // Homeblock kontrolü
        Location home = town.getHomeLocation();
        if (home != null && home.getWorld().equals(world)) {
            int homeX = home.getBlockX() >> 4;
            int homeZ = home.getBlockZ() >> 4;
            if (homeX == chunkX && homeZ == chunkZ && getTownClaimCount(town.getUuid()) > 1) {
                MessageUtil.sendMessage(player, "<red>Kasabanın merkez (Home) chunk'ını terk edemezsiniz! Önce '/t setspawn' ile merkezi taşıyın.");
                return false;
            }
        }

        // Bellekten sil
        claimsByWorld.get(world.getUID()).remove(key);
        Set<Long> set = townClaims.get(town.getUuid());
        if (set != null) {
            set.remove(key);
        }

        databaseManager.deleteClaimAsync(world.getUID(), chunkX, chunkZ);

        MessageUtil.sendMessage(player, "<yellow>Bu arazi kasabanızdan çıkarıldı ve vahşi doğaya devredildi.</yellow>");
        return true;
    }

    public void unclaimAll(Town town) {
        UUID townUuid = town.getUuid();
        Set<Long> claims = townClaims.remove(townUuid);
        if (claims == null) return;

        for (Map<Long, ClaimChunk> worldMap : claimsByWorld.values()) {
            worldMap.values().removeIf(c -> c.getTownUuid().equals(townUuid));
        }
    }

    public boolean canPerform(Player player, Location location, PlotFlag flag) {
        ClaimChunk claim = getClaim(location);
        if (claim == null) {
            // Vahşi doğada her şey serbesttir
            return true;
        }

        Town town = townManager.getTown(claim.getTownUuid());
        if (town == null) return true;

        // Harabe Modu: 24 saatlik süre bittiyse koruma kalkmıştır, yağmaya açık!
        if (town.isRuined(configManager.getRuinDurationHours() * 3600000L)) {
            return true;
        }

        // Aktif Kuşatma: Kuşatma sancağı alanında patlama, merdiven ve kızıltaş kullanımı serbesttir
        if (warManager != null && warManager.isSiegeActiveInChunk(claim.getWorldUuid(), claim.getChunkX(), claim.getChunkZ())) {
            if (flag == PlotFlag.EXPLOSION || flag == PlotFlag.BLOCK_PLACE || flag == PlotFlag.BLOCK_BREAK) {
                return true;
            }
        }

        // Yönetici yetkisi bypass
        if (player.hasPermission("grizmtowny.bypass")) {
            return true;
        }

        // Oyuncunun ilişki grubunu tespit et
        PlotGroup group = PlotGroup.OUTSIDER;
        Town playerTown = townManager.getTownByPlayer(player.getUniqueId());

        if (playerTown != null) {
            if (playerTown.getUuid().equals(town.getUuid())) {
                group = PlotGroup.RESIDENT;
            } else if (playerTown.getNationUuid() != null && playerTown.getNationUuid().equals(town.getNationUuid())) {
                group = PlotGroup.ALLY;
            }
        }

        // Eğer parsel bir üyeye aitse ve işlem yapan o üyeyse izin ver
        if (claim.getPlotOwnerUuid() != null && claim.getPlotOwnerUuid().equals(player.getUniqueId())) {
            return true;
        }

        // Belediye başkanı veya Co-Mayor kasaba içinde tam yetkiye sahiptir
        if (group == PlotGroup.RESIDENT) {
            TownMember member = townManager.getMember(player.getUniqueId());
            if (member != null && member.getRole().isHigherOrEqual(TownRole.CO_MAYOR)) {
                return true;
            }
        }

        return claim.hasFlag(group, flag);
    }
}
