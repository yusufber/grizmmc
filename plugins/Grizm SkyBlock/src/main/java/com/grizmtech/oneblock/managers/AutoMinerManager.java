package com.grizmtech.oneblock.managers;

import com.grizmtech.oneblock.GrizmOneBlock;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class AutoMinerManager {

    private final GrizmOneBlock plugin;

    public AutoMinerManager(GrizmOneBlock plugin) {
        this.plugin = plugin;
        startAutoMinerTask();
    }

    public boolean isEnabled(UUID uuid) {
        return plugin.getDataManager().getConfig().getBoolean("players." + uuid + ".autominer.enabled", false);
    }

    public void setEnabled(UUID uuid, boolean enabled) {
        plugin.getDataManager().getConfig().set("players." + uuid + ".autominer.enabled", enabled);
        plugin.getDataManager().saveConfig();
    }

    public int getFuel(UUID uuid) {
        return plugin.getDataManager().getConfig().getInt("players." + uuid + ".autominer.fuel", 0);
    }

    public void setFuel(UUID uuid, int fuel) {
        plugin.getDataManager().getConfig().set("players." + uuid + ".autominer.fuel", Math.max(0, fuel));
        plugin.getDataManager().saveConfig();
    }

    public void addFuel(UUID uuid, int amount) {
        setFuel(uuid, getFuel(uuid) + amount);
    }

    public int getSpeedLevel(UUID uuid) {
        return plugin.getDataManager().getConfig().getInt("players." + uuid + ".autominer.speed_level", 1);
    }

    public void setSpeedLevel(UUID uuid, int level) {
        plugin.getDataManager().getConfig().set("players." + uuid + ".autominer.speed_level", level);
        plugin.getDataManager().saveConfig();
    }

    public boolean depositFuelItem(Player player, Material mat) {
        UUID uuid = player.getUniqueId();
        int fuelAdd = 0;

        if (mat == Material.COAL || mat == Material.CHARCOAL) {
            if (player.getInventory().contains(Material.COAL)) {
                removeItem(player, Material.COAL, 1);
                fuelAdd = 20;
            } else if (player.getInventory().contains(Material.CHARCOAL)) {
                removeItem(player, Material.CHARCOAL, 1);
                fuelAdd = 20;
            } else {
                player.sendMessage(Component.text("Envanterinizde Kömür bulunamadı!").color(NamedTextColor.RED));
                return false;
            }
        } else if (mat == Material.COAL_BLOCK) {
            if (player.getInventory().contains(Material.COAL_BLOCK)) {
                removeItem(player, Material.COAL_BLOCK, 1);
                fuelAdd = 180;
            } else {
                player.sendMessage(Component.text("Envanterinizde Kömür Bloğu bulunamadı!").color(NamedTextColor.RED));
                return false;
            }
        } else if (mat == Material.LAVA_BUCKET) {
            if (player.getInventory().contains(Material.LAVA_BUCKET)) {
                removeItem(player, Material.LAVA_BUCKET, 1);
                player.getInventory().addItem(new org.bukkit.inventory.ItemStack(Material.BUCKET));
                fuelAdd = 500;
            } else {
                player.sendMessage(Component.text("Envanterinizde Lav Kovası bulunamadı!").color(NamedTextColor.RED));
                return false;
            }
        }

        if (fuelAdd > 0) {
            addFuel(uuid, fuelAdd);
            player.sendMessage(Component.text("Minyona +" + fuelAdd + " Yakıt eklendi! Toplam: " + getFuel(uuid)).color(NamedTextColor.GREEN));
            player.playSound(player.getLocation(), Sound.BLOCK_FURNACE_FIRE_CRACKLE, 0.7f, 1.2f);
            return true;
        }
        return false;
    }

    private void removeItem(Player player, Material mat, int amount) {
        for (org.bukkit.inventory.ItemStack is : player.getInventory().getContents()) {
            if (is != null && is.getType() == mat) {
                int newAmount = is.getAmount() - amount;
                if (newAmount > 0) {
                    is.setAmount(newAmount);
                    return;
                } else {
                    amount -= is.getAmount();
                    player.getInventory().remove(is);
                    if (amount <= 0) return;
                }
            }
        }
    }

    public boolean buyFuelWithMoney(Player player, double cost, int fuel) {
        UUID uuid = player.getUniqueId();
        if (plugin.getEconomyManager().isAvailable() && plugin.getEconomyManager().hasMoney(player, cost)) {
            plugin.getEconomyManager().takeMoney(player, cost);
            addFuel(uuid, fuel);
            player.sendMessage(Component.text("+" + fuel + " Yakıt satın alındı! (Ücret: " + (long)cost + " Para)").color(NamedTextColor.GREEN));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.4f);
            return true;
        }
        player.sendMessage(Component.text("Yetersiz bakiye!").color(NamedTextColor.RED));
        return false;
    }

    private void startAutoMinerTask() {
        // Her 20 tickte (1 saniye) bir çalışır
        new BukkitRunnable() {
            private int tickCounter = 0;

            @Override
            public void run() {
                tickCounter++;
                if (!plugin.getDataManager().getConfig().contains("players")) return;

                for (String uuidStr : plugin.getDataManager().getConfig().getConfigurationSection("players").getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        if (!isEnabled(uuid)) continue;

                        int fuel = getFuel(uuid);
                        if (fuel <= 0) {
                            setEnabled(uuid, false);
                            continue;
                        }

                        // TPS Koruma: Sadece adada AKTİF bir üye varken çalışır
                        World world = Bukkit.getWorld("island_" + uuid.toString());
                        if (world == null || world.getPlayers().isEmpty()) continue;

                        int speedLevel = getSpeedLevel(uuid);
                        int requiredSeconds = Math.max(1, 6 - speedLevel); // Seviye 1: 5s, Seviye 5: 1s

                        if (tickCounter % requiredSeconds == 0) {
                            performAutoMine(uuid, world);
                        }
                    } catch (Exception ignored) {}
                }
            }
        }.runTaskTimer(plugin, 40L, 20L);
    }

    private void performAutoMine(UUID uuid, World world) {
        Location oneBlockLoc = plugin.getDataManager().getOneBlockLocation(uuid);
        if (oneBlockLoc == null || oneBlockLoc.getWorld() == null) return;

        Block block = oneBlockLoc.getBlock();
        if (block.getType() == Material.AIR || block.getType() == Material.BEDROCK) return;

        Material currentType = block.getType();

        // Yakıtı 1 düşür
        setFuel(uuid, getFuel(uuid) - 1);

        // Kırılan blok sayısını arttır
        int currentBlocks = plugin.getDataManager().getBlocksBroken(uuid);
        int newBlocks = currentBlocks + 1;
        plugin.getDataManager().setBlocksBroken(uuid, newBlocks);

        PhaseManager.Phase phase = plugin.getPhaseManager().getCurrentPhase(newBlocks);
        double oreMultiplier = plugin.getRebirthManager().getOreMultiplier(uuid);
        Material nextBlock = plugin.getPhaseManager().getRandomBlock(phase, oreMultiplier);

        // Eşyayı bağlı sandığa aktar veya bloğun üstüne düşür
        org.bukkit.inventory.ItemStack dropItem = new org.bukkit.inventory.ItemStack(currentType);
        boolean storedInChest = false;
        for (org.bukkit.block.BlockFace face : new org.bukkit.block.BlockFace[]{
                org.bukkit.block.BlockFace.DOWN, org.bukkit.block.BlockFace.NORTH,
                org.bukkit.block.BlockFace.SOUTH, org.bukkit.block.BlockFace.EAST,
                org.bukkit.block.BlockFace.WEST, org.bukkit.block.BlockFace.UP}) {
            Block rel = block.getRelative(face);
            if (rel.getState() instanceof org.bukkit.block.Container container) {
                if (container.getInventory().addItem(dropItem).isEmpty()) {
                    storedInChest = true;
                    break;
                }
            }
        }
        if (!storedInChest) {
            oneBlockLoc.getWorld().dropItemNaturally(oneBlockLoc.clone().add(0.5, 1.0, 0.5), dropItem);
        }

        // Efekt ve Ses
        oneBlockLoc.getWorld().spawnParticle(Particle.CRIT, oneBlockLoc.clone().add(0.5, 0.5, 0.5), 8, 0.2, 0.2, 0.2, 0.05);
        try {
            oneBlockLoc.getWorld().playSound(oneBlockLoc, Sound.BLOCK_STONE_BREAK, 0.4f, 1.2f);
        } catch (Exception ignored) {}

        // Bloğu güncelle
        block.setType(nextBlock);

        // Hologramı güncelle
        plugin.getHologramManager().updateHologram(uuid);

        // Yakıt bitti mi kontrolü
        if (getFuel(uuid) <= 0) {
            setEnabled(uuid, false);
            Player owner = Bukkit.getPlayer(uuid);
            if (owner != null && owner.isOnline()) {
                owner.sendMessage(Component.text("⚠ Otomatik Kırıcınızın (Minyon) yakıtı tükendi!").color(NamedTextColor.RED));
                owner.playSound(owner.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.8f);
            }
        }
    }
}
