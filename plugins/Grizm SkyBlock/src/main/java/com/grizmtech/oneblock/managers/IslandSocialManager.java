package com.grizmtech.oneblock.managers;

import com.grizmtech.oneblock.GrizmOneBlock;
import com.grizmtech.oneblock.models.IslandWarp;
import com.grizmtech.oneblock.models.WarpCategory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class IslandSocialManager {

    private final GrizmOneBlock plugin;
    private final Map<UUID, IslandWarp> warps = new ConcurrentHashMap<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM HH:mm");

    public IslandSocialManager(GrizmOneBlock plugin) {
        this.plugin = plugin;
        loadWarps();
    }

    private void loadWarps() {
        ConfigurationSection sec = plugin.getDataManager().getConfig().getConfigurationSection("warps");
        if (sec == null) return;

        for (String uuidStr : sec.getKeys(false)) {
            try {
                UUID ownerUUID = UUID.fromString(uuidStr);
                String path = "warps." + uuidStr;
                String ownerName = plugin.getDataManager().getConfig().getString(path + ".owner_name", "Oyuncu");
                String worldName = plugin.getDataManager().getConfig().getString(path + ".world");
                double x = plugin.getDataManager().getConfig().getDouble(path + ".x");
                double y = plugin.getDataManager().getConfig().getDouble(path + ".y");
                double z = plugin.getDataManager().getConfig().getDouble(path + ".z");
                float yaw = (float) plugin.getDataManager().getConfig().getDouble(path + ".yaw");
                float pitch = (float) plugin.getDataManager().getConfig().getDouble(path + ".pitch");

                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    Location loc = new Location(world, x, y, z, yaw, pitch);
                    WarpCategory cat = WarpCategory.valueOf(plugin.getDataManager().getConfig().getString(path + ".category", "MARKET"));
                    String desc = plugin.getDataManager().getConfig().getString(path + ".desc", "Ada Warpi");
                    int likes = plugin.getDataManager().getConfig().getInt(path + ".likes", 0);
                    long created = plugin.getDataManager().getConfig().getLong(path + ".created", System.currentTimeMillis());

                    warps.put(ownerUUID, new IslandWarp(ownerUUID, ownerName, loc, cat, desc, likes, created));
                }
            } catch (Exception ignored) {}
        }
    }

    public void setWarp(Player player, WarpCategory category, String description) {
        UUID uuid = player.getUniqueId();
        Location loc = player.getLocation();

        IslandWarp warp = new IslandWarp(uuid, player.getName(), loc, category, description, getLikes(uuid), System.currentTimeMillis());
        warps.put(uuid, warp);

        String path = "warps." + uuid.toString();
        plugin.getDataManager().getConfig().set(path + ".owner_name", player.getName());
        plugin.getDataManager().getConfig().set(path + ".world", loc.getWorld().getName());
        plugin.getDataManager().getConfig().set(path + ".x", loc.getX());
        plugin.getDataManager().getConfig().set(path + ".y", loc.getY());
        plugin.getDataManager().getConfig().set(path + ".z", loc.getZ());
        plugin.getDataManager().getConfig().set(path + ".yaw", loc.getYaw());
        plugin.getDataManager().getConfig().set(path + ".pitch", loc.getPitch());
        plugin.getDataManager().getConfig().set(path + ".category", category.name());
        plugin.getDataManager().getConfig().set(path + ".desc", description);
        plugin.getDataManager().getConfig().set(path + ".likes", warp.getLikes());
        plugin.getDataManager().getConfig().set(path + ".created", warp.getCreatedTime());
        plugin.getDataManager().saveConfig();

        player.sendMessage(Component.text("Ada warpınız başarıyla yayınlandı! Kategori: ").color(NamedTextColor.GREEN)
                .append(Component.text(category.getDisplayName()).color(NamedTextColor.GOLD)));
    }

    public void removeWarp(UUID ownerUUID) {
        warps.remove(ownerUUID);
        plugin.getDataManager().getConfig().set("warps." + ownerUUID.toString(), null);
        plugin.getDataManager().saveConfig();
    }

    public IslandWarp getWarp(UUID ownerUUID) {
        return warps.get(ownerUUID);
    }

    public List<IslandWarp> getWarps(WarpCategory filter) {
        List<IslandWarp> list = new ArrayList<>();
        for (IslandWarp w : warps.values()) {
            if (filter == null || w.getCategory() == filter) {
                list.add(w);
            }
        }
        return list;
    }

    public int getLikes(UUID ownerUUID) {
        return plugin.getDataManager().getConfig().getInt("players." + ownerUUID + ".likes", 0);
    }

    public boolean likeIsland(Player visitor, UUID ownerUUID) {
        if (visitor.getUniqueId().equals(ownerUUID)) {
            visitor.sendMessage(Component.text("Kendi adanızı beğenemezsiniz!").color(NamedTextColor.RED));
            return false;
        }

        String cooldownPath = "players." + ownerUUID + ".liked_by." + visitor.getUniqueId();
        long lastLike = plugin.getDataManager().getConfig().getLong(cooldownPath, 0L);
        long now = System.currentTimeMillis();

        if (now - lastLike < 86400000L) { // 24 saat bekleme
            long remainingHours = (86400000L - (now - lastLike)) / 3600000L;
            visitor.sendMessage(Component.text("Bu adayı zaten beğendiniz! Tekrar oy vermek için " + remainingHours + " saat beklemelisiniz.").color(NamedTextColor.RED));
            return false;
        }

        int current = getLikes(ownerUUID);
        int next = current + 1;
        plugin.getDataManager().getConfig().set("players." + ownerUUID + ".likes", next);
        plugin.getDataManager().getConfig().set(cooldownPath, now);

        IslandWarp warp = warps.get(ownerUUID);
        if (warp != null) {
            warp.setLikes(next);
            plugin.getDataManager().getConfig().set("warps." + ownerUUID + ".likes", next);
        }

        plugin.getDataManager().saveConfig();

        visitor.sendMessage(Component.text("Adası beğenildi! Toplam Beğeni: " + next + " ★").color(NamedTextColor.GREEN));
        visitor.playSound(visitor.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.4f);

        Player owner = Bukkit.getPlayer(ownerUUID);
        if (owner != null && owner.isOnline()) {
            owner.sendMessage(Component.text(visitor.getName() + " adanızı beğendi! ★").color(NamedTextColor.GOLD));
        }

        return true;
    }

    public void leaveMessage(Player visitor, UUID ownerUUID, String msg) {
        String entry = "[" + dateFormat.format(new Date()) + "] " + visitor.getName() + ": " + msg;
        List<String> list = getGuestbook(ownerUUID);
        list.add(0, entry);
        if (list.size() > 10) {
            list = list.subList(0, 10);
        }
        plugin.getDataManager().getConfig().set("players." + ownerUUID + ".guestbook", list);
        plugin.getDataManager().saveConfig();

        visitor.sendMessage(Component.text("Ziyaretçi defterine mesajınız bırakıldı!").color(NamedTextColor.GREEN));
    }

    public List<String> getGuestbook(UUID ownerUUID) {
        List<String> list = plugin.getDataManager().getConfig().getStringList("players." + ownerUUID + ".guestbook");
        return new ArrayList<>(list);
    }

    public boolean tipOwner(Player visitor, UUID ownerUUID, double amount) {
        if (visitor.getUniqueId().equals(ownerUUID)) {
            visitor.sendMessage(Component.text("Kendinize bahşiş atamazsınız!").color(NamedTextColor.RED));
            return false;
        }

        if (!Double.isFinite(amount) || amount <= 0 || !plugin.getEconomyManager().hasMoney(visitor, amount)) {
            visitor.sendMessage(Component.text("Yetersiz bakiye!").color(NamedTextColor.RED));
            return false;
        }

        plugin.getEconomyManager().takeMoney(visitor, amount);

        // Ada Bankasına ekle
        double bank = plugin.getBankManager().getBalance(ownerUUID);
        plugin.getBankManager().setBalance(ownerUUID, bank + amount);
        plugin.getBankManager().logTransaction(ownerUUID, visitor.getName() + " +" + (long)amount + " Bahşiş bıraktı.");

        leaveMessage(visitor, ownerUUID, "♥ " + (long)amount + " Para bahşiş bıraktı.");

        visitor.sendMessage(Component.text("Ada sahibine ").color(NamedTextColor.GREEN)
                .append(Component.text((long)amount + " Para").color(NamedTextColor.GOLD))
                .append(Component.text(" bahşiş gönderildi!").color(NamedTextColor.GREEN)));

        Player owner = Bukkit.getPlayer(ownerUUID);
        if (owner != null && owner.isOnline()) {
            owner.sendMessage(Component.text(visitor.getName() + " adanıza ").color(NamedTextColor.GOLD)
                    .append(Component.text((long)amount + " Para").color(NamedTextColor.GREEN))
                    .append(Component.text(" bahşiş bıraktı!").color(NamedTextColor.GOLD)));
        }

        return true;
    }

    public List<IslandWarp> getPopularIslands() {
        List<IslandWarp> list = new ArrayList<>(warps.values());
        list.sort((a, b) -> Integer.compare(b.getLikes(), a.getLikes()));
        return list;
    }
}
