package com.grizmtech.towny.listeners;

import com.grizmtech.towny.claim.ClaimChunk;
import com.grizmtech.towny.claim.ClaimManager;
import com.grizmtech.towny.claim.PlotFlag;
import com.grizmtech.towny.claim.PlotGroup;
import com.grizmtech.towny.core.ConfigManager;
import com.grizmtech.towny.core.MessageUtil;
import com.grizmtech.towny.town.Town;
import com.grizmtech.towny.town.TownManager;
import com.grizmtech.towny.war.SiegeWar;
import com.grizmtech.towny.war.WarManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerMoveListener implements Listener {

    private final ClaimManager claimManager;
    private final TownManager townManager;
    private final WarManager warManager;
    private final ConfigManager configManager;

    public PlayerMoveListener(ClaimManager claimManager, TownManager townManager, WarManager warManager, ConfigManager configManager) {
        this.claimManager = claimManager;
        this.townManager = townManager;
        this.warManager = warManager;
        this.configManager = configManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        int fromChunkX = from.getBlockX() >> 4;
        int fromChunkZ = from.getBlockZ() >> 4;
        int toChunkX = to.getBlockX() >> 4;
        int toChunkZ = to.getBlockZ() >> 4;

        if (fromChunkX == toChunkX && fromChunkZ == toChunkZ && from.getWorld().equals(to.getWorld())) {
            return;
        }

        Player player = event.getPlayer();
        ClaimChunk claim = claimManager.getClaim(to.getWorld(), toChunkX, toChunkZ);

        if (claim == null) {
            MessageUtil.sendActionBar(player, "<dark_green>Vahşi Doğa <gray>(Sahipsiz Arazi)</gray></dark_green>");
        } else {
            Town town = townManager.getTown(claim.getTownUuid());
            String townName = town != null ? town.getName() : "Bilinmiyor";
            boolean pvpAllowed = claim.hasFlag(PlotGroup.RESIDENT, PlotFlag.PVP);
            String pvpStatus = pvpAllowed ? "<red>Açık</red>" : "<green>Kapalı</green>";

            String barMsg = String.format("<gold>Kasaba: <yellow><b>%s</b></yellow> <gray>|</gray> PvP: %s <gray>|</gray> Parsel: %s",
                    townName, pvpStatus, claim.getPlotType().getFormattedName());
            MessageUtil.sendActionBar(player, barMsg);
        }
    }
}
