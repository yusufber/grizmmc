package com.grizmtech.towny.listeners;

import com.grizmtech.towny.claim.ClaimChunk;
import com.grizmtech.towny.claim.ClaimManager;
import com.grizmtech.towny.claim.PlotFlag;
import com.grizmtech.towny.combat.CombatTagManager;
import com.grizmtech.towny.core.ConfigManager;
import com.grizmtech.towny.core.MessageUtil;
import com.grizmtech.towny.town.Town;
import com.grizmtech.towny.town.TownManager;
import com.grizmtech.towny.war.BlockRollbackManager;
import com.grizmtech.towny.war.SiegeWar;
import com.grizmtech.towny.war.WarManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Set;

public class ClaimProtectionListener implements Listener {

    private final ClaimManager claimManager;
    private final TownManager townManager;
    private final WarManager warManager;
    private final CombatTagManager combatTagManager;
    private final BlockRollbackManager rollbackManager;
    private final ConfigManager configManager;

    private static final Set<Material> CONTAINER_TYPES = Set.of(
            Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL,
            Material.SHULKER_BOX, Material.FURNACE, Material.BLAST_FURNACE,
            Material.SMOKER, Material.HOPPER, Material.DROPPER, Material.DISPENSER
    );

    public ClaimProtectionListener(ClaimManager claimManager, TownManager townManager,
                                   WarManager warManager, CombatTagManager combatTagManager,
                                   BlockRollbackManager rollbackManager, ConfigManager configManager) {
        this.claimManager = claimManager;
        this.townManager = townManager;
        this.warManager = warManager;
        this.combatTagManager = combatTagManager;
        this.rollbackManager = rollbackManager;
        this.configManager = configManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (!claimManager.canPerform(player, block.getLocation(), PlotFlag.BLOCK_BREAK)) {
            event.setCancelled(true);
            MessageUtil.sendMessage(player, "<red>Bu arazide blok kırma yetkiniz yok!</red>");
            return;
        }

        // Aktif kuşatma alanındaysa rollback için kaydet
        ClaimChunk claim = claimManager.getClaim(block.getLocation());
        if (claim != null && warManager.isSiegeActiveInChunk(claim.getWorldUuid(), claim.getChunkX(), claim.getChunkZ())) {
            SiegeWar war = warManager.getWarByTown(claim.getTownUuid());
            if (war != null) {
                rollbackManager.recordBlock(war.getWarId(), block.getLocation());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (!claimManager.canPerform(player, block.getLocation(), PlotFlag.BLOCK_PLACE)) {
            event.setCancelled(true);
            MessageUtil.sendMessage(player, "<red>Bu arazide blok koyma yetkiniz yok!</red>");
            return;
        }

        // Aktif kuşatma alanındaysa koyulan bloğu geri kaldırmak için hava olarak kaydet
        ClaimChunk claim = claimManager.getClaim(block.getLocation());
        if (claim != null && warManager.isSiegeActiveInChunk(claim.getWorldUuid(), claim.getChunkX(), claim.getChunkZ())) {
            SiegeWar war = warManager.getWarByTown(claim.getTownUuid());
            if (war != null) {
                rollbackManager.recordBlock(war.getWarId(), block.getLocation());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        Player player = event.getPlayer();
        Material type = block.getType();

        // Sandık / Konteyner Kontrolü
        if (CONTAINER_TYPES.contains(type) || type.name().endsWith("SHULKER_BOX")) {
            if (!claimManager.canPerform(player, block.getLocation(), PlotFlag.CHEST_ACCESS)) {
                event.setCancelled(true);
                MessageUtil.sendMessage(player, "<red>Bu sandığı açma yetkiniz yok!</red>");
                return;
            }
        }

        // Kapı, Tuzak Kapısı, Buton, Şalter Kontrolü
        if (Tag.DOORS.isTagged(type) || Tag.TRAPDOORS.isTagged(type) ||
                Tag.BUTTONS.isTagged(type) || type == Material.LEVER) {
            if (!claimManager.canPerform(player, block.getLocation(), PlotFlag.DOOR_USE)) {
                event.setCancelled(true);
                MessageUtil.sendMessage(player, "<red>Bu mekanizmayı kullanma yetkiniz yok!</red>");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPvPDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        Player attacker = null;
        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            attacker = p;
        }

        if (attacker == null || attacker.equals(victim)) return;

        Location loc = victim.getLocation();
        if (!claimManager.canPerform(attacker, loc, PlotFlag.PVP)) {
            event.setCancelled(true);
            MessageUtil.sendMessage(attacker, "<red>Bu arazide PvP kapalıdır!</red>");
            return;
        }

        // PvP izni varsa her iki oyuncuyu Combat-Tag listesine al
        combatTagManager.tagPlayer(attacker, configManager.getCombatTagSeconds());
        combatTagManager.tagPlayer(victim, configManager.getCombatTagSeconds());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        handleExplosion(event.getLocation(), event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        handleExplosion(event.getBlock().getLocation(), event.blockList());
    }

    private void handleExplosion(Location center, java.util.List<Block> blocks) {
        ClaimChunk claim = claimManager.getClaim(center);
        if (claim == null) return; // Vahşi doğada patlamalar serbest

        Town town = townManager.getTown(claim.getTownUuid());
        if (town == null) return;

        // Harabe modundaysa patlama serbest
        if (town.isRuined(configManager.getRuinDurationHours() * 3600000L)) return;

        // Aktif kuşatma varsa patlamaya izin ver ve blokları geri alma kuyruğuna yaz
        if (warManager.isSiegeActiveInChunk(claim.getWorldUuid(), claim.getChunkX(), claim.getChunkZ())) {
            SiegeWar war = warManager.getWarByTown(claim.getTownUuid());
            if (war != null) {
                for (Block b : blocks) {
                    rollbackManager.recordBlock(war.getWarId(), b.getLocation());
                }
            }
            return;
        }

        // Normal durumda patlama bayrağı kapalıysa hasarı iptal et
        if (!claim.hasFlag(com.grizmtech.towny.claim.PlotGroup.RESIDENT, PlotFlag.EXPLOSION)) {
            blocks.clear();
        }
    }
}
