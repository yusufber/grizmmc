package com.grizmtech.oneblock.listeners;

import com.grizmtech.oneblock.GrizmOneBlock;
import com.grizmtech.oneblock.managers.BiomeManager;
import com.grizmtech.oneblock.managers.GUIManager;
import com.grizmtech.oneblock.models.IslandSetting;
import com.grizmtech.oneblock.models.IslandWarp;
import com.grizmtech.oneblock.models.WarpCategory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.HashMap;
import java.util.UUID;

public class InventoryClickListener implements Listener {

    private final GrizmOneBlock plugin;

    public InventoryClickListener(GrizmOneBlock plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) return;
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        if (title.equals("Ada Menüsü")) {
            event.setCancelled(true);
            if (!event.getClickedInventory().equals(event.getView().getTopInventory())) return;

            Player player = (Player) event.getWhoClicked();
            int slot = event.getSlot();

            if (slot == 10) {
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getGuiManager().openUpgradeMenu(player));
            } else if (slot == 11) {
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getGuiManager().openBankMenu(player));
            } else if (slot == 13) {
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getGuiManager().openMissionsMenu(player));
            } else if (slot == 14) {
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getGuiManager().openSettingsMenu(player));
            } else if (slot == 15) {
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getGuiManager().openBiomesMenu(player));
            } else if (slot == 16) {
                player.closeInventory();
                if (plugin.getDataManager().hasIsland(player.getUniqueId())) {
                    plugin.getIslandManager().teleportToIsland(player);
                } else {
                    plugin.getIslandManager().createIsland(player);
                }
            } else if (slot == 20) {
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getGuiManager().openAutoMinerMenu(player));
            } else if (slot == 21) {
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getGuiManager().openSkillsMenu(player));
            } else if (slot == 22) {
                player.closeInventory();
                plugin.getRebirthManager().doPrestige(player);
            } else if (slot == 24) {
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getGuiManager().openWarpsMenu(player, null));
            }
        } else if (title.equals("Ada Bankası")) {
            event.setCancelled(true);
            if (!event.getClickedInventory().equals(event.getView().getTopInventory())) return;

            Player player = (Player) event.getWhoClicked();
            UUID uuid = player.getUniqueId();
            int slot = event.getSlot();

            if (slot == 31) {
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getGuiManager().openAdaMenu(player));
                return;
            }

            if (slot == 11) {
                plugin.getBankManager().deposit(uuid, player, 10000.0);
            } else if (slot == 12) {
                plugin.getBankManager().deposit(uuid, player, 100000.0);
            } else if (slot == 13) {
                double myMoney = plugin.getEconomyManager().isAvailable() ? plugin.getEconomyManager().getBalance(player) : 0.0;
                plugin.getBankManager().deposit(uuid, player, myMoney);
            } else if (slot == 19) {
                plugin.getBankManager().withdraw(uuid, player, 10000.0);
            } else if (slot == 20) {
                plugin.getBankManager().withdraw(uuid, player, 100000.0);
            } else if (slot == 21) {
                double bankBal = plugin.getBankManager().getBalance(uuid);
                plugin.getBankManager().withdraw(uuid, player, bankBal);
            } else if (slot == 15) {
                plugin.getBankManager().depositExp(uuid, player, 100);
            } else if (slot == 24) {
                plugin.getBankManager().withdrawExp(uuid, player, 100);
            }

            Bukkit.getScheduler().runTask(plugin, () -> plugin.getGuiManager().openBankMenu(player));

        } else if (title.equals("Biyom Değiştirici")) {
            event.setCancelled(true);
            if (!event.getClickedInventory().equals(event.getView().getTopInventory())) return;

            Player player = (Player) event.getWhoClicked();
            int slot = event.getSlot();

            if (slot == 22) {
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getGuiManager().openAdaMenu(player));
                return;
            }

            int index = slot - 9;
            BiomeManager.IslandBiome[] biomes = BiomeManager.IslandBiome.values();
            if (index >= 0 && index < biomes.length) {
                plugin.getBiomeManager().changeBiome(player.getUniqueId(), player, biomes[index]);
            }

        } else if (title.equals("Aşama Görevleri")) {
            event.setCancelled(true);
            if (!event.getClickedInventory().equals(event.getView().getTopInventory())) return;

            Player player = (Player) event.getWhoClicked();
            if (event.getSlot() == 22) {
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getGuiManager().openAdaMenu(player));
            }

        } else if (title.equals("Ada Geliştirmeleri")) {
            event.setCancelled(true);
            if (!event.getClickedInventory().equals(event.getView().getTopInventory())) return;

            Player player = (Player) event.getWhoClicked();
            UUID uuid = player.getUniqueId();
            int slot = event.getSlot();

            if (slot == 31) {
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getGuiManager().openAdaMenu(player));
                return;
            }

            if (slot == 10) {
                // Sınır
                int level = plugin.getDataManager().getBorderLevel(uuid);
                double cost = plugin.getUpgradeManager().getBorderUpgradeCost(level);
                if (cost > 0 && tryPay(player, uuid, cost)) {
                    plugin.getDataManager().setBorderLevel(uuid, level + 1);
                    plugin.getDataManager().saveConfig();
                    World world = Bukkit.getWorld("island_" + uuid.toString());
                    if (world != null) {
                        plugin.getIslandSettingManager().applyBorder(uuid, world);
                    }
                    player.sendMessage(Component.text("Dünya sınırınız genişletildi!").color(NamedTextColor.GREEN));
                }
            } else if (slot == 11) {
                // Jeneratör (Cobblestone)
                int level = plugin.getDataManager().getGeneratorLevel(uuid);
                double cost = plugin.getUpgradeManager().getGeneratorUpgradeCost(level);
                if (cost > 0 && tryPay(player, uuid, cost)) {
                    plugin.getDataManager().setGeneratorLevel(uuid, level + 1);
                    plugin.getDataManager().saveConfig();
                    player.sendMessage(Component.text("Maden jeneratörünüz yükseltildi!").color(NamedTextColor.GREEN));
                }
            } else if (slot == 12) {
                // Huni
                int level = plugin.getDataManager().getHopperLevel(uuid);
                double cost = plugin.getUpgradeManager().getHopperUpgradeCost(level);
                if (cost > 0 && tryPay(player, uuid, cost)) {
                    plugin.getDataManager().setHopperLevel(uuid, level + 1);
                    plugin.getDataManager().saveConfig();
                    player.sendMessage(Component.text("Huni sınırınız yükseltildi!").color(NamedTextColor.GREEN));
                }
            } else if (slot == 14) {
                // Ana Blok Zenginliği (Booster)
                int level = plugin.getUpgradeManager().getGeneratorBooster(uuid);
                double cost = plugin.getUpgradeManager().getGeneratorBoosterCost(level);
                if (cost > 0 && tryPay(player, uuid, cost)) {
                    plugin.getUpgradeManager().setGeneratorBooster(uuid, level + 1);
                    player.sendMessage(Component.text("Ana Blok Zenginliği yükseltildi! (+%5 Değerli Maden Şansı)").color(NamedTextColor.GREEN));
                }
            } else if (slot == 15) {
                // Ekin & Spawner Hızı
                int level = plugin.getUpgradeManager().getSpeedBooster(uuid);
                double cost = plugin.getUpgradeManager().getSpeedBoosterCost(level);
                if (cost > 0 && tryPay(player, uuid, cost)) {
                    plugin.getUpgradeManager().setSpeedBooster(uuid, level + 1);
                    player.sendMessage(Component.text("Ekin ve Spawner hızınız yükseltildi!").color(NamedTextColor.GREEN));
                }
            }

            Bukkit.getScheduler().runTask(plugin, () -> plugin.getGuiManager().openUpgradeMenu(player));

        } else if (title.equals("Ada Ayarları ve İzinler")) {
            event.setCancelled(true);
            if (!event.getClickedInventory().equals(event.getView().getTopInventory())) return;

            Player player = (Player) event.getWhoClicked();
            int slot = event.getSlot();

            if (slot == 49) {
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getGuiManager().openAdaMenu(player));
                return;
            }

            IslandSetting setting = GUIManager.SETTING_SLOTS.get(slot);
            if (setting != null) {
                plugin.getIslandSettingManager().toggleSetting(player, setting);
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getGuiManager().openSettingsMenu(player));
            }
        } else if (title.startsWith("Ajan (Spy):")) {
            event.setCancelled(true);
        } else if (title.equals("Ada Yetenek Ağacı")) {
            event.setCancelled(true);
            if (!event.getClickedInventory().equals(event.getView().getTopInventory())) return;

            Player player = (Player) event.getWhoClicked();
            int slot = event.getSlot();

            if (slot == 31) {
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getGuiManager().openAdaMenu(player));
                return;
            }

            if (slot == 10) {
                plugin.getSkillManager().upgradeSkill(player, "durability", 1, 3);
            } else if (slot == 19) {
                plugin.getSkillManager().upgradeSkill(player, "double_drop", 2, 5);
            } else if (slot == 12) {
                plugin.getSkillManager().upgradeSkill(player, "mob_damage", 1, 3);
            } else if (slot == 21) {
                plugin.getSkillManager().upgradeSkill(player, "extra_xp", 2, 3);
            } else if (slot == 14) {
                plugin.getSkillManager().upgradeSkill(player, "fly", 5, 1);
            } else if (slot == 23) {
                plugin.getSkillManager().upgradeSkill(player, "auto_replant", 3, 1);
            }

            Bukkit.getScheduler().runTask(plugin, () -> plugin.getGuiManager().openSkillsMenu(player));

        } else if (title.equals("Otomatik Kırıcı (Minyon)")) {
            event.setCancelled(true);
            if (!event.getClickedInventory().equals(event.getView().getTopInventory())) return;

            Player player = (Player) event.getWhoClicked();
            UUID uuid = player.getUniqueId();
            int slot = event.getSlot();

            if (slot == 31) {
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getGuiManager().openAdaMenu(player));
                return;
            }

            if (slot == 4) {
                boolean current = plugin.getAutoMinerManager().isEnabled(uuid);
                if (!current && plugin.getAutoMinerManager().getFuel(uuid) <= 0) {
                    player.sendMessage(Component.text("Otomatik Kırıcıyı çalıştırmak için önce yakıt doldurmalısınız!").color(NamedTextColor.RED));
                } else {
                    plugin.getAutoMinerManager().setEnabled(uuid, !current);
                    player.sendMessage(Component.text("Otomatik Kırıcı: " + (!current ? "[AÇIK]" : "[KAPALI]")).color(!current ? NamedTextColor.GREEN : NamedTextColor.RED));
                }
            } else if (slot == 11) {
                plugin.getAutoMinerManager().depositFuelItem(player, Material.COAL);
            } else if (slot == 12) {
                plugin.getAutoMinerManager().depositFuelItem(player, Material.COAL_BLOCK);
            } else if (slot == 13) {
                plugin.getAutoMinerManager().depositFuelItem(player, Material.LAVA_BUCKET);
            } else if (slot == 15) {
                plugin.getAutoMinerManager().buyFuelWithMoney(player, 10000.0, 1000);
            } else if (slot == 22) {
                int speedLvl = plugin.getAutoMinerManager().getSpeedLevel(uuid);
                if (speedLvl >= 5) {
                    player.sendMessage(Component.text("Kırma hızı zaten maksimum seviyede!").color(NamedTextColor.RED));
                } else {
                    double cost = speedLvl * 50000.0;
                    if (tryPay(player, uuid, cost)) {
                        plugin.getAutoMinerManager().setSpeedLevel(uuid, speedLvl + 1);
                        player.sendMessage(Component.text("Kırma hızı yükseltildi! (Seviye " + (speedLvl + 1) + "/5)").color(NamedTextColor.GREEN));
                    }
                }
            } else if (slot == 24) {
                double cost = 25000.0;
                if (tryPay(player, uuid, cost)) {
                    ItemStack minionItem = plugin.getGeneratorMinionManager().createMinionItem(1);
                    HashMap<Integer, ItemStack> rem = player.getInventory().addItem(minionItem);
                    for (ItemStack left : rem.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), left);
                    }
                    player.sendMessage(Component.text("✔ Su-Lav Jeneratör Minyonu satın aldınız! Adanızdaki jeneratörün yanına yerleştirebilirsiniz.").color(NamedTextColor.GREEN));
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
                }
            }

            Bukkit.getScheduler().runTask(plugin, () -> plugin.getGuiManager().openAutoMinerMenu(player));

        } else if (title.equals("Ada İlan Panosu & Warplar")) {
            event.setCancelled(true);
            if (!event.getClickedInventory().equals(event.getView().getTopInventory())) return;

            Player player = (Player) event.getWhoClicked();
            int slot = event.getSlot();

            if (slot == 49) {
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getGuiManager().openAdaMenu(player));
                return;
            }

            if (slot == 1) {
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getGuiManager().openWarpsMenu(player, null));
                return;
            } else if (slot == 3) {
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getGuiManager().openWarpsMenu(player, WarpCategory.MARKET));
                return;
            } else if (slot == 5) {
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getGuiManager().openWarpsMenu(player, WarpCategory.XP_FARM));
                return;
            } else if (slot == 7) {
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getGuiManager().openWarpsMenu(player, WarpCategory.SERGI));
                return;
            }

            if (event.getCurrentItem() != null && event.getCurrentItem().hasItemMeta() && event.getCurrentItem().getItemMeta().hasDisplayName()) {
                String dName = PlainTextComponentSerializer.plainText().serialize(event.getCurrentItem().getItemMeta().displayName());
                if (dName.endsWith(" Adasına Git")) {
                    String targetName = dName.replace(" Adasına Git", "").trim();
                    for (IslandWarp w : plugin.getIslandSocialManager().getWarps(null)) {
                        if (w.getOwnerName().equalsIgnoreCase(targetName)) {
                            player.closeInventory();
                            player.teleport(w.getLocation());
                            player.sendMessage(Component.text(w.getOwnerName() + " adasına ışınlandınız!").color(NamedTextColor.GREEN));
                            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.2f);
                            return;
                        }
                    }
                }
            }

        } else if (title.equals("Doğrulama (Makro Koruması)")) {
            event.setCancelled(true);
            if (!event.getClickedInventory().equals(event.getView().getTopInventory())) return;

            Player player = (Player) event.getWhoClicked();
            plugin.getAntiMacroManager().handleCaptchaClick(player, event.getSlot());
        }
    }

    private boolean tryPay(Player player, UUID islandId, double cost) {
        // Önce bankadan dene, yetmezse oyuncudan çek
        double bankBal = plugin.getBankManager().getBalance(islandId);
        if (bankBal >= cost) {
            plugin.getBankManager().setBalance(islandId, bankBal - cost);
            plugin.getBankManager().logTransaction(islandId, "Yükseltme için -" + (long)cost + " Para çekildi.");
            return true;
        }

        if (plugin.getEconomyManager().isAvailable() && plugin.getEconomyManager().hasMoney(player, cost)) {
            return plugin.getEconomyManager().takeMoney(player, cost);
        }

        player.sendMessage(Component.text("Yetersiz bakiye! Ne bankada ne cüzdanınızda yeterli para var (Gereken: " + (long)cost + ").").color(NamedTextColor.RED));
        return false;
    }
}
