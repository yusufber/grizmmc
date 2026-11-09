package com.prizmarin.crates.listener;

import com.prizmarin.crates.CrateLocation;
import com.prizmarin.crates.CrateTier;
import com.prizmarin.crates.CratesPlugin;
import com.prizmarin.crates.animation.CrateRoulette;
import com.prizmarin.crates.gui.CratePreviewMenu;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class CrateInteractListener implements Listener {

    private final CratesPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public CrateInteractListener(CratesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            return;
        }

        CrateLocation crate = plugin.getCrateRegistry().getCrateAt(clickedBlock);
        if (crate == null) {
            return;
        }

        Player player = event.getPlayer();
        event.setCancelled(true);

        CrateTier tier = crate.getTier();

        // Sol Tık (Left Click)
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            // Shift + Sol tık ve kasa.admin yetkisi varsa kasayı sil
            if (player.isSneaking() && player.hasPermission("kasa.admin")) {
                plugin.getCrateRegistry().removeCrate(crate);
                player.sendMessage(plugin.getPrefixedMessage("crate-deleted"));
                return;
            }

            // Normal sol tıkta önizleme menüsünü aç
            CratePreviewMenu preview = new CratePreviewMenu(plugin, tier);
            preview.open(player);
            return;
        }

        // Sağ Tık (Right Click)
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (CrateRoulette.isPlayerOpening(player.getUniqueId())) {
                player.sendMessage(plugin.getPrefixedMessage("crate-already-opening"));
                return;
            }

            // PDC Anahtar kontrolü
            if (!plugin.getKeyManager().hasKey(player, tier)) {
                String noKeyMsg = plugin.getConfig().getString("messages.no-key", "<red>Bu kasayı açmak için bir {key} <red>anahtarına ihtiyacınız var!</red>");
                String keyName = plugin.getConfig().getString("tiers." + tier.getId() + ".key-name", tier.getDisplayName() + " Anahtarı");
                noKeyMsg = noKeyMsg.replace("{key}", keyName);
                player.sendMessage(plugin.getPrefix().append(miniMessage.deserialize(noKeyMsg)));
                return;
            }

            // Anahtarı tüket ve ruleti başlat
            if (plugin.getKeyManager().consumeKey(player, tier)) {
                CrateRoulette roulette = new CrateRoulette(plugin, player, tier);
                roulette.start();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        CrateLocation crate = plugin.getCrateRegistry().getCrateAt(block);
        if (crate != null) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.getPrefixedMessage("break-deny"));
        }
    }
}
