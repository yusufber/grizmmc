package com.grizmtech.oneblock.managers;

import com.grizmtech.oneblock.GrizmOneBlock;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;

import java.time.Duration;
import java.util.UUID;

public class RebirthManager {

    private final GrizmOneBlock plugin;
    public static final int REQUIRED_BLOCKS_FOR_PRESTIGE = 12000;

    public RebirthManager(GrizmOneBlock plugin) {
        this.plugin = plugin;
    }

    public int getPrestige(UUID uuid) {
        return plugin.getDataManager().getConfig().getInt("players." + uuid.toString() + ".prestige_level", 0);
    }

    public void setPrestige(UUID uuid, int level) {
        plugin.getDataManager().getConfig().set("players." + uuid.toString() + ".prestige_level", level);
        plugin.getDataManager().saveConfig();
    }

    public boolean canPrestige(UUID uuid) {
        int broken = plugin.getDataManager().getBlocksBroken(uuid);
        return broken >= REQUIRED_BLOCKS_FOR_PRESTIGE;
    }

    public double getOreMultiplier(UUID uuid) {
        int prestige = getPrestige(uuid);
        int genBoosterLevel = plugin.getDataManager().getConfig().getInt("players." + uuid.toString() + ".generator_booster_level", 0);
        return 1.0 + (prestige * 0.10) + (genBoosterLevel * 0.05);
    }

    public String getPrestigeBadge(UUID uuid) {
        int prestige = getPrestige(uuid);
        if (prestige > 0) {
            return "§6[★" + prestige + "] ";
        }
        return "";
    }

    public boolean doPrestige(Player player) {
        UUID uuid = player.getUniqueId();
        if (!canPrestige(uuid)) {
            player.sendMessage(Component.text("Prestij yapabilmek için tüm aşamaları bitirmelisin! (" + REQUIRED_BLOCKS_FOR_PRESTIGE + " Blok)").color(NamedTextColor.RED));
            return false;
        }

        int current = getPrestige(uuid);
        int next = current + 1;
        setPrestige(uuid, next);

        // İlerlemeyi sıfırla
        plugin.getDataManager().setBlocksBroken(uuid, 0);
        plugin.getDataManager().saveConfig();

        // Ödül: 250.000 Para
        if (plugin.getEconomyManager().isAvailable()) {
            plugin.getEconomyManager().giveMoney(player, 250000.0);
        }

        // Efekt ve Duyuru
        Title title = Title.title(
                Component.text("★ PRESTİJ KAZANDIN! ★").color(NamedTextColor.GOLD),
                Component.text("Prestij Seviyesi: " + next + " | Kalıcı +%" + (next * 10) + " Maden Şansı").color(NamedTextColor.YELLOW),
                Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(4), Duration.ofSeconds(1))
        );
        player.showTitle(title);

        try {
            Firework fw = (Firework) player.getWorld().spawnEntity(player.getLocation().add(0, 1, 0), EntityType.FIREWORK_ROCKET);
            FireworkMeta fwm = fw.getFireworkMeta();
            fwm.addEffect(FireworkEffect.builder().withColor(Color.ORANGE, Color.RED, Color.YELLOW).with(FireworkEffect.Type.BALL_LARGE).build());
            fwm.setPower(1);
            fw.setFireworkMeta(fwm);
        } catch (Exception ignored) {}

        Bukkit.broadcast(Component.text("★ ").color(NamedTextColor.GOLD)
                .append(Component.text(player.getName()).color(NamedTextColor.YELLOW))
                .append(Component.text(" adlı oyuncu ").color(NamedTextColor.WHITE))
                .append(Component.text("PRESTİJ " + next).color(NamedTextColor.GOLD))
                .append(Component.text(" seviyesine ulaştı!").color(NamedTextColor.WHITE)));

        return true;
    }
}
