package com.prizmarin.crates.gui;

import com.prizmarin.crates.CrateTier;
import com.prizmarin.crates.CratesPlugin;
import com.prizmarin.crates.reward.CrateReward;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class CratePreviewMenu implements InventoryHolder {

    private final CratesPlugin plugin;
    private final CrateTier tier;
    private final Inventory inventory;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public CratePreviewMenu(CratesPlugin plugin, CrateTier tier) {
        this.plugin = plugin;
        this.tier = tier;

        Component title = miniMessage.deserialize(tier.getDisplayName() + " <dark_gray>İçeriği</dark_gray>");
        this.inventory = Bukkit.createInventory(this, 54, title);

        setupPreview();
    }

    private void setupPreview() {
        // Border slots around 54 slot inventory
        ItemStack grayGlass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta grayMeta = grayGlass.getItemMeta();
        if (grayMeta != null) {
            grayMeta.displayName(Component.text(" "));
            grayGlass.setItemMeta(grayMeta);
        }

        // Fill border
        for (int i = 0; i < 9; i++) inventory.setItem(i, grayGlass);
        for (int i = 45; i < 54; i++) inventory.setItem(i, grayGlass);
        for (int i = 9; i < 45; i += 9) {
            inventory.setItem(i, grayGlass);
            inventory.setItem(i + 8, grayGlass);
        }

        // Close button at slot 49
        ItemStack barrier = new ItemStack(Material.BARRIER);
        ItemMeta barrierMeta = barrier.getItemMeta();
        if (barrierMeta != null) {
            barrierMeta.displayName(miniMessage.deserialize("<red><b>Menüyü Kapat</b></red>"));
            List<Component> lore = new ArrayList<>();
            lore.add(miniMessage.deserialize("<gray>Geri dönmek için tıklayın.</gray>"));
            barrierMeta.lore(lore);
            barrier.setItemMeta(barrierMeta);
        }
        inventory.setItem(49, barrier);

        // Display rewards with chance calculation
        List<CrateReward> rewards = plugin.getRewardManager().getRewards(tier);
        int totalWeight = Math.max(1, plugin.getRewardManager().getTotalWeight(tier));

        int currentSlot = 10;
        for (CrateReward reward : rewards) {
            if (currentSlot >= 44) break;
            if (currentSlot % 9 == 8) {
                currentSlot += 2; // skip edge borders
            }

            ItemStack displayItem = reward.getItem();
            if (displayItem == null) {
                displayItem = new ItemStack(tier.getIconMaterial());
            }
            displayItem = displayItem.clone();

            ItemMeta meta = displayItem.getItemMeta();
            if (meta != null) {
                List<Component> lore = meta.lore();
                if (lore == null) lore = new ArrayList<>();

                lore.add(Component.empty());
                double chance = ((double) reward.getWeight() / totalWeight) * 100.0;
                lore.add(miniMessage.deserialize(String.format("<yellow>Kazanma Şansı:</yellow> <gold>%%%.2f</gold>", chance)));
                meta.lore(lore);
                displayItem.setItemMeta(meta);
            }

            inventory.setItem(currentSlot, displayItem);
            currentSlot++;
        }
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
