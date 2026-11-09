package com.grizmtech.towny.listeners;

import com.grizmtech.towny.combat.CombatTagManager;
import com.grizmtech.towny.core.ConfigManager;
import com.grizmtech.towny.core.MessageUtil;
import com.grizmtech.towny.war.WarManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class CombatListener implements Listener {

    private final CombatTagManager combatTagManager;
    private final WarManager warManager;
    private final ConfigManager configManager;

    public CombatListener(CombatTagManager combatTagManager, WarManager warManager, ConfigManager configManager) {
        this.combatTagManager = combatTagManager;
        this.warManager = warManager;
        this.configManager = configManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (combatTagManager.isTagged(player.getUniqueId())) {
            // Savaşta oyundan kaçma cezası: anında öldür
            player.setHealth(0.0);
            combatTagManager.removeTag(player);

            MessageUtil.broadcast(String.format("<red>☠ <b>%s</b> savaş esnasında oyundan kaçtığı için katledildi!</red>", player.getName()));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!combatTagManager.isTagged(player.getUniqueId())) return;

        if (player.hasPermission("grizmtowny.bypass")) return;

        String rawCommand = event.getMessage().substring(1).toLowerCase();
        String commandRoot = rawCommand.split(" ")[0];

        for (String blocked : configManager.getCombatBlockedCommands()) {
            if (rawCommand.startsWith(blocked.toLowerCase()) || commandRoot.equals(blocked.toLowerCase())) {
                event.setCancelled(true);
                int remaining = combatTagManager.getRemainingSeconds(player.getUniqueId());
                MessageUtil.sendMessage(player, "<red>Savaş modundayken bu komutu kullanamazsınız! Kalan süre: <yellow>" + remaining + "s</yellow></red>");
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer != null) {
            warManager.handleKillInSiege(killer, victim);
        }

        // Oyuncu öldüyse savaştan çıkar
        combatTagManager.removeTag(victim);
    }
}
