package com.grizmtech.oneblock.managers;

import com.grizmtech.oneblock.GrizmOneBlock;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;

import java.util.UUID;

public class UpgradeManager {

    private final GrizmOneBlock plugin;

    public UpgradeManager(GrizmOneBlock plugin) {
        this.plugin = plugin;
    }

    // --- 1. Dünya Sınırı (Border) Yükseltmeleri ---
    public int getBorderSize(int level) {
        switch (level) {
            case 1: return 50;
            case 2: return 100;
            case 3: return 150;
            case 4: return 200;
            case 5: return 250;
            default: return 50;
        }
    }

    public double getBorderUpgradeCost(int currentLevel) {
        switch (currentLevel) {
            case 1: return 50000;
            case 2: return 100000;
            case 3: return 200000;
            case 4: return 400000;
            default: return -1;
        }
    }

    public void applyBorder(UUID uuid, World world) {
        if (world == null) return;
        int level = plugin.getDataManager().getBorderLevel(uuid);
        int size = getBorderSize(level);
        WorldBorder border = world.getWorldBorder();
        border.setCenter(0.5, 0.5);
        border.setSize(size);
    }

    // --- 2. Takım Kapasitesi (Team Limit) ---
    public int getTeamLimit(int level) {
        switch (level) {
            case 1: return 3;
            case 2: return 4;
            case 3: return 5;
            case 4: return 6;
            case 5: return 8;
            default: return 3;
        }
    }

    public double getTeamUpgradeCost(int currentLevel) {
        switch (currentLevel) {
            case 1: return 40000;
            case 2: return 80000;
            case 3: return 150000;
            case 4: return 300000;
            default: return -1;
        }
    }

    // --- 3. Ana Blok Zenginliği (Generator Booster) ---
    public int getGeneratorBooster(UUID uuid) {
        return plugin.getDataManager().getConfig().getInt("players." + uuid.toString() + ".generator_booster_level", 0);
    }

    public void setGeneratorBooster(UUID uuid, int level) {
        plugin.getDataManager().getConfig().set("players." + uuid.toString() + ".generator_booster_level", level);
        plugin.getDataManager().saveConfig();
    }

    public double getGeneratorBoosterCost(int currentLevel) {
        switch (currentLevel) {
            case 0: return 50000;
            case 1: return 100000;
            case 2: return 200000;
            case 3: return 350000;
            case 4: return 500000;
            default: return -1;
        }
    }

    // --- 4. Ekin ve Spawner Hızı ---
    public int getSpeedBooster(UUID uuid) {
        return plugin.getDataManager().getConfig().getInt("players." + uuid.toString() + ".speed_booster_level", 0);
    }

    public void setSpeedBooster(UUID uuid, int level) {
        plugin.getDataManager().getConfig().set("players." + uuid.toString() + ".speed_booster_level", level);
        plugin.getDataManager().saveConfig();
    }

    public double getSpeedBoosterCost(int currentLevel) {
        switch (currentLevel) {
            case 0: return 60000;
            case 1: return 120000;
            case 2: return 240000;
            case 3: return 400000;
            case 4: return 600000;
            default: return -1;
        }
    }

    // --- 5. Limit Yükseltmeleri (Hopper, Piston, Spawner, Villager) ---
    public int getHopperLimit(int level) {
        switch (level) {
            case 1: return 10;
            case 2: return 25;
            case 3: return 50;
            case 4: return 100;
            case 5: return 200;
            default: return 10;
        }
    }

    public double getHopperUpgradeCost(int currentLevel) {
        switch (currentLevel) {
            case 1: return 50000;
            case 2: return 100000;
            case 3: return 250000;
            case 4: return 500000;
            default: return -1;
        }
    }

    public int getPistonLimit(int level) {
        switch (level) {
            case 1: return 10;
            case 2: return 25;
            case 3: return 50;
            case 4: return 100;
            default: return 10;
        }
    }

    public double getPistonUpgradeCost(int currentLevel) {
        switch (currentLevel) {
            case 1: return 30000;
            case 2: return 60000;
            case 3: return 120000;
            default: return -1;
        }
    }

