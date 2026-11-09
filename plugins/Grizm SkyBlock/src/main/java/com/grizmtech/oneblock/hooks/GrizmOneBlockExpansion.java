package com.grizmtech.oneblock.hooks;

import com.grizmtech.oneblock.GrizmOneBlock;
import com.grizmtech.oneblock.managers.PhaseManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class GrizmOneBlockExpansion extends PlaceholderExpansion {

    private final GrizmOneBlock plugin;

    public GrizmOneBlockExpansion(GrizmOneBlock plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "oneblock";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Yusuf Berk Demirel";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";
        UUID uuid = player.getUniqueId();

        int blocks = plugin.getDataManager().getBlocksBroken(uuid);
        PhaseManager.Phase phase = plugin.getPhaseManager().getCurrentPhase(blocks);

        switch (params.toLowerCase()) {
            case "phase":
                return phase.getName();
            case "phase_tier":
                return String.valueOf(phase.getTier());
            case "blocks_mined":
                return String.valueOf(blocks);
            case "next_blocks":
                return String.valueOf(Math.max(0, phase.getEndBlocks() - blocks));
            case "progress_bar":
                int cur = Math.max(0, blocks - phase.getRequiredBlocks());
                int max = Math.max(1, phase.getEndBlocks() - phase.getRequiredBlocks());
                return (int) (((float) cur / max) * 100) + "%";
            case "island_level":
                return String.valueOf(plugin.getIslandLevelManager().getIslandLevel(uuid));
            case "prestige":
                return String.valueOf(plugin.getRebirthManager().getPrestige(uuid));
            case "bank_balance":
                return String.valueOf((long) plugin.getBankManager().getBalance(uuid));
            case "likes":
                return String.valueOf(plugin.getIslandSocialManager().getLikes(uuid));
            case "skill_points":
                return String.valueOf(plugin.getSkillManager().getSkillPoints(uuid));
        }

        return null;
    }
}
