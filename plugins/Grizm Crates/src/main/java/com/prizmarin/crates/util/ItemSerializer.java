package com.prizmarin.crates.util;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public final class ItemSerializer {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private ItemSerializer() {}

    public static ItemStack deserialize(ConfigurationSection section) {
        if (section == null) {
            return new ItemStack(Material.STONE);
        }

        String materialName = section.getString("material", "STONE");
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            material = Material.STONE;
        }

        int amount = Math.max(1, section.getInt("amount", 1));
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        // Display name
        if (section.contains("name")) {
            String nameStr = section.getString("name");
            if (nameStr != null && !nameStr.isEmpty()) {
                meta.displayName(MINI_MESSAGE.deserialize(nameStr));
            }
        }

        // Lore
        if (section.contains("lore")) {
            List<String> loreList = section.getStringList("lore");
            List<Component> componentLore = new ArrayList<>();
            for (String line : loreList) {
                componentLore.add(MINI_MESSAGE.deserialize(line));
            }
            meta.lore(componentLore);
        }

        // Custom Model Data
        if (section.contains("custom-model-data")) {
            meta.setCustomModelData(section.getInt("custom-model-data"));
        }

        // Glowing
        if (section.getBoolean("glowing", false)) {
            meta.addEnchant(Enchantment.LUCK, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        // Item Flags
        if (section.contains("flags")) {
            List<String> flags = section.getStringList("flags");
            for (String flagName : flags) {
                try {
                    meta.addItemFlags(ItemFlag.valueOf(flagName.toUpperCase()));
                } catch (IllegalArgumentException ignored) {}
            }
        }

        // Unsafe Enchantments
        if (section.isConfigurationSection("enchantments")) {
            ConfigurationSection enchSection = section.getConfigurationSection("enchantments");
            if (enchSection != null) {
                for (String enchKey : enchSection.getKeys(false)) {
                    Enchantment ench = Enchantment.getByKey(NamespacedKey.minecraft(enchKey.toLowerCase()));
                    if (ench == null) {
                        ench = Enchantment.getByName(enchKey.toUpperCase());
                    }
                    if (ench != null) {
                        int level = enchSection.getInt(enchKey, 1);
                        meta.addEnchant(ench, level, true);
                    }
                }
            }
        }

        // Skull Texture (Base64 or URL)
        if (meta instanceof SkullMeta skullMeta && section.contains("skull-texture")) {
            String texture = section.getString("skull-texture");
            applySkullTexture(skullMeta, texture);
        }

        // PersistentDataContainer (PDC tags)
        if (section.isConfigurationSection("nbt")) {
            ConfigurationSection nbtSection = section.getConfigurationSection("nbt");
            if (nbtSection != null) {
                PersistentDataContainer pdc = meta.getPersistentDataContainer();
                for (String key : nbtSection.getKeys(false)) {
                    NamespacedKey nKey = new NamespacedKey("grizmcrates", key.toLowerCase());
                    String value = nbtSection.getString(key);
                    if (value != null) {
                        pdc.set(nKey, PersistentDataType.STRING, value);
                    }
                }
            }
        }

        item.setItemMeta(meta);
        return item;
    }

    public static void applySkullTexture(SkullMeta skullMeta, String texture) {
        if (texture == null || texture.isEmpty()) return;

        try {
            PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
            if (texture.startsWith("http://") || texture.startsWith("https://")) {
                URL skinUrl = URI.create(texture).toURL();
                profile.getTextures().setSkin(skinUrl);
            } else {
                String decodedJson;
                if (!texture.contains("{") && !texture.contains("textures")) {
                    decodedJson = new String(Base64.getDecoder().decode(texture), StandardCharsets.UTF_8);
                } else {
                    decodedJson = texture;
                }

                if (decodedJson.contains("http")) {
                    int startIndex = decodedJson.indexOf("http");
                    int endIndex = decodedJson.indexOf("\"", startIndex);
                    if (endIndex == -1) endIndex = decodedJson.indexOf("}", startIndex);
                    if (endIndex <= startIndex) return;
                    String urlStr = decodedJson.substring(startIndex, endIndex).replace("\\/", "/");
                    profile.getTextures().setSkin(URI.create(urlStr).toURL());
                } else {
                    profile.setProperty(new ProfileProperty("textures", texture));
                }
            }
            skullMeta.setPlayerProfile(profile);
        } catch (Exception ignored) {
            try {
                PlayerProfile fallback = Bukkit.createProfile(UUID.randomUUID());
                fallback.setProperty(new ProfileProperty("textures", texture));
                skullMeta.setPlayerProfile(fallback);
            } catch (Exception ignoredFallback) {}
        }
    }
}
