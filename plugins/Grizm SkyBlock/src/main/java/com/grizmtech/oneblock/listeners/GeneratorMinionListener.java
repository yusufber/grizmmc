package com.grizmtech.oneblock.listeners;

import com.grizmtech.oneblock.GrizmOneBlock;
import com.grizmtech.oneblock.managers.GeneratorMinionManager;
import com.grizmtech.oneblock.models.GeneratorMinion;
import com.grizmtech.oneblock.models.IslandRole;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class GeneratorMinionListener implements Listener {

    private final GrizmOneBlock plugin;
    private final GeneratorMinionManager minionManager;

    public GeneratorMinionListener(GrizmOneBlock plugin, GeneratorMinionManager minionManager) {
        this.plugin = plugin;
        this.minionManager = minionManager;
    }

    /**
     * Minyonu Yere Yerleştirme
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!minionManager.isMinionItem(item)) return;

        event.setCancelled(true);

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        String worldName = clickedBlock.getWorld().getName();
        if (!worldName.startsWith("island_")) {
            player.sendMessage(Component.text("Jeneratör Minyonu sadece ada dünyalarında kullanılabilir!").color(NamedTextColor.RED));
            return;
        }

        UUID islandOwnerUUID;
        try {
            islandOwnerUUID = UUID.fromString(worldName.replace("island_", ""));
        } catch (Exception e) {
            return;
        }

        // Yetki kontrolü (Ada sahibi, üyesi veya coop mu?)
        if (plugin.getRoleManager().getRole(islandOwnerUUID, player.getUniqueId()) == IslandRole.VISITOR && !player.isOp()) {
            player.sendMessage(Component.text("Bu adaya minyon yerleştirme yetkiniz yok!").color(NamedTextColor.RED));
            return;
        }

        // Ada başına minyon limiti (Maksimum 5 minyon)
        List<GeneratorMinion> islandMinions = minionManager.getMinionsByOwner(islandOwnerUUID);
        if (islandMinions.size() >= 5 && !player.isOp()) {
            player.sendMessage(Component.text("Adanızda maksimum minyon sınırına ulaştınız (5/5)!").color(NamedTextColor.RED));
            return;
        }

        // Minyonun koyulacağı yer: Tıklanan bloğun üstü
        Location placeLoc = clickedBlock.getLocation().add(0, 1, 0);
        if (placeLoc.getBlock().getType() != Material.AIR) {
            player.sendMessage(Component.text("Minyon yerleştirmek için bloğun üstünün boş olması gerekir!").color(NamedTextColor.RED));
            return;
        }

        // Minyon oyuncunun baktığı yöne doğru baksın
        BlockFace facing = player.getFacing();

        int level = minionManager.getMinionItemLevel(item);
        GeneratorMinion minion = minionManager.spawnMinion(islandOwnerUUID, placeLoc, facing, level);

        if (minion != null) {
            // Eşyayı envanterden 1 azalt
            item.setAmount(item.getAmount() - 1);
            player.sendMessage(Component.text("✔ Jeneratör Minyonu başarıyla kuruldu!").color(NamedTextColor.GREEN));
            player.sendMessage(Component.text("▸ Önündeki veya altındaki su-lav jeneratörünü kıracaktır.").color(NamedTextColor.YELLOW));
            player.sendMessage(Component.text("▸ Minyona sağ tıklayarak menüsünü açabilirsiniz.").color(NamedTextColor.GRAY));
        }
    }

    /**
     * Minyona Sağ Tıklama (GUI Açma)
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityInteract(PlayerInteractAtEntityEvent event) {
        if (!(event.getRightClicked() instanceof ArmorStand stand)) return;

        GeneratorMinion minion = minionManager.getMinionByEntity(stand);
        if (minion == null) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        // Yetki kontrolü: Yabancılar başkasının minyonunu açamaz
        UUID ownerUUID = minion.getOwnerUUID();
        if (plugin.getRoleManager().getRole(ownerUUID, player.getUniqueId()) == IslandRole.VISITOR && !player.isOp()) {
            player.sendMessage(Component.text("Bu minyona erişim yetkiniz yok!").color(NamedTextColor.RED));
            return;
        }

        minionManager.openMinionMenu(player, minion);
    }

    /**
     * Zırh Askısı Ekipmanını Çalmayı Engelle
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        if (minionManager.getMinionByEntity(event.getRightClicked()) != null) {
            event.setCancelled(true);
        }
    }

    /**
     * Minyonun Hasar Almasını / Kırılmasını Engelle
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (minionManager.getMinionByEntity(event.getEntity()) != null) {
            event.setCancelled(true);
        }
    }

    /**
     * Minyon Menüsü Buton Tıklamaları
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.startsWith("Jeneratör Minyonu")) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        int slot = event.getRawSlot();

        // Oyuncunun açık olan menüsündeki minyonu bulalım
        GeneratorMinion targetMinion = minionManager.getPlayerOpenMinion(player.getUniqueId());

        if (targetMinion == null) {
            Location playerLoc = player.getLocation();
            double minDistance = Double.MAX_VALUE;

            for (GeneratorMinion m : minionManager.getAllMinions()) {
                if (m.getLocation() != null && m.getLocation().getWorld() != null && m.getLocation().getWorld().equals(playerLoc.getWorld())) {
                    double dist = m.getLocation().distanceSquared(playerLoc);
                    if (dist < minDistance && dist <= 64) {
                        minDistance = dist;
                        targetMinion = m;
                    }
                }
            }
        }

        if (targetMinion == null) {
            player.closeInventory();
            return;
        }

        // Slot 29: Tüm Madenleri Topla
        if (slot == 29) {
            if (targetMinion.getStorage().isEmpty()) {
                player.sendMessage(Component.text("Minyonun haznesinde toplanacak maden yok!").color(NamedTextColor.RED));
                return;
            }

            for (ItemStack is : targetMinion.getStorage()) {
                if (is != null && is.getType() != Material.AIR) {
                    HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(is);
                    for (ItemStack left : leftover.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), left);
                    }
                }
            }
            targetMinion.getStorage().clear();
            minionManager.saveMinions();

            player.sendMessage(Component.text("✔ Minyondaki tüm madenler envanterinize aktarıldı.").color(NamedTextColor.GREEN));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.8f, 1.3f);
            minionManager.openMinionMenu(player, targetMinion);
        }

        // Slot 31: Seviye Yükselt
        else if (slot == 31) {
            if (minionManager.upgradeMinion(player, targetMinion)) {
                minionManager.openMinionMenu(player, targetMinion);
            }
        }

        // Slot 33: Minyonu Geri Al
        else if (slot == 33) {
            player.closeInventory();
            minionManager.pickupMinion(player, targetMinion);
        }
    }

    @EventHandler
    public void onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        minionManager.setPlayerOpenMinion(event.getPlayer().getUniqueId(), null);
    }
}
