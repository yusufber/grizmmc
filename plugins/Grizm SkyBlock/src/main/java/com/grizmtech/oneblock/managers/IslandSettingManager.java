package com.grizmtech.oneblock.managers;

import com.grizmtech.oneblock.GrizmOneBlock;
import com.grizmtech.oneblock.models.IslandSetting;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.WeatherType;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class IslandSettingManager {

    private final GrizmOneBlock plugin;
    private final Map<UUID, Map<IslandSetting, Object>> cache = new ConcurrentHashMap<>();

    public IslandSettingManager(GrizmOneBlock plugin) {
        this.plugin = plugin;
    }

    public boolean getBoolean(UUID ownerUUID, IslandSetting setting) {
        if (ownerUUID == null || setting == null) return false;
        Map<IslandSetting, Object> ownerMap = cache.computeIfAbsent(ownerUUID, k -> new ConcurrentHashMap<>());
        Object cached = ownerMap.get(setting);
        if (cached instanceof Boolean b) {
            return b;
        }
        boolean val = plugin.getDataManager().getSettingBoolean(ownerUUID, setting);
        ownerMap.put(setting, val);
        return val;
    }

    public void setBoolean(UUID ownerUUID, IslandSetting setting, boolean value) {
        if (ownerUUID == null || setting == null) return;
        cache.computeIfAbsent(ownerUUID, k -> new ConcurrentHashMap<>()).put(setting, value);
        plugin.getDataManager().setSettingBoolean(ownerUUID, setting, value);
    }

    public String getString(UUID ownerUUID, IslandSetting setting) {
        if (ownerUUID == null || setting == null) return (String) setting.getDefaultValue();
        Map<IslandSetting, Object> ownerMap = cache.computeIfAbsent(ownerUUID, k -> new ConcurrentHashMap<>());
        Object cached = ownerMap.get(setting);
        if (cached instanceof String s) {
            return s;
        }
        String val = plugin.getDataManager().getSettingString(ownerUUID, setting);
        ownerMap.put(setting, val);
        return val;
    }

    public void setString(UUID ownerUUID, IslandSetting setting, String value) {
        if (ownerUUID == null || setting == null) return;
        cache.computeIfAbsent(ownerUUID, k -> new ConcurrentHashMap<>()).put(setting, value);
        plugin.getDataManager().setSettingString(ownerUUID, setting, value);
    }

    /**
     * Oyuncu ayara tıkladığında değeri değiştirir ve gerekli efektleri uygular.
     */
    public void toggleSetting(Player player, IslandSetting setting) {
        UUID ownerUUID = player.getUniqueId();
        World islandWorld = Bukkit.getWorld("island_" + ownerUUID.toString());

        if (setting == IslandSetting.CORE_BLOCK_MINE) {
            String current = getString(ownerUUID, setting);
            String next = current.equals("MEMBERS_ONLY") ? "EVERYONE" : "MEMBERS_ONLY";
            setString(ownerUUID, setting, next);
            plugin.getDataManager().saveConfig();

            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
            String label = next.equals("MEMBERS_ONLY") ? "Sadece Ada Sahibi" : "Herkes (Ziyaretçiler Dahil)";
            player.sendMessage(Component.text("Ana Blok Kırma İzni: ").color(NamedTextColor.YELLOW)
                    .append(Component.text(label).color(next.equals("MEMBERS_ONLY") ? NamedTextColor.GREEN : NamedTextColor.GOLD)));
            return;
        }

        if (setting == IslandSetting.TIME_WEATHER) {
            String current = getString(ownerUUID, setting);
            String next;
            switch (current) {
                case "SYNC": next = "DAY"; break;
                case "DAY": next = "NIGHT"; break;
                case "NIGHT": next = "SUN"; break;
                case "SUN": next = "RAIN"; break;
                case "RAIN":
                default: next = "SYNC"; break;
            }
            setString(ownerUUID, setting, next);
            plugin.getDataManager().saveConfig();

            if (islandWorld != null) {
                for (Player p : islandWorld.getPlayers()) {
                    applyTimeWeather(p, next);
                }
            }

            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 0.7f, 1.3f);
            player.sendMessage(Component.text("Ada Saati & Hava Durumu: ").color(NamedTextColor.YELLOW)
                    .append(Component.text(getTimeWeatherLabel(next)).color(NamedTextColor.AQUA)));
            return;
        }

        // Boolean ayarlar
        boolean current = getBoolean(ownerUUID, setting);
        boolean next = !current;
        setBoolean(ownerUUID, setting, next);

        // Özel ek davranışlar
        if (setting == IslandSetting.VISITOR_LOCK) {
            if (next) {
                kickVisitors(ownerUUID);
            }
        } else if (setting == IslandSetting.BORDER_TOGGLE) {
            if (islandWorld != null) {
                applyBorder(ownerUUID, islandWorld);
            }
        } else if (setting == IslandSetting.PHASE_LOCK) {
            if (next) {
                int blocks = plugin.getDataManager().getBlocksBroken(ownerUUID);
                PhaseManager.Phase phase = plugin.getPhaseManager().getCurrentPhase(blocks);
                plugin.getDataManager().setLockedPhase(ownerUUID, phase.name());
            } else {
                plugin.getDataManager().setLockedPhase(ownerUUID, null);
            }
        }

        plugin.getDataManager().saveConfig();

        if (next) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 2.0f);
            player.sendMessage(Component.text(setting.getDisplayName() + " ").color(NamedTextColor.YELLOW)
                    .append(Component.text("[AÇIK]").color(NamedTextColor.GREEN)));
        } else {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.8f);
            player.sendMessage(Component.text(setting.getDisplayName() + " ").color(NamedTextColor.YELLOW)
                    .append(Component.text("[KAPALI]").color(NamedTextColor.RED)));
        }
    }

    public String getTimeWeatherLabel(String mode) {
        switch (mode) {
            case "DAY": return "Sürekli Gündüz";
            case "NIGHT": return "Sürekli Gece";
            case "SUN": return "Açık Güneşli";
            case "RAIN": return "Yağmurlu";
            case "SYNC":
            default: return "Sunucu ile Eşzamanlı";
        }
    }

    public void applyTimeWeather(Player player, String mode) {
        if (player == null) return;
        switch (mode) {
            case "DAY":
                player.setPlayerTime(6000L, false);
                player.resetPlayerWeather();
                break;
            case "NIGHT":
                player.setPlayerTime(18000L, false);
                player.resetPlayerWeather();
                break;
            case "SUN":
                player.resetPlayerTime();
                player.setPlayerWeather(WeatherType.CLEAR);
                break;
            case "RAIN":
                player.resetPlayerTime();
                player.setPlayerWeather(WeatherType.DOWNFALL);
                break;
            case "SYNC":
            default:
                player.resetPlayerTime();
                player.resetPlayerWeather();
                break;
        }
    }

    public void applyPlayerAtmosphere(Player player, World world) {
        if (player == null || world == null) return;
        String worldName = world.getName();
        if (worldName.startsWith("island_")) {
            try {
                UUID ownerUUID = UUID.fromString(worldName.replace("island_", ""));
                String mode = getString(ownerUUID, IslandSetting.TIME_WEATHER);
                applyTimeWeather(player, mode);
                return;
            } catch (Exception ignored) {}
        }
        player.resetPlayerTime();
        player.resetPlayerWeather();
    }

    public void applyBorder(UUID ownerUUID, World world) {
        if (world == null || ownerUUID == null) return;
        boolean borderVisible = getBoolean(ownerUUID, IslandSetting.BORDER_TOGGLE);
        if (borderVisible) {
            plugin.getUpgradeManager().applyBorder(ownerUUID, world);
        } else {
            world.getWorldBorder().setCenter(0.5, 0.5);
            world.getWorldBorder().setSize(59999996.0);
        }
    }

    public void kickVisitors(UUID ownerUUID) {
        World world = Bukkit.getWorld("island_" + ownerUUID.toString());
        if (world == null) return;
        World mainWorld = Bukkit.getWorlds().get(0);
        org.bukkit.Location spawnLoc = (mainWorld != null) ? mainWorld.getSpawnLocation() : null;

        for (Player p : world.getPlayers()) {
            if (!p.getUniqueId().equals(ownerUUID) && !p.hasPermission("grizmoneblock.admin")) {
                if (spawnLoc != null) {
                    p.teleport(spawnLoc);
                }
                p.sendMessage(Component.text("Ada sahibi adayı kilitlediği için spawna ışınlandınız!").color(NamedTextColor.YELLOW));
            }
        }
    }

    public boolean isContainer(Material mat, BlockState state) {
        if (state instanceof Container) return true;
        if (mat == null) return false;
        String name = mat.name();
        return name.contains("CHEST") || name.contains("BARREL") || name.contains("SHULKER")
                || name.contains("FURNACE") || name.contains("SMOKER") || name.contains("HOPPER")
                || name.contains("DISPENSER") || name.contains("DROPPER") || name.contains("BREWING")
                || name.contains("CAULDRON") || name.contains("JUKEBOX") || name.contains("POT");
    }

    public boolean isRedstoneInteractable(Material mat) {
        if (mat == null) return false;
        String name = mat.name();
        return name.contains("BUTTON")
                || name.equals("LEVER")
                || name.contains("PRESSURE_PLATE")
                || name.contains("DOOR")
                || name.contains("GATE")
                || name.contains("TRAPDOOR")
                || name.equals("REPEATER")
                || name.equals("COMPARATOR")
                || name.equals("DAYLIGHT_DETECTOR")
                || name.equals("BELL")
                || name.equals("TARGET")
                || name.equals("NOTE_BLOCK");
    }
}
