package com.grizmtech.towny.hooks;

import com.grizmtech.towny.claim.ClaimManager;
import com.grizmtech.towny.combat.CombatTagManager;
import com.grizmtech.towny.town.Town;
import com.grizmtech.towny.town.TownManager;
import com.grizmtech.towny.town.TownMember;
import com.grizmtech.towny.war.SiegeWar;
import com.grizmtech.towny.war.WarManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TownyPlaceholderExpansion extends PlaceholderExpansion {

    private final JavaPlugin plugin;
    private final TownManager townManager;
    private final ClaimManager claimManager;
    private final WarManager warManager;
    private final CombatTagManager combatTagManager;

    public TownyPlaceholderExpansion(JavaPlugin plugin, TownManager townManager, ClaimManager claimManager,
                                     WarManager warManager, CombatTagManager combatTagManager) {
        this.plugin = plugin;
        this.townManager = townManager;
        this.claimManager = claimManager;
        this.warManager = warManager;
        this.combatTagManager = combatTagManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "grizmtowny";
    }

    @Override
    public @NotNull String getAuthor() {
        return "GrizmTech";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";

        Town town = townManager.getTownByPlayer(player.getUniqueId());
        TownMember member = townManager.getMember(player.getUniqueId());

        return switch (params.toLowerCase()) {
            case "town" -> town != null ? town.getName() : "Yok";
            case "role" -> member != null ? member.getRole().getDisplayName() : "Vatansız";
            case "bank" -> town != null ? String.format("%.2f", town.getBankBalance()) : "0.00";
            case "claims" -> town != null ? String.valueOf(claimManager.getTownClaimCount(town.getUuid())) : "0";
            case "members" -> town != null ? String.valueOf(townManager.getTownMembers(town.getUuid()).size()) : "0";
            case "level" -> town != null ? String.valueOf(town.getLevel()) : "0";
            case "combat_tag" -> String.valueOf(combatTagManager.getRemainingSeconds(player.getUniqueId()));
            case "war_status" -> {
                if (town == null) yield "Savaş Yok";
                SiegeWar war = warManager.getWarByTown(town.getUuid());
                yield war != null ? war.getState().getDisplayName() : "Barış";
            }
            default -> null;
        };
    }
}
