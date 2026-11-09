package com.prizmarin.crates;

import org.bukkit.Material;

public enum CrateTier {
    SIRADAN("siradan", "<white>Sıradan Kasa</white>", "WHITE", Material.IRON_INGOT),
    GELISMIS("gelismis", "<green>Gelişmiş Kasa</green>", "GREEN", Material.EMERALD),
    NADIR("nadir", "<blue>Nadir Kasa</blue>", "BLUE", Material.DIAMOND),
    EPIK("epik", "<dark_purple>Epik Kasa</dark_purple>", "DARK_PURPLE", Material.AMETHYST_SHARD),
    MISTIK("mistik", "<light_purple>Mistik Kasa</light_purple>", "LIGHT_PURPLE", Material.NETHER_STAR),
    EFSANEVI("efsanevi", "<gold>Efsanevi Kasa</gold>", "GOLD", Material.NETHERITE_INGOT);

    private final String id;
    private final String displayName;
    private final String colorCode;
    private final Material iconMaterial;

    CrateTier(String id, String displayName, String colorCode, Material iconMaterial) {
        this.id = id;
        this.displayName = displayName;
        this.colorCode = colorCode;
        this.iconMaterial = iconMaterial;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColorCode() {
        return colorCode;
    }

    public Material getIconMaterial() {
        return iconMaterial;
    }

    public static CrateTier fromId(String id) {
        if (id == null) return null;
        for (CrateTier tier : values()) {
            if (tier.id.equalsIgnoreCase(id) || tier.name().equalsIgnoreCase(id)) {
                return tier;
            }
        }
        return null;
    }
}
