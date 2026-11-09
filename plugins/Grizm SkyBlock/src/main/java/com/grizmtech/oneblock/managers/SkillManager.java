package com.grizmtech.oneblock.managers;

import com.grizmtech.oneblock.GrizmOneBlock;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.UUID;

public class SkillManager {

    private final GrizmOneBlock plugin;

    public SkillManager(GrizmOneBlock plugin) {
        this.plugin = plugin;
    }

    public int getSkillPoints(UUID uuid) {
        return plugin.getDataManager().getConfig().getInt("players." + uuid + ".skills.points", 0);
    }

    public void setSkillPoints(UUID uuid, int points) {
        plugin.getDataManager().getConfig().set("players." + uuid + ".skills.points", Math.max(0, points));
        plugin.getDataManager().saveConfig();
    }

    public void addSkillPoints(UUID uuid, int amount) {
        setSkillPoints(uuid, getSkillPoints(uuid) + amount);
    }

    public int getSkillLevel(UUID uuid, String skillId) {
        return plugin.getDataManager().getConfig().getInt("players." + uuid + ".skills." + skillId, 0);
    }

    public void setSkillLevel(UUID uuid, String skillId, int level) {
        plugin.getDataManager().getConfig().set("players." + uuid + ".skills." + skillId, level);
        plugin.getDataManager().saveConfig();
    }

    public boolean hasSkill(UUID uuid, String skillId) {
        return getSkillLevel(uuid, skillId) > 0;
    }

    public boolean upgradeSkill(Player player, String skillId, int cost, int maxLevel) {
        UUID uuid = player.getUniqueId();
        int points = getSkillPoints(uuid);
        int current = getSkillLevel(uuid, skillId);

        if (current >= maxLevel) {
            player.sendMessage(Component.text("Bu yetenek zaten maksimum seviyede!").color(NamedTextColor.RED));
            return false;
        }

        if (points < cost) {
            player.sendMessage(Component.text("Yetersiz Yetenek Puanı! Gerekli: " + cost + " Puan (Mevcut: " + points + ")").color(NamedTextColor.RED));
            return false;
        }

        setSkillPoints(uuid, points - cost);
        setSkillLevel(uuid, skillId, current + 1);

        player.sendMessage(Component.text("Yetenek başarıyla yükseltildi: Seviye " + (current + 1)).color(NamedTextColor.GREEN));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.6f);

        // Uçuş yetkisi verildiyse aktif et
        if (skillId.equals("fly") && player.getWorld().getName().startsWith("island_")) {
            player.setAllowFlight(true);
        }

        return true;
    }
}
