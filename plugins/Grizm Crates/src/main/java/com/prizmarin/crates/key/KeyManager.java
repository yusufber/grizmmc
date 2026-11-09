package com.prizmarin.crates.key;

import com.prizmarin.crates.CrateTier;
import com.prizmarin.crates.CratesPlugin;
import com.prizmarin.crates.util.ItemSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class KeyManager {

    private final CratesPlugin plugin;
    private final NamespacedKey keyTierPdc;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public KeyManager(CratesPlugin plugin) {
        this.plugin = plugin;
        this.keyTierPdc = new NamespacedKey(plugin, "crate_key_tier");
    }

    public NamespacedKey getKeyTierPdc() {
        return keyTierPdc;
    }

    public ItemStack createKey(CrateTier tier, int amount) {
        ConfigurationSection tierSec = plugin.getConfig().getConfigurationSection("tiers." + tier.getId());
        Material mat = Material.TRIPWIRE_HOOK;
        String nameStr = tier.getDisplayName() + " Anahtarı";
        List<String> loreStrings = new ArrayList<>();

        if (tierSec != null) {
            String matName = tierSec.getString("key-material", "TRIPWIRE_HOOK");
            Material m = Material.matchMaterial(matName);
            if (m != null) mat = m;

            nameStr = tierSec.getString("key-name", nameStr);
            loreStrings = tierSec.getStringList("key-lore");
        }

        int safeAmount = Math.min(640, Math.max(1, amount));
        ItemStack item = new ItemStack(mat, safeAmount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(miniMessage.deserialize(nameStr));
            List<Component> lore = new ArrayList<>();
            for (String line : loreStrings) {
                lore.add(miniMessage.deserialize(line));
            }
            meta.lore(lore);
            meta.addEnchant(Enchantment.LUCK, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            // Persistent tag
            meta.getPersistentDataContainer().set(keyTierPdc, PersistentDataType.STRING, tier.getId());
            item.setItemMeta(meta);
        }

        return item;
    }

    public boolean isKey(ItemStack item, CrateTier tier) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        String storedTier = meta.getPersistentDataContainer().get(keyTierPdc, PersistentDataType.STRING);
        if (storedTier == null) return false;
        return storedTier.equalsIgnoreCase(tier.getId());
    }

    public boolean hasKey(Player player, CrateTier tier) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isKey(item, tier) && item.getAmount() > 0) {
                return true;
            }
        }
        return false;
    }

    public boolean consumeKey(Player player, CrateTier tier) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (isKey(item, tier)) {
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                } else {
                    player.getInventory().setItem(i, null);
                }
                player.updateInventory();
                return true;
            }
        }
        return false;
    }

    public void giveKey(Player player, CrateTier tier, int amount) {
        int safeAmount = Math.min(640, Math.max(1, amount));
        ItemStack keyItem = createKey(tier, safeAmount);
        var leftovers = player.getInventory().addItem(keyItem);
        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }
}
