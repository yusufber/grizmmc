package com.grizmtech.oneblock.models;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PhaseLoot {

    public enum ChestRarity {
        COMMON("Standart Sandık", NamedTextColor.WHITE, 1),
        RARE("Nadir Sandık", NamedTextColor.AQUA, 2),
        EPIC("Destansı Sandık", NamedTextColor.DARK_PURPLE, 3),
        LEGENDARY("Efsanevi Sandık", NamedTextColor.GOLD, 4);

        private final String displayName;
        private final NamedTextColor color;
        private final int tier;

        ChestRarity(String displayName, NamedTextColor color, int tier) {
            this.displayName = displayName;
            this.color = color;
            this.tier = tier;
        }

        public String getDisplayName() {
            return displayName;
        }

        public NamedTextColor getColor() {
            return color;
        }

        public int getTier() {
            return tier;
        }
    }

    private static final Random random = new Random();

    public static ChestRarity getRarityForBlocks(int blocks) {
        if (blocks % 500 == 0) return ChestRarity.LEGENDARY;
        if (blocks % 250 == 0) return ChestRarity.EPIC;
        if (blocks % 100 == 0) return ChestRarity.RARE;
        return ChestRarity.COMMON;
    }

    public static List<ItemStack> generateLoot(ChestRarity rarity, int phaseIndex) {
        List<ItemStack> loot = new ArrayList<>();

        switch (rarity) {
            case LEGENDARY:
                loot.add(new ItemStack(Material.NETHERITE_INGOT, 1 + random.nextInt(2)));
                loot.add(new ItemStack(Material.DIAMOND_BLOCK, 1 + random.nextInt(2)));
                loot.add(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1 + random.nextInt(2)));
                loot.add(new ItemStack(Material.TOTEM_OF_UNDYING, 1));
                loot.add(createEnchantedBook(Enchantment.EFFICIENCY, 5));
                loot.add(new ItemStack(Material.EXPERIENCE_BOTTLE, 16 + random.nextInt(16)));
                break;

            case EPIC:
                loot.add(new ItemStack(Material.DIAMOND, 3 + random.nextInt(5)));
                loot.add(new ItemStack(Material.EMERALD_BLOCK, 1 + random.nextInt(2)));
                loot.add(new ItemStack(Material.GOLDEN_APPLE, 2 + random.nextInt(4)));
                loot.add(new ItemStack(Material.ANCIENT_DEBRIS, 1 + random.nextInt(2)));
                loot.add(createEnchantedBook(Enchantment.FORTUNE, 3));
                loot.add(new ItemStack(Material.EXPERIENCE_BOTTLE, 8 + random.nextInt(12)));
                break;

            case RARE:
                loot.add(new ItemStack(Material.DIAMOND, 1 + random.nextInt(3)));
                loot.add(new ItemStack(Material.IRON_BLOCK, 1 + random.nextInt(3)));
                loot.add(new ItemStack(Material.GOLD_INGOT, 4 + random.nextInt(6)));
                loot.add(new ItemStack(Material.ENDER_PEARL, 2 + random.nextInt(4)));
                loot.add(new ItemStack(Material.GOLDEN_CARROT, 8 + random.nextInt(8)));
                loot.add(new ItemStack(Material.EXPERIENCE_BOTTLE, 5 + random.nextInt(5)));
                break;

            case COMMON:
            default:
                loot.add(new ItemStack(Material.IRON_INGOT, 2 + random.nextInt(5)));
                loot.add(new ItemStack(Material.COAL, 4 + random.nextInt(8)));
                loot.add(new ItemStack(Material.BREAD, 4 + random.nextInt(6)));
                loot.add(new ItemStack(Material.OAK_SAPLING, 2 + random.nextInt(2)));
                break;
        }

        return loot;
    }

    private static ItemStack createEnchantedBook(Enchantment ench, int level) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        org.bukkit.inventory.meta.EnchantmentStorageMeta meta = (org.bukkit.inventory.meta.EnchantmentStorageMeta) book.getItemMeta();
        meta.addStoredEnchant(ench, level, true);
        book.setItemMeta(meta);
        return book;
    }
}
