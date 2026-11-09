package com.grizmtech.towny.teleport;

import com.grizmtech.towny.claim.ClaimManager;
import com.grizmtech.towny.combat.CombatTagManager;
import com.grizmtech.towny.core.ConfigManager;
import com.grizmtech.towny.core.MessageUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RTPManager {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final ClaimManager claimManager;
    private final CombatTagManager combatTagManager;

    private final Random random = new Random();
    private final Map<UUID, Long> rtpCooldowns = new ConcurrentHashMap<>();

    private static final Set<Material> DANGEROUS_BLOCKS = Set.of(
            Material.LAVA, Material.WATER, Material.MAGMA_BLOCK,
            Material.CACTUS, Material.POWDER_SNOW, Material.FIRE,
            Material.SOUL_FIRE, Material.VOID_AIR, Material.AIR
    );

    public RTPManager(JavaPlugin plugin, ConfigManager configManager, ClaimManager claimManager, CombatTagManager combatTagManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.claimManager = claimManager;
        this.combatTagManager = combatTagManager;
    }

    public void executeRtp(Player player) {
        if (combatTagManager.isTagged(player.getUniqueId())) {
            MessageUtil.sendMessage(player, "<red>Savaş esnasında RTP kullanamazsınız!</red>");
            return;
        }

        long now = System.currentTimeMillis();
        Long lastRtp = rtpCooldowns.get(player.getUniqueId());
        long cooldownMillis = configManager.getRtpCooldownSeconds() * 1000L;

        if (lastRtp != null && (now - lastRtp) < cooldownMillis) {
            long remainingSec = (cooldownMillis - (now - lastRtp)) / 1000L;
            MessageUtil.sendMessage(player, "<red>RTP için beklemeniz gerekiyor: <yellow>" + remainingSec + " saniye</yellow></red>");
            return;
        }

        World world = player.getWorld();
        if (world.getName().equalsIgnoreCase("lobby")) {
            World mainWorld = org.bukkit.Bukkit.getWorld("world");
            if (mainWorld != null) {
                world = mainWorld;
            }
        }
        if (world.getEnvironment() != World.Environment.NORMAL) {
            MessageUtil.sendMessage(player, "<red>RTP komutu yalnızca normal dünyada kullanılabilir!</red>");
            return;
        }

        MessageUtil.sendMessage(player, "<yellow>Güvenli ve sahipsiz bir bölge taranıyor, lütfen bekleyin...</yellow>");

        findSafeLocationAsync(world, 0, player, safeLoc -> {
            if (safeLoc == null) {
                MessageUtil.sendMessage(player, "<red>10 denemede güvenli bir bölge bulunamadı, lütfen tekrar deneyin.</red>");
                return;
            }

            player.teleportAsync(safeLoc).thenAccept(success -> {
                if (success) {
                    rtpCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
                    MessageUtil.sendMessage(player, String.format("<green>Işınlandınız! Koordinat: <yellow>X: %d, Y: %d, Z: %d</yellow></green>",
                            safeLoc.getBlockX(), safeLoc.getBlockY(), safeLoc.getBlockZ()));
                    MessageUtil.sendTitle(player, "<gold>RASTGELE IŞINLANMA</gold>", "<gray>Vahşi doğada dikkatli ol!</gray>", 10, 40, 10);
                } else {
                    MessageUtil.sendMessage(player, "<red>Işınlanma sırasında bir hata oluştu!</red>");
                }
            });
        });
    }

    private void findSafeLocationAsync(World world, int attempt, Player player, java.util.function.Consumer<Location> callback) {
        if (attempt >= configManager.getRtpMaxAttempts()) {
            callback.accept(null);
            return;
        }

        int minR = configManager.getRtpMinRadius();
        int maxR = configManager.getRtpMaxRadius();

        int x = (random.nextBoolean() ? 1 : -1) * (minR + random.nextInt(maxR - minR));
        int z = (random.nextBoolean() ? 1 : -1) * (minR + random.nextInt(maxR - minR));

        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        int safeRadius = configManager.getRtpClaimSafeRadiusChunks();

        // 1. Bellek içi ClaimCache kontrolü (çevresindeki 6 chunk yarıçapı)
        boolean hasClaimNear = false;
        for (int dx = -safeRadius; dx <= safeRadius; dx++) {
            for (int dz = -safeRadius; dz <= safeRadius; dz++) {
                if (claimManager.isClaimed(world, chunkX + dx, chunkZ + dz)) {
                    hasClaimNear = true;
                    break;
                }
            }
            if (hasClaimNear) break;
        }

        if (hasClaimNear) {
            // Claim varsa tekrar dene
            findSafeLocationAsync(world, attempt + 1, player, callback);
            return;
        }

        // 2. Chunk asenkron yüklenir
        world.getChunkAtAsync(chunkX, chunkZ).thenAccept(chunk -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                int surfaceY = world.getHighestBlockYAt(x, z);
                if (surfaceY <= world.getMinHeight() + 5 || surfaceY >= world.getMaxHeight() - 5) {
                    findSafeLocationAsync(world, attempt + 1, player, callback);
                    return;
                }

                Block surfaceBlock = world.getBlockAt(x, surfaceY, z);
                Biome biome = world.getBiome(x, surfaceY, z);

                // Okyanus veya tehlikeli blok kontrolü
                String biomeName = biome.getKey().value().toUpperCase();
                if (biomeName.contains("OCEAN") || biomeName.contains("RIVER") || DANGEROUS_BLOCKS.contains(surfaceBlock.getType())) {
                    findSafeLocationAsync(world, attempt + 1, player, callback);
                    return;
                }

                Block above1 = world.getBlockAt(x, surfaceY + 1, z);
                Block above2 = world.getBlockAt(x, surfaceY + 2, z);

                if (!above1.isPassable() || !above2.isPassable()) {
                    findSafeLocationAsync(world, attempt + 1, player, callback);
                    return;
                }

                // Güvenli konum bulundu
                callback.accept(new Location(world, x + 0.5, surfaceY + 1.0, z + 0.5));
            });
        });
    }
}
