package com.grizmtech.oneblock.managers;

import com.grizmtech.oneblock.GrizmOneBlock;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BiomeManager {

    private final GrizmOneBlock plugin;

    public enum IslandBiome {
        PLAINS("Ovalar (Plains)", Biome.PLAINS, Material.GRASS_BLOCK, 25000),
        FOREST("Orman (Forest)", Biome.FOREST, Material.OAK_SAPLING, 30000),
        TAIGA("Tayga (Taiga - Kar/Buz)", Biome.TAIGA, Material.SNOWBALL, 40000),
        DESERT("Çöl (Desert)", Biome.DESERT, Material.SAND, 35000),
        SWAMP("Bataklık (Swamp - Balçık)", Biome.SWAMP, Material.SLIME_BALL, 50000),
        MUSHROOM_FIELDS("Mantar Adası (Canavar Doğmaz)", Biome.MUSHROOM_FIELDS, Material.RED_MUSHROOM, 75000),
        NETHER_WASTES("Nether Atıkları", Biome.NETHER_WASTES, Material.NETHERRACK, 60000),
        DEEP_DARK("Derin Karanlık (Deep Dark)", Biome.DEEP_DARK, Material.SCULK, 100000);

        private final String displayName;
        private final Biome biome;
        private final Material icon;
        private final double cost;

        IslandBiome(String displayName, Biome biome, Material icon, double cost) {
            this.displayName = displayName;
            this.biome = biome;
            this.icon = icon;
            this.cost = cost;
        }

        public String getDisplayName() { return displayName; }
        public Biome getBiome() { return biome; }
        public Material getIcon() { return icon; }
        public double getCost() { return cost; }
    }

    public BiomeManager(GrizmOneBlock plugin) {
        this.plugin = plugin;
    }

    public boolean changeBiome(UUID ownerUUID, Player player, IslandBiome islandBiome) {
        World world = Bukkit.getWorld("island_" + ownerUUID.toString());
        if (world == null) {
            player.sendMessage(Component.text("Ada dünyanız bulunamadı!").color(NamedTextColor.RED));
            return false;
        }

        if (plugin.getEconomyManager().isAvailable() && !plugin.getEconomyManager().hasMoney(player, islandBiome.getCost())) {
            player.sendMessage(Component.text("Yetersiz bakiye! Gerekli: " + (long)islandBiome.getCost() + " Para").color(NamedTextColor.RED));
            return false;
        }

        if (plugin.getEconomyManager().isAvailable()) {
            plugin.getEconomyManager().takeMoney(player, islandBiome.getCost());
        }

        // Yüklü olan tüm chunk'ların biyomunu değiştir
        for (Chunk chunk : world.getLoadedChunks()) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = world.getMinHeight(); y < world.getMaxHeight(); y += 4) {
                        world.setBiome(chunk.getX() * 16 + x, y, chunk.getZ() * 16 + z, islandBiome.getBiome());
                    }
                }
            }
        }

        player.sendMessage(Component.text("Adanızın biyomu başarıyla ").color(NamedTextColor.GREEN)
                .append(Component.text(islandBiome.getDisplayName()).color(NamedTextColor.GOLD))
                .append(Component.text(" olarak değiştirildi!").color(NamedTextColor.GREEN)));
        player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_BUCKET_FILL, 0.7f, 1.2f);

        return true;
    }
}