    public int getSpawnerLimit(int level) {
        switch (level) {
            case 1: return 5;
            case 2: return 10;
            case 3: return 20;
            case 4: return 35;
            default: return 5;
        }
    }

    public double getSpawnerUpgradeCost(int currentLevel) {
        switch (currentLevel) {
            case 1: return 100000;
            case 2: return 250000;
            case 3: return 500000;
            default: return -1;
        }
    }

    public int getVillagerLimit(int level) {
        switch (level) {
            case 1: return 5;
            case 2: return 10;
            case 3: return 20;
            case 4: return 30;
            default: return 5;
        }
    }

    public double getVillagerUpgradeCost(int currentLevel) {
        switch (currentLevel) {
            case 1: return 75000;
            case 2: return 150000;
            case 3: return 300000;
            default: return -1;
        }
    }

    // --- 6. Su & Lav Maden Jeneratörü Yükseltmeleri ---
    public int getMaxGeneratorLevel() {
        return 5;
    }

    public double getGeneratorUpgradeCost(int currentLevel) {
        switch (currentLevel) {
            case 1: return 30000;
            case 2: return 75000;
            case 3: return 150000;
            case 4: return 300000;
            default: return -1;
        }
    }

    public String getGeneratorDescription(int level) {
        switch (level) {
            case 1: return "Kömür (%6), Bakır (%2), Demir (%2)";
            case 2: return "Demir (%7), Lapis (%4), Altın (%2) + Temel";
            case 3: return "Altın (%6), Kızıltaş (%6), Elmas (%2) + Gelişmiş";
            case 4: return "Elmas (%5), Zümrüt (%2), Yüksek Cevher Oranları";
            case 5: return "Maksimum: Antik Kalıntı (%2), Elmas (%9), Zümrüt (%4)";
            default: return "Temel Jeneratör";
        }
    }

    public Material getRandomGeneratorBlock(int level) {
        int roll = java.util.concurrent.ThreadLocalRandom.current().nextInt(1000);
        switch (level) {
            case 1:
                if (roll < 750) return Material.COBBLESTONE;
                if (roll < 900) return Material.STONE;
                if (roll < 960) return Material.COAL_ORE;
                if (roll < 980) return Material.COPPER_ORE;
                return Material.IRON_ORE;

            case 2:
                if (roll < 580) return Material.COBBLESTONE;
                if (roll < 730) return Material.STONE;
                if (roll < 830) return Material.COAL_ORE;
                if (roll < 870) return Material.COPPER_ORE;
                if (roll < 940) return Material.IRON_ORE;
                if (roll < 980) return Material.LAPIS_ORE;
                return Material.GOLD_ORE;

            case 3:
                if (roll < 450) return Material.COBBLESTONE;
                if (roll < 600) return Material.STONE;
                if (roll < 700) return Material.COAL_ORE;
                if (roll < 770) return Material.IRON_ORE;
                if (roll < 840) return Material.GOLD_ORE;
                if (roll < 910) return Material.REDSTONE_ORE;
                if (roll < 970) return Material.LAPIS_ORE;
                return Material.DIAMOND_ORE;

            case 4:
                if (roll < 350) return Material.COBBLESTONE;
                if (roll < 500) return Material.STONE;
                if (roll < 600) return Material.IRON_ORE;
                if (roll < 700) return Material.GOLD_ORE;
                if (roll < 800) return Material.REDSTONE_ORE;
                if (roll < 880) return Material.LAPIS_ORE;
                if (roll < 950) return Material.DIAMOND_ORE;
                return Material.EMERALD_ORE;

            case 5:
            default:
                if (roll < 250) return Material.COBBLESTONE;
                if (roll < 400) return Material.STONE;
                if (roll < 500) return Material.IRON_ORE;
                if (roll < 620) return Material.GOLD_ORE;
                if (roll < 720) return Material.REDSTONE_ORE;
                if (roll < 810) return Material.LAPIS_ORE;
                if (roll < 910) return Material.DIAMOND_ORE;
                if (roll < 970) return Material.EMERALD_ORE;
                return Material.ANCIENT_DEBRIS;
        }
    }
}
