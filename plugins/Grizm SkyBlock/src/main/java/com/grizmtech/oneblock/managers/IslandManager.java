package com.grizmtech.oneblock.managers;

import com.grizmtech.oneblock.GrizmOneBlock;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class IslandManager {

    private final GrizmOneBlock plugin;

    public IslandManager(GrizmOneBlock plugin) {
        this.plugin = plugin;
    }

    public void createIsland(Player player) {
        if (plugin.getDataManager().hasIsland(player.getUniqueId())) {
            player.sendMessage(Component.text("Zaten bir adanız var! Adanıza ışınlanıyorsunuz...").color(NamedTextColor.YELLOW));
            teleportToIsland(player);
            return;
        }

        String worldName = "island_" + player.getUniqueId().toString();
        player.sendMessage(Component.text("Size özel dünyanız oluşturuluyor, lütfen bekleyin...").color(NamedTextColor.YELLOW));

        // Dünyayı oluştur (Void Chunk Generator kullanarak tamamen boş bir dünya)
        org.bukkit.WorldCreator wc = new org.bukkit.WorldCreator(worldName);
        wc.generator(new com.grizmtech.oneblock.world.VoidChunkGenerator());
        World world = Bukkit.createWorld(wc);

        if (world == null) {
            player.sendMessage(Component.text("Dünya oluşturulurken bir hata oluştu! Lütfen yetkiliye bildirin.").color(NamedTextColor.RED));
            return;
        }

        // Dünya oyun kuralları
        world.setGameRule(org.bukkit.GameRule.DO_IMMEDIATE_RESPAWN, true);
        world.setGameRule(org.bukkit.GameRule.SPAWN_RADIUS, 0);

        // Sınırı ayarla (Varsayılan Level 1 veya ayara göre)
        plugin.getIslandSettingManager().applyBorder(player.getUniqueId(), world);

        // Y:63 seviyesine kırılmaz taban (Bedrock) koy - böylece oyuncu ve düşen kumlar boşluğa kaybolmaz
        Location bedrockLoc = new Location(world, 0, 63, 0);
        bedrockLoc.getBlock().setType(Material.BEDROCK);

        // Y:64 seviyesine ilk OneBlock bloğunu yerleştir (Çimen)
        Location islandLoc = new Location(world, 0, 64, 0);
        islandLoc.getBlock().setType(Material.GRASS_BLOCK);

        // Oyuncuya bu konumu OneBlock olarak kaydet
        plugin.getDataManager().setOneBlockLocation(player.getUniqueId(), islandLoc);
        plugin.getDataManager().saveConfig();

        // Hologramı başlat
        plugin.getHologramManager().updateHologram(player.getUniqueId());

        // Spawn noktasını belirle ve oyuncuyu adaya ışınla
        world.setSpawnLocation(0, 65, 0);
        Location tpLoc = new Location(world, 0.5, 65, 0.5);
        player.teleport(tpLoc);

        player.sendMessage(Component.text("Kişisel adanız başarıyla oluşturuldu ve adanıza ışınlandınız!").color(NamedTextColor.GREEN));
    }

    public void teleportToIsland(Player player) {
        Location islandLoc = plugin.getDataManager().getOneBlockLocation(player.getUniqueId());
        if (islandLoc != null && islandLoc.getWorld() != null) {
            // Işınlanma konumu bloğun tam üstü olsun
            Location tpLoc = islandLoc.clone().add(0.5, 1, 0.5);
            player.teleport(tpLoc);
            player.sendMessage(Component.text("Adanıza ışınlandınız!").color(NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Henüz bir adanız yok! '/ada' komutunu kullanarak hemen oluşturabilirsiniz.").color(NamedTextColor.RED));
        }
    }
}
