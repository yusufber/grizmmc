package com.grizmtech.towny.visual;

import com.grizmtech.towny.claim.ClaimChunk;
import com.grizmtech.towny.claim.ClaimManager;
import com.grizmtech.towny.town.Town;
import com.grizmtech.towny.town.TownManager;
import com.grizmtech.towny.war.SiegeWar;
import com.grizmtech.towny.war.WarManager;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BorderParticleTask {

    private final JavaPlugin plugin;
    private final ClaimManager claimManager;
    private final TownManager townManager;
    private final WarManager warManager;

    private final Set<UUID> enabledPlayers = ConcurrentHashMap.newKeySet();
    private BukkitTask task;

    public BorderParticleTask(JavaPlugin plugin, ClaimManager claimManager, TownManager townManager, WarManager warManager) {
        this.plugin = plugin;
        this.claimManager = claimManager;
        this.townManager = townManager;
        this.warManager = warManager;

        start();
    }

    public boolean toggle(Player player) {
        UUID uuid = player.getUniqueId();
        if (enabledPlayers.contains(uuid)) {
            enabledPlayers.remove(uuid);
            return false;
        } else {
            enabledPlayers.add(uuid);
            return true;
        }
    }

    private void start() {
        this.task = new BukkitRunnable() {
            @Override
            public void run() {
                if (enabledPlayers.isEmpty()) return;

                for (UUID uuid : enabledPlayers) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null || !player.isOnline()) {
                        enabledPlayers.remove(uuid);
                        continue;
                    }

                    renderBorderForPlayer(player);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Her saniye
    }

    private void renderBorderForPlayer(Player player) {
        Location loc = player.getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        int chunkX = loc.getBlockX() >> 4;
        int chunkZ = loc.getBlockZ() >> 4;
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        int maxX = minX + 16;
        int maxZ = minZ + 16;

        double playerY = loc.getY();

        ClaimChunk claim = claimManager.getClaim(world, chunkX, chunkZ);
        Town currentTown = claim != null ? townManager.getTown(claim.getTownUuid()) : null;
        Town playerTown = townManager.getTownByPlayer(player.getUniqueId());

        Particle particle;
        Particle.DustOptions redDust = new Particle.DustOptions(Color.RED, 1.2f);
        boolean isRedDust = false;

        if (currentTown == null) {
            particle = Particle.SMOKE;
        } else if (playerTown != null && playerTown.getUuid().equals(currentTown.getUuid())) {
            particle = Particle.HAPPY_VILLAGER; // Yeşil
        } else {
            // Savaşta düşman mı?
            SiegeWar war = warManager.getWarByTown(currentTown.getUuid());
            if (war != null && playerTown != null && (war.getAttackerTownUuid().equals(playerTown.getUuid()) || war.getDefenderTownUuid().equals(playerTown.getUuid()))) {
                particle = Particle.DUST; // Kırmızı
                isRedDust = true;
            } else {
                particle = Particle.FLAME; // Sarı / Tarafsız
            }
        }

        // 16x16 çevresini çiz
        for (int step = 0; step < 16; step += 2) {
            spawnAt(player, particle, isRedDust ? redDust : null, world, minX + step, playerY + 0.8, minZ);
            spawnAt(player, particle, isRedDust ? redDust : null, world, maxX, playerY + 0.8, minZ + step);
            spawnAt(player, particle, isRedDust ? redDust : null, world, maxX - step, playerY + 0.8, maxZ);
            spawnAt(player, particle, isRedDust ? redDust : null, world, minX, playerY + 0.8, maxZ - step);
        }
    }

    private void spawnAt(Player player, Particle particle, Particle.DustOptions dust, World world, double x, double y, double z) {
        if (dust != null) {
            player.spawnParticle(particle, x, y, z, 1, 0, 0, 0, dust);
        } else {
            player.spawnParticle(particle, x, y, z, 1, 0, 0, 0, 0);
        }
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
        }
        enabledPlayers.clear();
    }
}
