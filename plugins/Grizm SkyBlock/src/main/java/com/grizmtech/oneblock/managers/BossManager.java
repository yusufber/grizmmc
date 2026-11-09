package com.grizmtech.oneblock.managers;

import com.grizmtech.oneblock.GrizmOneBlock;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Drowned;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Husk;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Vindicator;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BossManager {

    private final GrizmOneBlock plugin;
    private final Map<UUID, LivingEntity> activeBosses = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar> activeBossBars = new ConcurrentHashMap<>();

    public BossManager(GrizmOneBlock plugin) {
        this.plugin = plugin;
    }

    public boolean isBossActive(UUID ownerUUID) {
        LivingEntity boss = activeBosses.get(ownerUUID);
        return boss != null && boss.isValid() && !boss.isDead();
    }

    public void spawnBoss(UUID ownerUUID, Location loc, PhaseManager.Phase phase) {
        if (loc.getWorld() == null) return;

        // Eski boss varsa temizle
        removeBoss(ownerUUID);

        String bossName;
        EntityType type;
        double maxHealth;
        BossBar.Color barColor;

        switch (phase) {
            case COL:
                bossName = "Çöl Firavunu";
                type = EntityType.HUSK;
                maxHealth = 120.0;
                barColor = BossBar.Color.YELLOW;
                break;
            case OKYANUS:
                bossName = "Okyanus Lordu";
                type = EntityType.DROWNED;
                maxHealth = 140.0;
                barColor = BossBar.Color.BLUE;
                break;
            case ORMANLIK_TEPE:
                bossName = "Gölge Muhafızı";
                type = EntityType.VINDICATOR;
                maxHealth = 160.0;
                barColor = BossBar.Color.GREEN;
                break;
            case NETHER:
                bossName = "Nether Hükümdarı";
                type = EntityType.WITHER_SKELETON;
                maxHealth = 200.0;
                barColor = BossBar.Color.RED;
                break;
            case MANTAR:
                bossName = "Mantar Devi";
                type = EntityType.ZOMBIE;
                maxHealth = 180.0;
                barColor = BossBar.Color.PINK;
                break;
            case BUZUL:
                bossName = "Buz Kıran Şövalye";
                type = EntityType.STRAY;
                maxHealth = 220.0;
                barColor = BossBar.Color.WHITE;
                break;
            case DERIN_KARANLIK:
                bossName = "Derin Muhafız";
                type = EntityType.IRON_GOLEM;
                maxHealth = 280.0;
                barColor = BossBar.Color.PURPLE;
                break;
            case END:
                bossName = "End Hükümdarı";
                type = EntityType.WITHER_SKELETON;
                maxHealth = 350.0;
                barColor = BossBar.Color.PURPLE;
                break;
            case MAGARA:
                bossName = "Mağara Kralı";
                type = EntityType.SKELETON;
                maxHealth = 100.0;
                barColor = BossBar.Color.WHITE;
                break;
            case ORMAN:
            default:
                bossName = "Orman Muhafızı";
                type = EntityType.ZOMBIE;
                maxHealth = 80.0;
                barColor = BossBar.Color.GREEN;
                break;
        }

        LivingEntity boss = (LivingEntity) loc.getWorld().spawnEntity(loc.clone().add(0.5, 1, 0.5), type);
        boss.setCustomNameVisible(true);
        boss.customName(Component.text("☠ " + bossName).color(NamedTextColor.RED));

        if (boss.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
            boss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth);
            boss.setHealth(maxHealth);
        }

        boss.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 1));
        boss.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0));

        // Zırh donatımı
        if (boss.getEquipment() != null) {
            boss.getEquipment().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
            boss.getEquipment().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
            boss.getEquipment().setItemInMainHand(new ItemStack(Material.DIAMOND_SWORD));
        }

        activeBosses.put(ownerUUID, boss);

        // BossBar oluştur ve adadaki oyunculara göster
        BossBar bossBar = BossBar.bossBar(
                Component.text("Aşama Boss'u: " + bossName).color(NamedTextColor.GOLD),
                1.0f,
                barColor,
                BossBar.Overlay.PROGRESS
        );

        activeBossBars.put(ownerUUID, bossBar);
        updateBossBarViewers(ownerUUID);

        // Title uyarısı
        Title title = Title.title(
                Component.text("⚠ AŞAMA BOSS'U DOĞDU! ⚠").color(NamedTextColor.DARK_RED),
                Component.text(bossName + " adanıza indi!").color(NamedTextColor.YELLOW),
                Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(3), Duration.ofSeconds(1))
        );

        for (Player p : loc.getWorld().getPlayers()) {
            p.showTitle(title);
            p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_WITHER_SPAWN, 0.8f, 1.0f);
        }
    }

    public void updateBossHealth(UUID ownerUUID, double current, double max) {
        BossBar bar = activeBossBars.get(ownerUUID);
        if (bar != null) {
            float progress = (float) Math.max(0.0, Math.min(1.0, current / max));
            bar.progress(progress);
        }
    }

    public void updateBossBarViewers(UUID ownerUUID) {
        BossBar bar = activeBossBars.get(ownerUUID);
        if (bar == null) return;

        org.bukkit.World world = org.bukkit.Bukkit.getWorld("island_" + ownerUUID.toString());
        if (world == null) return;

        for (Player p : world.getPlayers()) {
            p.showBossBar(bar);
        }
    }

    public void onBossKilled(UUID ownerUUID, Player killer) {
        removeBoss(ownerUUID);

        org.bukkit.World world = org.bukkit.Bukkit.getWorld("island_" + ownerUUID.toString());
        if (world != null) {
            Location loc = world.getSpawnLocation();

            // Havai fişek patlat
            try {
                Firework fw = (Firework) world.spawnEntity(loc.clone().add(0, 2, 0), EntityType.FIREWORK_ROCKET);
                FireworkMeta fwm = fw.getFireworkMeta();
                fwm.addEffect(FireworkEffect.builder().withColor(Color.ORANGE, Color.YELLOW).with(FireworkEffect.Type.BALL_LARGE).build());
                fwm.setPower(1);
                fw.setFireworkMeta(fwm);
            } catch (Exception ignored) {}

            Title title = Title.title(
                    Component.text("★ BOSS MAĞLUP EDİLDİ! ★").color(NamedTextColor.GREEN),
                    Component.text("Tebrikler! Sonraki aşamaya geçiş açıldı.").color(NamedTextColor.GOLD),
                    Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(4), Duration.ofSeconds(1))
            );

            for (Player p : world.getPlayers()) {
                p.showTitle(title);
                p.playSound(p.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f);
            }

            // Ödül: Ada Bankasına veya oyuncuya 10,000 para
            if (killer != null && plugin.getEconomyManager().isAvailable()) {
                plugin.getEconomyManager().giveMoney(killer, 10000.0);
                killer.sendMessage(Component.text("Boss ödülü olarak 10.000 Para kazandınız!").color(NamedTextColor.GOLD));
            }
        }
    }

    public void removeBoss(UUID ownerUUID) {
        LivingEntity entity = activeBosses.remove(ownerUUID);
        if (entity != null && entity.isValid()) {
            entity.remove();
        }

        BossBar bar = activeBossBars.remove(ownerUUID);
        if (bar != null) {
            org.bukkit.World world = org.bukkit.Bukkit.getWorld("island_" + ownerUUID.toString());
            if (world != null) {
                for (Player p : world.getPlayers()) {
                    p.hideBossBar(bar);
                }
            }
        }
    }
}
