package com.grizmtech.oneblock.managers;

import com.grizmtech.oneblock.GrizmOneBlock;
import com.grizmtech.oneblock.models.GeneratorMinion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class GeneratorMinionManager {

    private final GrizmOneBlock plugin;
    private final Map<UUID, GeneratorMinion> minions = new HashMap<>();
    private final Map<UUID, UUID> playerOpenMinions = new java.util.concurrent.ConcurrentHashMap<>();
    private final NamespacedKey minionKey;
    private final File dataFile;
    private FileConfiguration dataConfig;

    public void setPlayerOpenMinion(UUID playerUUID, UUID minionId) {
        if (minionId == null) {
            playerOpenMinions.remove(playerUUID);
        } else {
            playerOpenMinions.put(playerUUID, minionId);
        }
    }

    public GeneratorMinion getPlayerOpenMinion(UUID playerUUID) {
        UUID minionId = playerOpenMinions.get(playerUUID);
        if (minionId == null)
            return null;
        return minions.get(minionId);
    }

    public GeneratorMinionManager(GrizmOneBlock plugin) {
        this.plugin = plugin;
        this.minionKey = new NamespacedKey(plugin, "grizm_generator_minion_level");
        this.dataFile = new File(plugin.getDataFolder(), "generator_minions.yml");
        loadMinions();
        startMinionTask();
    }

    public NamespacedKey getMinionKey() {
        return minionKey;
    }

    public Collection<GeneratorMinion> getAllMinions() {
        return minions.values();
    }

    public GeneratorMinion getMinionByEntity(Entity entity) {
        if (!(entity instanceof ArmorStand))
            return null;
        for (GeneratorMinion minion : minions.values()) {
            if (entity.getUniqueId().equals(minion.getArmorStandUUID()) ||
                    entity.getUniqueId().equals(minion.getHologramUUID())) {
                return minion;
            }
        }
        return null;
    }

    public GeneratorMinion getMinionById(UUID id) {
        return minions.get(id);
    }

    public List<GeneratorMinion> getMinionsByOwner(UUID ownerUUID) {
        List<GeneratorMinion> list = new ArrayList<>();
        for (GeneratorMinion m : minions.values()) {
            if (m.getOwnerUUID().equals(ownerUUID)) {
                list.add(m);
            }
        }
        return list;
    }

    /**
     * Oyuncunun eline verilecek Jeneratör Minyonu Eşyası
     */
    public ItemStack createMinionItem(int level) {
        level = Math.max(1, Math.min(5, level));
        ItemStack item = new ItemStack(Material.ARMOR_STAND);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Jeneratör Minyonu ")
                    .color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD)
                    .append(Component.text("[Lv." + level + "]")
                            .color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, false)));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Önündeki veya altındaki su-lav").color(NamedTextColor.GRAY));
            lore.add(Component.text("jeneratörlerini otomatik olarak kırar,").color(NamedTextColor.GRAY));
            lore.add(Component.text("madenleri dahili haznesinde biriktirir.").color(NamedTextColor.GRAY));
            lore.add(Component.empty());
            lore.add(Component.text("▸ Kırma Hızı: ").color(NamedTextColor.YELLOW)
                    .append(Component.text("Her " + Math.max(1, 6 - (level - 1)) + " saniyede bir")
                            .color(NamedTextColor.WHITE)));
            lore.add(Component.text("▸ Depo Kapasitesi: ").color(NamedTextColor.YELLOW)
                    .append(Component.text((6 + level * 3) + " Eşya Slotu").color(NamedTextColor.WHITE)));
            lore.add(Component.text("▸ Sandık Entegre: ").color(NamedTextColor.YELLOW)
                    .append(Component.text("Arkasındaki sandığa otomatik aktarır").color(NamedTextColor.GREEN)));
            lore.add(Component.empty());
            lore.add(Component.text("▸ Jeneratörün yanına sağ tıklayarak yerleştirin.").color(NamedTextColor.GOLD));

            meta.lore(lore);
            meta.getPersistentDataContainer().set(minionKey, PersistentDataType.INTEGER, level);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isMinionItem(ItemStack item) {
        if (item == null || item.getType() != Material.ARMOR_STAND)
            return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return false;
        return meta.getPersistentDataContainer().has(minionKey, PersistentDataType.INTEGER);
    }

    public int getMinionItemLevel(ItemStack item) {
        if (!isMinionItem(item))
            return 1;
        Integer lvl = item.getItemMeta().getPersistentDataContainer().get(minionKey, PersistentDataType.INTEGER);
        return lvl != null ? lvl : 1;
    }

    /**
     * Minyonu dünyada canlandır
     */
    public GeneratorMinion spawnMinion(UUID ownerUUID, Location placeLoc, BlockFace facing, int level) {
        World world = placeLoc.getWorld();
        if (world == null)
            return null;

        // Minyonun duracağı merkez koordinat
        Location standLoc = placeLoc.clone();
        standLoc.setX(Math.floor(standLoc.getX()) + 0.5);
        standLoc.setZ(Math.floor(standLoc.getZ()) + 0.5);

        // Baktığı yöne doğru Yaw açısı
        float yaw = switch (facing) {
            case NORTH -> 180f;
            case SOUTH -> 0f;
            case WEST -> 90f;
            case EAST -> 270f;
            default -> 0f;
        };
        standLoc.setYaw(yaw);
        standLoc.setPitch(0f);

        // 1. Ana Zırh Askısı
        ArmorStand stand = (ArmorStand) world.spawnEntity(standLoc, EntityType.ARMOR_STAND);
        stand.setSmall(true);
        stand.setArms(true);
        stand.setBasePlate(false);
        stand.setGravity(false);
        stand.setInvulnerable(true);
        stand.setCanPickupItems(false);
        stand.setCustomNameVisible(false);
        stand.setPersistent(true);
        stand.setRemoveWhenFarAway(false);
        stand.getPersistentDataContainer().set(new NamespacedKey(plugin, "is_grizm_minion"), PersistentDataType.BYTE,
                (byte) 1);
        stand.setRightArmPose(new EulerAngle(Math.toRadians(310), 0, 0));

        // Ekipmanlar
        ItemStack helmet = new ItemStack(Material.GOLDEN_HELMET);
        ItemStack chest = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta cm = (LeatherArmorMeta) chest.getItemMeta();
        if (cm != null) {
            cm.setColor(Color.fromRGB(240, 160, 40)); // Madenci turuncusu
            chest.setItemMeta(cm);
        }

        ItemStack tool = switch (level) {
            case 1 -> new ItemStack(Material.STONE_PICKAXE);
            case 2 -> new ItemStack(Material.IRON_PICKAXE);
            case 3 -> new ItemStack(Material.GOLDEN_PICKAXE);
            case 4, 5 -> new ItemStack(Material.DIAMOND_PICKAXE);
            default -> new ItemStack(Material.STONE_PICKAXE);
        };

        stand.getEquipment().setHelmet(helmet);
        stand.getEquipment().setChestplate(chest);
        stand.getEquipment().setItemInMainHand(tool);

        // 2. Üst Hologram
        Location holoLoc = standLoc.clone().add(0, 1.15, 0);
        ArmorStand holo = (ArmorStand) world.spawnEntity(holoLoc, EntityType.ARMOR_STAND);
        holo.setSmall(true);
        holo.setVisible(false);
        holo.setGravity(false);
        holo.setInvulnerable(true);
        holo.setMarker(true);
        holo.setCustomNameVisible(true);
        holo.setPersistent(true);
        holo.setRemoveWhenFarAway(false);
        holo.getPersistentDataContainer().set(new NamespacedKey(plugin, "is_grizm_minion"), PersistentDataType.BYTE,
                (byte) 1);
        updateHologramText(holo, level, 0, 6 + level * 3);

        GeneratorMinion minion = new GeneratorMinion(UUID.randomUUID(), ownerUUID, standLoc, facing, level);
        minion.setArmorStandUUID(stand.getUniqueId());
        minion.setHologramUUID(holo.getUniqueId());

        minions.put(minion.getId(), minion);
        saveMinions();

        // Parçacık ve ses efekti
        world.spawnParticle(Particle.HAPPY_VILLAGER, standLoc.clone().add(0, 0.8, 0), 10, 0.3, 0.3, 0.3);
        world.playSound(standLoc, Sound.BLOCK_ANVIL_USE, 0.6f, 1.4f);

        return minion;
    }

    private void updateHologramText(ArmorStand holo, int level, int storedCount, int maxSlots) {
        if (holo == null || !holo.isValid())
            return;
        holo.customName(Component.text("⛏ Jeneratör Minyonu ")
                .color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD)
                .append(Component.text("[Lv." + level + "]")
                        .color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, false))
                .append(Component.text(" §7(" + storedCount + "/" + maxSlots + ")")));
    }

    /**
     * Minyonu dünyadan kaldır ve eşyasını oyuncuya ver
     */
    public boolean pickupMinion(Player player, GeneratorMinion minion) {
        if (minion == null)
            return false;

        World world = minion.getLocation().getWorld();
        if (world != null) {
            // Depodaki eşyaları oyuncuya ver veya yere at
            for (ItemStack is : minion.getStorage()) {
                if (is != null && is.getType() != Material.AIR) {
                    HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(is);
                    for (ItemStack left : leftover.values()) {
                        world.dropItemNaturally(minion.getLocation(), left);
                    }
                }
            }

            // Entity'leri sil
            Entity stand = Bukkit.getEntity(minion.getArmorStandUUID());
            if (stand != null)
                stand.remove();

            Entity holo = Bukkit.getEntity(minion.getHologramUUID());
            if (holo != null)
                holo.remove();

            world.spawnParticle(Particle.POOF, minion.getLocation().add(0, 0.5, 0), 8, 0.2, 0.2, 0.2, 0.05);
            world.playSound(minion.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.8f, 1.2f);
        }

        minions.remove(minion.getId());
        saveMinions();

        // Minyon eşyasını oyuncuya iade et
        ItemStack minionItem = createMinionItem(minion.getLevel());
        HashMap<Integer, ItemStack> rem = player.getInventory().addItem(minionItem);
        for (ItemStack left : rem.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), left);
        }

        player.sendMessage(Component.text("✔ Jeneratör Minyonu [Lv." + minion.getLevel() + "] başarıyla geri alındı.")
                .color(NamedTextColor.GREEN));
        return true;
    }

    /**
     * Minyon Seviyesini Yükselt
     */
    public boolean upgradeMinion(Player player, GeneratorMinion minion) {
        if (minion.getLevel() >= 5) {
            player.sendMessage(Component.text("Minyon zaten maksimum seviyede (Lv.5)!").color(NamedTextColor.RED));
            return false;
        }

        double cost = minion.getUpgradeCost();
        if (!plugin.getEconomyManager().isAvailable() || !plugin.getEconomyManager().hasMoney(player, cost)) {
            player.sendMessage(Component.text("Yetersiz bakiye! Yükseltme ücreti: " + (long) cost + " Para")
                    .color(NamedTextColor.RED));
            return false;
        }

        plugin.getEconomyManager().takeMoney(player, cost);
        int newLevel = minion.getLevel() + 1;
        minion.setLevel(newLevel);

        // Armorstand aletini güncelle
        Entity entity = Bukkit.getEntity(minion.getArmorStandUUID());
        if (entity instanceof ArmorStand stand) {
            ItemStack tool = switch (newLevel) {
                case 2 -> new ItemStack(Material.IRON_PICKAXE);
                case 3 -> new ItemStack(Material.GOLDEN_PICKAXE);
                case 4, 5 -> new ItemStack(Material.DIAMOND_PICKAXE);
                default -> new ItemStack(Material.STONE_PICKAXE);
            };
            stand.getEquipment().setItemInMainHand(tool);
        }

        // Hologramı güncelle
        Entity holoEntity = Bukkit.getEntity(minion.getHologramUUID());
        if (holoEntity instanceof ArmorStand holo) {
            updateHologramText(holo, newLevel, minion.getStorage().size(), minion.getMaxStorageSlots());
        }

        saveMinions();

        player.sendMessage(
                Component.text("★ Jeneratör Minyonu Seviye " + newLevel + " oldu!").color(NamedTextColor.GREEN));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
        return true;
    }

    /**
     * Minyon Yönetim Menüsü
     */
    public void openMinionMenu(Player player, GeneratorMinion minion) {
        setPlayerOpenMinion(player.getUniqueId(), minion.getId());
        Inventory gui = Bukkit.createInventory(null, 36,
                Component.text("Jeneratör Minyonu [Lv." + minion.getLevel() + "]"));

        // Kenar camları
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta gm = glass.getItemMeta();
        if (gm != null) {
            gm.displayName(Component.empty());
            glass.setItemMeta(gm);
        }
        for (int i = 0; i < 9; i++)
            gui.setItem(i, glass);
        for (int i = 27; i < 36; i++)
            gui.setItem(i, glass);

        // Bilgi Kartı (Slot 4)
        ItemStack info = new ItemStack(Material.HOPPER);
        ItemMeta im = info.getItemMeta();
        if (im != null) {
            im.displayName(Component.text("Minyon Durumu").color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("• Seviye: ").color(NamedTextColor.GRAY)
                    .append(Component.text("Lv." + minion.getLevel()).color(NamedTextColor.YELLOW)));
            lore.add(Component.text("• Kırma Hızı: ").color(NamedTextColor.GRAY)
                    .append(Component.text("Her " + minion.getBreakIntervalSeconds() + " saniyede bir")
                            .color(NamedTextColor.WHITE)));
            lore.add(Component.text("• Doluluk: ").color(NamedTextColor.GRAY)
                    .append(Component.text(minion.getStorage().size() + " / " + minion.getMaxStorageSlots() + " Slot")
                            .color(NamedTextColor.WHITE)));
            lore.add(Component.text("• Hedef Yönü: ").color(NamedTextColor.GRAY)
                    .append(Component.text(minion.getTargetFace().name()).color(NamedTextColor.WHITE)));
            im.lore(lore);
            info.setItemMeta(im);
        }
        gui.setItem(4, info);

        // Depolanan Madenler (Slot 9 ile 26 arası)
        int slot = 9;
        int maxSlots = minion.getMaxStorageSlots();
        for (int i = 0; i < maxSlots && slot < 27; i++) {
            if (i < minion.getStorage().size()) {
                gui.setItem(slot, minion.getStorage().get(i));
            } else {
                ItemStack emptySlot = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
                ItemMeta em = emptySlot.getItemMeta();
                if (em != null) {
                    em.displayName(Component.text("Boş Depo Yuvası").color(NamedTextColor.DARK_GRAY));
                    emptySlot.setItemMeta(em);
                }
                gui.setItem(slot, emptySlot);
            }
            slot++;
        }

        // Slot 29: Tüm Madenleri Topla
        ItemStack collectBtn = new ItemStack(Material.CHEST_MINECART);
        ItemMeta cm = collectBtn.getItemMeta();
        if (cm != null) {
            cm.displayName(
                    Component.text("Tüm Madenleri Topla").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD));
            List<Component> clore = new ArrayList<>();
            clore.add(Component.text("Minyonun haznesindeki tüm madenleri").color(NamedTextColor.GRAY));
            clore.add(Component.text("envanterinize aktarır.").color(NamedTextColor.GRAY));
            cm.lore(clore);
            collectBtn.setItemMeta(cm);
        }
        gui.setItem(29, collectBtn);

        // Slot 31: Seviye Yükseltme
        ItemStack upgradeBtn;
        if (minion.getLevel() < 5) {
            upgradeBtn = new ItemStack(Material.ANVIL);
            ItemMeta um = upgradeBtn.getItemMeta();
            if (um != null) {
                um.displayName(Component.text("Seviye Yükselt (Lv." + (minion.getLevel() + 1) + ")")
                        .color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
                List<Component> ulore = new ArrayList<>();
                ulore.add(Component.text("Kırma hızını ve depo kapasitesini arttırır.").color(NamedTextColor.GRAY));
                ulore.add(Component.text("Ücret: " + (long) minion.getUpgradeCost() + " Para")
                        .color(NamedTextColor.YELLOW));
                um.lore(ulore);
                upgradeBtn.setItemMeta(um);
            }
        } else {
            upgradeBtn = new ItemStack(Material.BARRIER);
            ItemMeta um = upgradeBtn.getItemMeta();
            if (um != null) {
                um.displayName(
                        Component.text("Maksimum Seviye!").color(NamedTextColor.RED).decorate(TextDecoration.BOLD));
                upgradeBtn.setItemMeta(um);
            }
        }
        gui.setItem(31, upgradeBtn);

        // Slot 33: Minyonu Geri Al
        ItemStack pickupBtn = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta pm = pickupBtn.getItemMeta();
        if (pm != null) {
            pm.displayName(
                    Component.text("Minyonu Kaldır ve Al").color(NamedTextColor.RED).decorate(TextDecoration.BOLD));
            List<Component> plore = new ArrayList<>();
            plore.add(Component.text("Minyonu kırıp eşya olarak").color(NamedTextColor.GRAY));
            plore.add(Component.text("envanterinize iade eder.").color(NamedTextColor.GRAY));
            pm.lore(plore);
            pickupBtn.setItemMeta(pm);
        }
        gui.setItem(33, pickupBtn);

        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.BLOCK_BARREL_OPEN, 0.7f, 1.2f);
    }

    /**
     * Minyonun dünyadaki görsel varlığını doğrula ve kaybolmuşsa hemen yeniden
     * canlandır (Respawn Failsafe)
     */
    public void ensureEntityExists(GeneratorMinion minion) {
        Location loc = minion.getLocation();
        if (loc == null || loc.getWorld() == null)
            return;
        World world = loc.getWorld();

        if (!world.isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4))
            return;

        Entity standEntity = minion.getArmorStandUUID() != null ? Bukkit.getEntity(minion.getArmorStandUUID()) : null;
        if (standEntity == null || !standEntity.isValid() || standEntity.isDead()) {
            respawnMinionEntity(minion);
            return;
        }

        Entity holoEntity = minion.getHologramUUID() != null ? Bukkit.getEntity(minion.getHologramUUID()) : null;
        if (holoEntity == null || !holoEntity.isValid() || holoEntity.isDead()) {
            Location holoLoc = loc.clone().add(0, 1.15, 0);
            ArmorStand holo = (ArmorStand) world.spawnEntity(holoLoc, EntityType.ARMOR_STAND);
            holo.setSmall(true);
            holo.setVisible(false);
            holo.setGravity(false);
            holo.setInvulnerable(true);
            holo.setMarker(true);
            holo.setCustomNameVisible(true);
            holo.setPersistent(true);
            holo.setRemoveWhenFarAway(false);
            holo.getPersistentDataContainer().set(new NamespacedKey(plugin, "is_grizm_minion"), PersistentDataType.BYTE,
                    (byte) 1);
            minion.setHologramUUID(holo.getUniqueId());
            updateHologramText(holo, minion.getLevel(), minion.getStorage().size(), minion.getMaxStorageSlots());
            saveMinions();
        }
    }

    public void respawnMinionEntity(GeneratorMinion minion) {
        Location standLoc = minion.getLocation();
        if (standLoc == null || standLoc.getWorld() == null)
            return;
        World world = standLoc.getWorld();

        if (minion.getArmorStandUUID() != null) {
            Entity oldStand = Bukkit.getEntity(minion.getArmorStandUUID());
            if (oldStand != null)
                oldStand.remove();
        }
        if (minion.getHologramUUID() != null) {
            Entity oldHolo = Bukkit.getEntity(minion.getHologramUUID());
            if (oldHolo != null)
                oldHolo.remove();
        }

        float yaw = switch (minion.getTargetFace()) {
            case NORTH -> 180f;
            case SOUTH -> 0f;
            case WEST -> 90f;
            case EAST -> 270f;
            default -> 0f;
        };
        standLoc.setYaw(yaw);
        standLoc.setPitch(0f);

        ArmorStand stand = (ArmorStand) world.spawnEntity(standLoc, EntityType.ARMOR_STAND);
        stand.setSmall(true);
        stand.setArms(true);
        stand.setBasePlate(false);
        stand.setGravity(false);
        stand.setInvulnerable(true);
        stand.setCanPickupItems(false);
        stand.setCustomNameVisible(false);
        stand.setPersistent(true);
        stand.setRemoveWhenFarAway(false);
        stand.getPersistentDataContainer().set(new NamespacedKey(plugin, "is_grizm_minion"), PersistentDataType.BYTE,
                (byte) 1);
        stand.setRightArmPose(new EulerAngle(Math.toRadians(310), 0, 0));

        ItemStack helmet = new ItemStack(Material.GOLDEN_HELMET);
        ItemStack chest = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta cm = (LeatherArmorMeta) chest.getItemMeta();
        if (cm != null) {
            cm.setColor(Color.fromRGB(240, 160, 40));
            chest.setItemMeta(cm);
        }

        ItemStack tool = switch (minion.getLevel()) {
            case 1 -> new ItemStack(Material.STONE_PICKAXE);
            case 2 -> new ItemStack(Material.IRON_PICKAXE);
            case 3 -> new ItemStack(Material.GOLDEN_PICKAXE);
            case 4, 5 -> new ItemStack(Material.DIAMOND_PICKAXE);
            default -> new ItemStack(Material.STONE_PICKAXE);
        };

        stand.getEquipment().setHelmet(helmet);
        stand.getEquipment().setChestplate(chest);
        stand.getEquipment().setItemInMainHand(tool);

        Location holoLoc = standLoc.clone().add(0, 1.15, 0);
        ArmorStand holo = (ArmorStand) world.spawnEntity(holoLoc, EntityType.ARMOR_STAND);
        holo.setSmall(true);
        holo.setVisible(false);
        holo.setGravity(false);
        holo.setInvulnerable(true);
        holo.setMarker(true);
        holo.setCustomNameVisible(true);
        holo.setPersistent(true);
        holo.setRemoveWhenFarAway(false);
        holo.getPersistentDataContainer().set(new NamespacedKey(plugin, "is_grizm_minion"), PersistentDataType.BYTE,
                (byte) 1);
        updateHologramText(holo, minion.getLevel(), minion.getStorage().size(), minion.getMaxStorageSlots());

        minion.setArmorStandUUID(stand.getUniqueId());
        minion.setHologramUUID(holo.getUniqueId());
        saveMinions();
    }

    /**
     * Ana Kırma ve Çalışma Zamanlayıcısı (Tick Loop)
     */
    private void startMinionTask() {
        new BukkitRunnable() {
            private int secondCounter = 0;

            @Override
            public void run() {
                secondCounter++;
                if (minions.isEmpty())
                    return;

                List<GeneratorMinion> minionList = new ArrayList<>(minions.values());
                for (GeneratorMinion minion : minionList) {
                    try {
                        Location loc = minion.getLocation();
                        World world = loc.getWorld();
                        if (world == null)
                            continue;

                        // TPS Koruması: Adada hiç oyuncu yoksa boşuna CPU harcama
                        if (world.getPlayers().isEmpty())
                            continue;

                        // Görsel varlık kontrolü: Stackleme veya despawn ile silinmişse hemen yeniden
                        // oluştur
                        ensureEntityExists(minion);

                        int interval = minion.getBreakIntervalSeconds();
                        if (secondCounter % interval != 0)
                            continue;

                        performMinionBreak(minion);
                    } catch (Exception ignored) {
                    }
                }
            }
        }.runTaskTimer(plugin, 40L, 20L); // 1 saniyede bir kontrol
    }

    /**
     * SADECE SU-LAV JENERATÖR BLOĞUNU KIRMA MANTIĞI
     */
    private void performMinionBreak(GeneratorMinion minion) {
        Location minionLoc = minion.getLocation();
        World world = minionLoc.getWorld();
        if (world == null)
            return;

        // 1. Minyonun baktığı önündeki bloğu bul
        Block targetBlock = minionLoc.getBlock().getRelative(minion.getTargetFace());

        // Eğer önündeki blok değilse altındaki bloğa da bakabilir
        if (!isGeneratorBlock(targetBlock.getType())) {
            Block downBlock = minionLoc.getBlock().getRelative(BlockFace.DOWN);
            if (isGeneratorBlock(downBlock.getType())) {
                targetBlock = downBlock;
            } else {
                return; // Jeneratör bloğu yok, bekle
            }
        }

        // KESİNLİKLE ANA TEKBLOK BLOĞUNU KIRMAZ!
        Location oneBlockLoc = plugin.getDataManager().getOneBlockLocation(minion.getOwnerUUID());
        if (oneBlockLoc != null && oneBlockLoc.getWorld() != null) {
            if (oneBlockLoc.getWorld().equals(targetBlock.getWorld()) &&
                    oneBlockLoc.getBlockX() == targetBlock.getX() &&
                    oneBlockLoc.getBlockY() == targetBlock.getY() &&
                    oneBlockLoc.getBlockZ() == targetBlock.getZ()) {
                // Bu ana TekBlok bloğudur, jeneratör minyonu buna DOKUNMAZ!
                return;
            }
        }

        Material currentType = targetBlock.getType();
        if (currentType == Material.AIR || currentType == Material.BEDROCK || currentType == Material.WATER
                || currentType == Material.LAVA) {
            return;
        }

        // 2. Kırma Animasyonu (Kol sallama)
        Entity entity = Bukkit.getEntity(minion.getArmorStandUUID());
        if (entity instanceof ArmorStand stand && stand.isValid()) {
            stand.setRightArmPose(new EulerAngle(Math.toRadians(350), 0, 0));
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (stand.isValid()) {
                        stand.setRightArmPose(new EulerAngle(Math.toRadians(310), 0, 0));
                    }
                }
            }.runTaskLater(plugin, 4L);
        }

        // 3. Çıkan eşyayı hazırla
        ItemStack dropItem = getDropForMaterial(currentType);

        // 4. Arkasındaki veya altındaki sandığa aktar
        boolean storedInChest = false;
        BlockFace oppositeFace = minion.getTargetFace().getOppositeFace();
        for (BlockFace face : new BlockFace[] { oppositeFace, BlockFace.DOWN }) {
            Block chestBlock = minionLoc.getBlock().getRelative(face);
            if (chestBlock.getState() instanceof Container container) {
                if (container.getInventory().addItem(dropItem).isEmpty()) {
                    storedInChest = true;
                    break;
                }
            }
        }

        // Eğer sandık yoksa minyonun kendi dahili deposuna ekle
        if (!storedInChest) {
            if (minion.getStorage().size() < minion.getMaxStorageSlots()) {
                addItemToMinionStorage(minion, dropItem);
            } else {
                // Depo da dolu, bloğu kırmaz (bekler)
                return;
            }
        }

        // 5. Ses ve Partikül
        Location breakLoc = targetBlock.getLocation().add(0.5, 0.5, 0.5);
        world.spawnParticle(Particle.BLOCK, breakLoc, 10, 0.2, 0.2, 0.2, currentType.createBlockData());
        world.playSound(breakLoc, Sound.BLOCK_STONE_BREAK, 0.5f, 1.1f);

        // 6. Bloğu kır (Hava yap)
        targetBlock.setType(Material.AIR);

        // 7. Hologramı güncelle
        Entity holoEntity = Bukkit.getEntity(minion.getHologramUUID());
        if (holoEntity instanceof ArmorStand holo && holo.isValid()) {
            updateHologramText(holo, minion.getLevel(), minion.getStorage().size(), minion.getMaxStorageSlots());
        }
    }

    private void addItemToMinionStorage(GeneratorMinion minion, ItemStack item) {
        for (ItemStack st : minion.getStorage()) {
            if (st.isSimilar(item) && st.getAmount() < st.getMaxStackSize()) {
                int add = Math.min(item.getAmount(), st.getMaxStackSize() - st.getAmount());
                st.setAmount(st.getAmount() + add);
                item.setAmount(item.getAmount() - add);
                if (item.getAmount() <= 0)
                    return;
            }
        }
        if (item.getAmount() > 0 && minion.getStorage().size() < minion.getMaxStorageSlots()) {
            minion.getStorage().add(item);
        }
    }

    /**
     * Su ve lavın oluşturduğu jeneratör blokları kontrolü
     */
    public boolean isGeneratorBlock(Material m) {
        return m == Material.COBBLESTONE || m == Material.STONE || m == Material.BASALT ||
                m == Material.ANDESITE || m == Material.DIORITE || m == Material.GRANITE ||
                m == Material.COAL_ORE || m == Material.DEEPSLATE_COAL_ORE ||
                m == Material.IRON_ORE || m == Material.DEEPSLATE_IRON_ORE ||
                m == Material.COPPER_ORE || m == Material.DEEPSLATE_COPPER_ORE ||
                m == Material.GOLD_ORE || m == Material.DEEPSLATE_GOLD_ORE ||
                m == Material.REDSTONE_ORE || m == Material.DEEPSLATE_REDSTONE_ORE ||
                m == Material.LAPIS_ORE || m == Material.DEEPSLATE_LAPIS_ORE ||
                m == Material.DIAMOND_ORE || m == Material.DEEPSLATE_DIAMOND_ORE ||
                m == Material.EMERALD_ORE || m == Material.DEEPSLATE_EMERALD_ORE ||
                m == Material.ANCIENT_DEBRIS || m == Material.NETHERRACK || m == Material.OBSIDIAN;
    }

    private ItemStack getDropForMaterial(Material m) {
        Material dropMat = switch (m) {
            case COAL_ORE, DEEPSLATE_COAL_ORE -> Material.COAL;
            case IRON_ORE, DEEPSLATE_IRON_ORE -> Material.RAW_IRON;
            case COPPER_ORE, DEEPSLATE_COPPER_ORE -> Material.RAW_COPPER;
            case GOLD_ORE, DEEPSLATE_GOLD_ORE -> Material.RAW_GOLD;
            case REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE -> Material.REDSTONE;
            case LAPIS_ORE, DEEPSLATE_LAPIS_ORE -> Material.LAPIS_LAZULI;
            case DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE -> Material.DIAMOND;
            case EMERALD_ORE, DEEPSLATE_EMERALD_ORE -> Material.EMERALD;
            case ANCIENT_DEBRIS -> Material.ANCIENT_DEBRIS;
            default -> Material.COBBLESTONE;
        };
        return new ItemStack(dropMat, 1);
    }

    /**
     * YAML Dosyasına Kaydet ve Yükle
     */
    public void saveMinions() {
        if (dataConfig == null)
            dataConfig = new YamlConfiguration();
        dataConfig.set("minions", null);

        for (GeneratorMinion m : minions.values()) {
            if (m.getLocation() == null)
                continue;
            String path = "minions." + m.getId().toString();
            dataConfig.set(path + ".owner", m.getOwnerUUID().toString());
            dataConfig.set(path + ".level", m.getLevel());
            dataConfig.set(path + ".facing", m.getTargetFace().name());
            if (m.getLocation().getWorld() != null) {
                dataConfig.set(path + ".world", m.getLocation().getWorld().getName());
            }
            dataConfig.set(path + ".x", m.getLocation().getX());
            dataConfig.set(path + ".y", m.getLocation().getY());
            dataConfig.set(path + ".z", m.getLocation().getZ());
            dataConfig.set(path + ".yaw", m.getLocation().getYaw());
            dataConfig.set(path + ".pitch", m.getLocation().getPitch());
            dataConfig.set(path + ".armorStandUUID",
                    m.getArmorStandUUID() != null ? m.getArmorStandUUID().toString() : null);
            dataConfig.set(path + ".hologramUUID", m.getHologramUUID() != null ? m.getHologramUUID().toString() : null);
            dataConfig.set(path + ".storage", m.getStorage());
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Jeneratör minyonları kaydedilirken hata oluştu: " + e.getMessage());
        }
    }

    public void loadMinions() {
        if (!dataFile.exists())
            return;
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection sec = dataConfig.getConfigurationSection("minions");
        if (sec == null)
            return;

        minions.clear();
        for (String idStr : sec.getKeys(false)) {
            try {
                UUID id = UUID.fromString(idStr);
                UUID ownerUUID = UUID.fromString(sec.getString(idStr + ".owner"));
                int level = sec.getInt(idStr + ".level", 1);
                BlockFace facing = BlockFace.valueOf(sec.getString(idStr + ".facing", "NORTH"));

                Location loc = null;
                String worldName = sec.getString(idStr + ".world");
                if (worldName != null) {
                    World w = Bukkit.getWorld(worldName);
                    if (w == null) {
                        w = Bukkit.createWorld(new org.bukkit.WorldCreator(worldName)
                                .generator(new com.grizmtech.oneblock.world.VoidChunkGenerator()));
                    }
                    if (w != null) {
                        double x = sec.getDouble(idStr + ".x");
                        double y = sec.getDouble(idStr + ".y");
                        double z = sec.getDouble(idStr + ".z");
                        float yaw = (float) sec.getDouble(idStr + ".yaw", 0.0);
                        float pitch = (float) sec.getDouble(idStr + ".pitch", 0.0);
                        loc = new Location(w, x, y, z, yaw, pitch);
                    }
                }

                if (loc == null) {
                    loc = sec.getLocation(idStr + ".location");
                }

                if (loc == null || loc.getWorld() == null) {
                    continue;
                }

                GeneratorMinion minion = new GeneratorMinion(id, ownerUUID, loc, facing, level);

                String standUUIDStr = sec.getString(idStr + ".armorStandUUID");
                if (standUUIDStr != null)
                    minion.setArmorStandUUID(UUID.fromString(standUUIDStr));

                String holoUUIDStr = sec.getString(idStr + ".hologramUUID");
                if (holoUUIDStr != null)
                    minion.setHologramUUID(UUID.fromString(holoUUIDStr));

                List<?> storageList = sec.getList(idStr + ".storage");
                if (storageList != null) {
                    for (Object obj : storageList) {
                        if (obj instanceof ItemStack is) {
                            minion.getStorage().add(is);
                        }
                    }
                }

                minions.put(minion.getId(), minion);
            } catch (Exception e) {
                plugin.getLogger().warning("Minyon yüklenirken hata: " + e.getMessage());
            }
        }
        plugin.getLogger().info("Toplam " + minions.size() + " adet Jeneratör Minyonu yüklendi.");
    }
}
