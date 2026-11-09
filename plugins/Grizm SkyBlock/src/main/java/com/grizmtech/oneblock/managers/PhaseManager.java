package com.grizmtech.oneblock.managers;

import com.grizmtech.oneblock.models.PhaseLoot;
import com.grizmtech.oneblock.models.WeightedItem;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class PhaseManager {

    private final Random random = new Random();

    public enum Phase {
        ORMAN("Orman", 1, 0, 500),
        MAGARA("Mağara", 2, 500, 1200),
        COL("Çöl", 3, 1200, 2000),
        OKYANUS("Okyanus", 4, 2000, 3000),
        ORMANLIK_TEPE("Ormanlık Tepe", 5, 3000, 4200),
        NETHER("Nether", 6, 4200, 5600),
        MANTAR("Mantar Adası", 7, 5600, 7000),
        BUZUL("Buzul", 8, 7000, 8500),
        DERIN_KARANLIK("Derin Karanlık", 9, 8500, 10000),
        END("End", 10, 10000, 12000);

        private final String name;
        private final int tier;
        private final int requiredBlocks;
        private final int endBlocks;

        Phase(String name, int tier, int requiredBlocks, int endBlocks) {
            this.name = name;
            this.tier = tier;
            this.requiredBlocks = requiredBlocks;
            this.endBlocks = endBlocks;
        }

        public String getName() {
            return name;
        }

        public int getTier() {
            return tier;
        }

        public int getRequiredBlocks() {
            return requiredBlocks;
        }

        public int getEndBlocks() {
            return endBlocks;
        }
    }

    private final Map<Phase, List<WeightedItem<Material>>> phaseBlocks = new HashMap<>();
    private final Map<Phase, List<WeightedItem<EntityType>>> phaseMobs = new HashMap<>();

    public PhaseManager() {
        initBlocks();
        initMobs();
    }

    private void initBlocks() {
        // 1. ORMAN (Tier 1)
        addBlock(Phase.ORMAN, Material.DIRT, 100);
        addBlock(Phase.ORMAN, Material.GRASS_BLOCK, 80);
        addBlock(Phase.ORMAN, Material.OAK_LOG, 45);
        addBlock(Phase.ORMAN, Material.BIRCH_LOG, 35);
        addBlock(Phase.ORMAN, Material.OAK_LEAVES, 30);
        addBlock(Phase.ORMAN, Material.CLAY, 25);
        addBlock(Phase.ORMAN, Material.STONE, 30);
        addBlock(Phase.ORMAN, Material.COBBLESTONE, 30);
        addBlock(Phase.ORMAN, Material.COAL_ORE, 15);
        addBlock(Phase.ORMAN, Material.COPPER_ORE, 10);
        addBlock(Phase.ORMAN, Material.IRON_ORE, 8);

        // 2. MAGARA (Tier 2)
        addBlock(Phase.MAGARA, Material.STONE, 100);
        addBlock(Phase.MAGARA, Material.COBBLESTONE, 80);
        addBlock(Phase.MAGARA, Material.ANDESITE, 40);
        addBlock(Phase.MAGARA, Material.DIORITE, 30);
        addBlock(Phase.MAGARA, Material.GRANITE, 30);
        addBlock(Phase.MAGARA, Material.COAL_ORE, 35);
        addBlock(Phase.MAGARA, Material.IRON_ORE, 25);
        addBlock(Phase.MAGARA, Material.COPPER_ORE, 20);
        addBlock(Phase.MAGARA, Material.LAPIS_ORE, 12);
        addBlock(Phase.MAGARA, Material.GOLD_ORE, 8);
        addBlock(Phase.MAGARA, Material.DIAMOND_ORE, 3);

        // 3. COL (Tier 3)
        addBlock(Phase.COL, Material.SAND, 100);
        addBlock(Phase.COL, Material.SANDSTONE, 70);
        addBlock(Phase.COL, Material.CUT_SANDSTONE, 40);
        addBlock(Phase.COL, Material.RED_SAND, 40);
        addBlock(Phase.COL, Material.TERRACOTTA, 35);
        addBlock(Phase.COL, Material.YELLOW_TERRACOTTA, 25);
        addBlock(Phase.COL, Material.GOLD_ORE, 18);
        addBlock(Phase.COL, Material.IRON_ORE, 15);
        addBlock(Phase.COL, Material.GRAVEL, 20);

        // 4. OKYANUS (Tier 4)
        addBlock(Phase.OKYANUS, Material.PRISMARINE, 80);
        addBlock(Phase.OKYANUS, Material.PRISMARINE_BRICKS, 50);
        addBlock(Phase.OKYANUS, Material.DARK_PRISMARINE, 40);
        addBlock(Phase.OKYANUS, Material.SEA_LANTERN, 25);
        addBlock(Phase.OKYANUS, Material.SPONGE, 15);
        addBlock(Phase.OKYANUS, Material.GRAVEL, 40);
        addBlock(Phase.OKYANUS, Material.SAND, 50);
        addBlock(Phase.OKYANUS, Material.LAPIS_ORE, 20);
        addBlock(Phase.OKYANUS, Material.GOLD_ORE, 15);

        // 5. ORMANLIK TEPE (Tier 5)
        addBlock(Phase.ORMANLIK_TEPE, Material.DARK_OAK_LOG, 70);
        addBlock(Phase.ORMANLIK_TEPE, Material.MOSS_BLOCK, 60);
        addBlock(Phase.ORMANLIK_TEPE, Material.ROOTED_DIRT, 50);
        addBlock(Phase.ORMANLIK_TEPE, Material.MUD, 40);
        addBlock(Phase.ORMANLIK_TEPE, Material.IRON_ORE, 25);
        addBlock(Phase.ORMANLIK_TEPE, Material.GOLD_ORE, 18);
        addBlock(Phase.ORMANLIK_TEPE, Material.EMERALD_ORE, 12);
        addBlock(Phase.ORMANLIK_TEPE, Material.DIAMOND_ORE, 6);

        // 6. NETHER (Tier 6)
        addBlock(Phase.NETHER, Material.NETHERRACK, 100);
        addBlock(Phase.NETHER, Material.SOUL_SAND, 50);
        addBlock(Phase.NETHER, Material.SOUL_SOIL, 40);
        addBlock(Phase.NETHER, Material.BASALT, 40);
        addBlock(Phase.NETHER, Material.BLACKSTONE, 40);
        addBlock(Phase.NETHER, Material.NETHER_QUARTZ_ORE, 35);
        addBlock(Phase.NETHER, Material.NETHER_GOLD_ORE, 30);
        addBlock(Phase.NETHER, Material.GLOWSTONE, 25);
        addBlock(Phase.NETHER, Material.MAGMA_BLOCK, 20);
        addBlock(Phase.NETHER, Material.ANCIENT_DEBRIS, 4);

        // 7. MANTAR (Tier 7)
        addBlock(Phase.MANTAR, Material.MYCELIUM, 90);
        addBlock(Phase.MANTAR, Material.BROWN_MUSHROOM_BLOCK, 60);
        addBlock(Phase.MANTAR, Material.RED_MUSHROOM_BLOCK, 60);
        addBlock(Phase.MANTAR, Material.MUSHROOM_STEM, 50);
        addBlock(Phase.MANTAR, Material.GOLD_ORE, 20);
        addBlock(Phase.MANTAR, Material.DIAMOND_ORE, 10);
        addBlock(Phase.MANTAR, Material.EMERALD_ORE, 8);

        // 8. BUZUL (Tier 8)
        addBlock(Phase.BUZUL, Material.ICE, 80);
        addBlock(Phase.BUZUL, Material.PACKED_ICE, 60);
        addBlock(Phase.BUZUL, Material.BLUE_ICE, 40);
        addBlock(Phase.BUZUL, Material.SNOW_BLOCK, 70);
        addBlock(Phase.BUZUL, Material.DIAMOND_ORE, 15);
        addBlock(Phase.BUZUL, Material.LAPIS_ORE, 20);
        addBlock(Phase.BUZUL, Material.EMERALD_ORE, 12);

        // 9. DERIN KARANLIK (Tier 9)
        addBlock(Phase.DERIN_KARANLIK, Material.DEEPSLATE, 90);
        addBlock(Phase.DERIN_KARANLIK, Material.SCULK, 70);
        addBlock(Phase.DERIN_KARANLIK, Material.SCULK_CATALYST, 25);
        addBlock(Phase.DERIN_KARANLIK, Material.SCULK_SHRIEKER, 15);
        addBlock(Phase.DERIN_KARANLIK, Material.DEEPSLATE_IRON_ORE, 30);
        addBlock(Phase.DERIN_KARANLIK, Material.DEEPSLATE_GOLD_ORE, 25);
        addBlock(Phase.DERIN_KARANLIK, Material.DEEPSLATE_REDSTONE_ORE, 30);
        addBlock(Phase.DERIN_KARANLIK, Material.DEEPSLATE_DIAMOND_ORE, 20);
        addBlock(Phase.DERIN_KARANLIK, Material.ANCIENT_DEBRIS, 6);

        // 10. END (Tier 10)
        addBlock(Phase.END, Material.END_STONE, 100);
        addBlock(Phase.END, Material.END_STONE_BRICKS, 60);
        addBlock(Phase.END, Material.PURPUR_BLOCK, 50);
        addBlock(Phase.END, Material.PURPUR_PILLAR, 35);
        addBlock(Phase.END, Material.OBSIDIAN, 30);
        addBlock(Phase.END, Material.CRYING_OBSIDIAN, 20);
        addBlock(Phase.END, Material.DIAMOND_BLOCK, 8);
        addBlock(Phase.END, Material.NETHERITE_BLOCK, 2);
    }

    private void initMobs() {
        addMob(Phase.ORMAN, EntityType.COW, 40);
        addMob(Phase.ORMAN, EntityType.PIG, 40);
        addMob(Phase.ORMAN, EntityType.SHEEP, 40);
        addMob(Phase.ORMAN, EntityType.CHICKEN, 40);
        addMob(Phase.ORMAN, EntityType.WOLF, 10);

        addMob(Phase.MAGARA, EntityType.ZOMBIE, 40);
        addMob(Phase.MAGARA, EntityType.SKELETON, 35);
        addMob(Phase.MAGARA, EntityType.SPIDER, 30);
        addMob(Phase.MAGARA, EntityType.BAT, 20);
        addMob(Phase.MAGARA, EntityType.CREEPER, 10);

        addMob(Phase.COL, EntityType.RABBIT, 40);
        addMob(Phase.COL, EntityType.CAMEL, 20);
        addMob(Phase.COL, EntityType.HUSK, 30);

        addMob(Phase.OKYANUS, EntityType.DROWNED, 40);
        addMob(Phase.OKYANUS, EntityType.SQUID, 40);
        addMob(Phase.OKYANUS, EntityType.GLOW_SQUID, 30);
        addMob(Phase.OKYANUS, EntityType.TURTLE, 20);

        addMob(Phase.ORMANLIK_TEPE, EntityType.FOX, 35);
        addMob(Phase.ORMANLIK_TEPE, EntityType.GOAT, 35);
        addMob(Phase.ORMANLIK_TEPE, EntityType.PANDA, 20);

        addMob(Phase.NETHER, EntityType.PIGLIN, 40);
        addMob(Phase.NETHER, EntityType.ZOMBIFIED_PIGLIN, 40);
        addMob(Phase.NETHER, EntityType.STRIDER, 30);
        addMob(Phase.NETHER, EntityType.BLAZE, 20);
        addMob(Phase.NETHER, EntityType.WITHER_SKELETON, 15);

        addMob(Phase.MANTAR, EntityType.MOOSHROOM, 80);

        addMob(Phase.BUZUL, EntityType.POLAR_BEAR, 40);
        addMob(Phase.BUZUL, EntityType.STRAY, 40);
        addMob(Phase.BUZUL, EntityType.SNOW_GOLEM, 20);

        addMob(Phase.DERIN_KARANLIK, EntityType.CREEPER, 40);
        addMob(Phase.DERIN_KARANLIK, EntityType.ENDERMAN, 35);
        addMob(Phase.DERIN_KARANLIK, EntityType.WITCH, 25);

        addMob(Phase.END, EntityType.ENDERMAN, 70);
        addMob(Phase.END, EntityType.SHULKER, 25);
        addMob(Phase.END, EntityType.ENDERMITE, 20);
    }

    private void addBlock(Phase phase, Material mat, int weight) {
        phaseBlocks.computeIfAbsent(phase, k -> new ArrayList<>()).add(new WeightedItem<>(mat, weight));
    }

    private void addMob(Phase phase, EntityType type, int weight) {
        phaseMobs.computeIfAbsent(phase, k -> new ArrayList<>()).add(new WeightedItem<>(type, weight));
    }

    public Phase getCurrentPhase(int blocksBroken) {
        Phase[] phases = Phase.values();
        for (int i = phases.length - 1; i >= 0; i--) {
            if (blocksBroken >= phases[i].getRequiredBlocks()) {
                return phases[i];
            }
        }
        return Phase.ORMAN;
    }

    public Phase getNextPhase(int blocksBroken) {
        Phase current = getCurrentPhase(blocksBroken);
        int nextOrdinal = current.ordinal() + 1;
        if (nextOrdinal < Phase.values().length) {
            return Phase.values()[nextOrdinal];
        }
        return null;
    }

    public boolean isPhaseEnd(int blocksBroken) {
        Phase current = getCurrentPhase(blocksBroken);
        return blocksBroken + 1 >= current.getEndBlocks() && getNextPhase(blocksBroken) != null;
    }

    public Material getRandomBlock(Phase phase, double oreMultiplier) {
        // Sandık Şansı: %2
        if (random.nextInt(100) < 2) {
            return Material.CHEST;
        }

        List<WeightedItem<Material>> list = phaseBlocks.getOrDefault(phase, phaseBlocks.get(Phase.ORMAN));
        int totalWeight = 0;
        for (WeightedItem<Material> entry : list) {
            int w = entry.getWeight();
            if (isOre(entry.getItem()) && oreMultiplier > 1.0) {
                w = (int) Math.round(w * oreMultiplier);
            }
            totalWeight += w;
        }

        int roll = random.nextInt(Math.max(1, totalWeight));
        int current = 0;
        for (WeightedItem<Material> entry : list) {
            int w = entry.getWeight();
            if (isOre(entry.getItem()) && oreMultiplier > 1.0) {
                w = (int) Math.round(w * oreMultiplier);
            }
            current += w;
            if (roll < current) {
                return entry.getItem();
            }
        }

        return Material.COBBLESTONE;
    }

    public EntityType getRandomMob(Phase phase) {
        List<WeightedItem<EntityType>> list = phaseMobs.getOrDefault(phase, phaseMobs.get(Phase.ORMAN));
        int totalWeight = 0;
        for (WeightedItem<EntityType> entry : list) {
            totalWeight += entry.getWeight();
        }
        int roll = random.nextInt(Math.max(1, totalWeight));
        int current = 0;
        for (WeightedItem<EntityType> entry : list) {
            current += entry.getWeight();
            if (roll < current) {
                return entry.getItem();
            }
        }
        return EntityType.PIG;
    }

    private boolean isOre(Material mat) {
        String name = mat.name();
        return name.contains("ORE") || name.equals("ANCIENT_DEBRIS") || name.contains("RAW_");
    }

    public void fillChestLoot(Inventory inv, int blocksBroken, Phase phase) {
        inv.clear();
        PhaseLoot.ChestRarity rarity = PhaseLoot.getRarityForBlocks(blocksBroken);
        List<ItemStack> items = PhaseLoot.generateLoot(rarity, phase.getTier());

        int[] availableSlots = {10, 11, 12, 13, 14, 15, 16};
        int index = 0;
        for (ItemStack item : items) {
            if (index < availableSlots.length) {
                inv.setItem(availableSlots[index++], item);
            } else {
                inv.addItem(item);
            }
        }
    }
}
