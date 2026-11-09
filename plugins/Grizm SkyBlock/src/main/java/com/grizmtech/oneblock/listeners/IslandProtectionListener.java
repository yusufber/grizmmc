package com.grizmtech.oneblock.listeners;

import com.grizmtech.oneblock.GrizmOneBlock;
import com.grizmtech.oneblock.models.IslandPermission;
import com.grizmtech.oneblock.models.IslandSetting;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.GlowItemFrame;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.UUID;

public class IslandProtectionListener implements Listener {

    private final GrizmOneBlock plugin;

    public IslandProtectionListener(GrizmOneBlock plugin) {
        this.plugin = plugin;
    }

    public UUID getIslandOwner(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        String worldName = loc.getWorld().getName();
        if (worldName.startsWith("island_")) {
            try {
                return UUID.fromString(worldName.replace("island_", ""));
            } catch (Exception ignored) {}
        }
        return null;
    }

    private boolean isOutsideIsland(Player player, Location loc) {
        if (player == null || loc == null || loc.getWorld() == null) return false;
        if (player.hasPermission("grizmoneblock.admin")) return false;

        UUID ownerUUID = getIslandOwner(loc);
        if (ownerUUID == null) return false;

        return !player.getUniqueId().equals(ownerUUID);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Location loc = event.getBlock().getLocation();
        if (isOutsideIsland(player, loc)) {
            UUID ownerUUID = getIslandOwner(loc);
            Location oneBlockLoc = plugin.getDataManager().getOneBlockLocation(ownerUUID);
            if (oneBlockLoc != null && loc.equals(oneBlockLoc)) {
                // Ana Blok kırma yetkisi BlockBreakListener içerisinde ayrıca kontrol edilecek
                return;
            }

            // Co-op veya Member blok kırma yetkisi var mı?
            if (plugin.getRoleManager().hasPermission(ownerUUID, player.getUniqueId(), IslandPermission.BREAK_BLOCK)) {
                return;
            }

            event.setCancelled(true);
            player.sendActionBar(Component.text("Burası senin adan değil!").color(NamedTextColor.RED));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Location loc = event.getBlock().getLocation();
        if (isOutsideIsland(player, loc)) {
            UUID ownerUUID = getIslandOwner(loc);
            if (plugin.getRoleManager().hasPermission(ownerUUID, player.getUniqueId(), IslandPermission.PLACE_BLOCK)) {
                return;
            }

            event.setCancelled(true);
            player.sendActionBar(Component.text("Burası senin adan değil!").color(NamedTextColor.RED));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        Player player = event.getPlayer();
        Location loc = event.getClickedBlock().getLocation();

        if (isOutsideIsland(player, loc)) {
            UUID ownerUUID = getIslandOwner(loc);
            if (ownerUUID == null) return;

            // 1. Konteyner / Depo Erişimi kontrolü
            if (plugin.getIslandSettingManager().isContainer(event.getClickedBlock().getType(), event.getClickedBlock().getState())) {
                boolean hasRolePerm = plugin.getRoleManager().hasPermission(ownerUUID, player.getUniqueId(), IslandPermission.OPEN_CONTAINER);
                boolean allowed = hasRolePerm || plugin.getIslandSettingManager().getBoolean(ownerUUID, IslandSetting.CONTAINER_ACCESS);
                if (!allowed) {
                    event.setCancelled(true);
                    player.sendActionBar(Component.text("Bu adada depo/sandık erişimi kapalıdır!").color(NamedTextColor.RED));
                }
                return;
            }

            // 2. Kızıltaş ve Mekanizma Kullanımı kontrolü
            if (plugin.getIslandSettingManager().isRedstoneInteractable(event.getClickedBlock().getType())) {
                boolean hasRolePerm = plugin.getRoleManager().hasPermission(ownerUUID, player.getUniqueId(), IslandPermission.USE_REDSTONE);
                boolean allowed = hasRolePerm || plugin.getIslandSettingManager().getBoolean(ownerUUID, IslandSetting.REDSTONE_USE);
                if (!allowed) {
                    event.setCancelled(true);
                    player.sendActionBar(Component.text("Bu adada mekanizma/kızıltaş kullanımı kapalıdır!").color(NamedTextColor.RED));
                }
                return;
            }

            // Diğer tüm blok etkileşimleri engellenir
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.PHYSICAL) {
                if (!plugin.getRoleManager().hasPermission(ownerUUID, player.getUniqueId(), IslandPermission.PLACE_BLOCK)) {
                    event.setCancelled(true);
                    player.sendActionBar(Component.text("Bu adada işlem yapamazsınız!").color(NamedTextColor.RED));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        Location invLoc = event.getInventory().getLocation();
        if (invLoc == null) return;

        if (isOutsideIsland(player, invLoc)) {
            UUID ownerUUID = getIslandOwner(invLoc);
            if (ownerUUID != null) {
                boolean hasRolePerm = plugin.getRoleManager().hasPermission(ownerUUID, player.getUniqueId(), IslandPermission.OPEN_CONTAINER);
                boolean allowed = hasRolePerm || plugin.getIslandSettingManager().getBoolean(ownerUUID, IslandSetting.CONTAINER_ACCESS);
                if (!allowed) {
                    event.setCancelled(true);
                    player.sendActionBar(Component.text("Bu adada depo/sandık erişimi kapalıdır!").color(NamedTextColor.RED));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onVillagerInteract(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();
        Location loc = entity.getLocation();

        if (isOutsideIsland(player, loc)) {
            UUID ownerUUID = getIslandOwner(loc);
            if (ownerUUID == null) return;

            // Köylü Ticareti
            if (entity instanceof Villager || entity instanceof WanderingTrader) {
                boolean tradeAllowed = plugin.getIslandSettingManager().getBoolean(ownerUUID, IslandSetting.VILLAGER_TRADE);
                if (!tradeAllowed) {
                    event.setCancelled(true);
                    player.sendActionBar(Component.text("Bu adada köylülerle ticaret yapamazsınız!").color(NamedTextColor.RED));
                    return;
                }
            }

            // Varlık (Armor Stand & Item Frame vb.)
            if (entity instanceof ArmorStand || entity instanceof ItemFrame || entity instanceof GlowItemFrame) {
                boolean interactAllowed = plugin.getIslandSettingManager().getBoolean(ownerUUID, IslandSetting.ENTITY_INTERACT);
                if (!interactAllowed) {
                    event.setCancelled(true);
                    player.sendActionBar(Component.text("Bu adada dekoratif varlıklarla etkileşime giremezsiniz!").color(NamedTextColor.RED));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        Player player = event.getPlayer();
        Location loc = event.getRightClicked().getLocation();
        if (isOutsideIsland(player, loc)) {
            UUID ownerUUID = getIslandOwner(loc);
            if (ownerUUID != null) {
                boolean allowed = plugin.getIslandSettingManager().getBoolean(ownerUUID, IslandSetting.ENTITY_INTERACT);
                if (!allowed) {
                    event.setCancelled(true);
                    player.sendActionBar(Component.text("Bu zırh askısına dokunamazsınız!").color(NamedTextColor.RED));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        if (event.getRemover() instanceof Player player) {
            Location loc = event.getEntity().getLocation();
            if (isOutsideIsland(player, loc)) {
                UUID ownerUUID = getIslandOwner(loc);
                if (ownerUUID != null) {
                    boolean allowed = plugin.getIslandSettingManager().getBoolean(ownerUUID, IslandSetting.ENTITY_INTERACT);
                    if (!allowed) {
                        event.setCancelled(true);
                        player.sendActionBar(Component.text("Bu dekorasyonu kıramazsınız!").color(NamedTextColor.RED));
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (isOutsideIsland(event.getPlayer(), event.getBlock().getLocation())) {
            UUID owner = getIslandOwner(event.getBlock().getLocation());
            if (!plugin.getRoleManager().hasPermission(owner, event.getPlayer().getUniqueId(), IslandPermission.PLACE_BLOCK)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (isOutsideIsland(event.getPlayer(), event.getBlock().getLocation())) {
            UUID owner = getIslandOwner(event.getBlock().getLocation());
            if (!plugin.getRoleManager().hasPermission(owner, event.getPlayer().getUniqueId(), IslandPermission.BREAK_BLOCK)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            Location loc = event.getItem().getLocation();
            if (isOutsideIsland(player, loc)) {
                UUID ownerUUID = getIslandOwner(loc);
                if (ownerUUID != null) {
                    boolean isMember = plugin.getRoleManager().getRole(ownerUUID, player.getUniqueId()).isAtLeast(com.grizmtech.oneblock.models.IslandRole.COOP);
                    boolean allowed = isMember || plugin.getIslandSettingManager().getBoolean(ownerUUID, IslandSetting.ITEM_PICKUP_DROP);
                    if (!allowed) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Location loc = player.getLocation();
        if (isOutsideIsland(player, loc)) {
            UUID ownerUUID = getIslandOwner(loc);
            if (ownerUUID != null) {
                boolean isMember = plugin.getRoleManager().getRole(ownerUUID, player.getUniqueId()).isAtLeast(com.grizmtech.oneblock.models.IslandRole.COOP);
                boolean allowed = isMember || plugin.getIslandSettingManager().getBoolean(ownerUUID, IslandSetting.ITEM_PICKUP_DROP);
                if (!allowed) {
                    event.setCancelled(true);
                    player.sendActionBar(Component.text("Bu adada yere eşya atamazsınız!").color(NamedTextColor.RED));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        World world = event.getEntity().getWorld();
        if (!world.getName().startsWith("island_")) return;

        UUID ownerUUID = getIslandOwner(event.getEntity().getLocation());
        if (ownerUUID == null) return;

        Player damagerPlayer = null;
        if (event.getDamager() instanceof Player p) {
            damagerPlayer = p;
        } else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            damagerPlayer = p;
        }

        // 1. Ada İçi PvP Kontrolü (Kurban bir oyuncuysa)
        if (event.getEntity() instanceof Player victimPlayer) {
            if (damagerPlayer != null) {
                boolean pvp = plugin.getIslandSettingManager().getBoolean(ownerUUID, IslandSetting.ISLAND_PVP);
                if (!pvp) {
                    event.setCancelled(true);
                    damagerPlayer.sendActionBar(Component.text("Bu adada PvP kapalıdır!").color(NamedTextColor.RED));
                    return;
                }
            }
        }

        // 2. Varlık Hasarı (Item frame, armor stand, painting)
        Entity victim = event.getEntity();
        if (victim instanceof ArmorStand || victim instanceof ItemFrame || victim instanceof GlowItemFrame || victim instanceof Painting) {
            if (damagerPlayer != null && isOutsideIsland(damagerPlayer, victim.getLocation())) {
                boolean allowed = plugin.getIslandSettingManager().getBoolean(ownerUUID, IslandSetting.ENTITY_INTERACT);
                if (!allowed) {
                    event.setCancelled(true);
                    damagerPlayer.sendActionBar(Component.text("Bu varlığa zarar veremezsin!").color(NamedTextColor.RED));
                    return;
                }
            }
        }

        // 3. Genel canavarlara veya hayvanlara yabancıların vurması
        if (damagerPlayer != null && isOutsideIsland(damagerPlayer, victim.getLocation())) {
            boolean isMember = plugin.getRoleManager().getRole(ownerUUID, damagerPlayer.getUniqueId()).isAtLeast(com.grizmtech.oneblock.models.IslandRole.COOP);
            if (!isMember) {
                event.setCancelled(true);
                damagerPlayer.sendActionBar(Component.text("Bu varlığa zarar veremezsin!").color(NamedTextColor.RED));
            }
        }
    }
}
