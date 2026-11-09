package com.grizmtech.oneblock.models;

import org.bukkit.Material;

public enum WarpCategory {
    MARKET("Market & Ticaret", Material.EMERALD, "Oyuncuların alışveriş yapabileceği pazar adaları."),
    XP_FARM("XP & Mob Farmı", Material.EXPERIENCE_BOTTLE, "Canavar ve deneyim farmı içeren adalar."),
    PARKUR("Parkur & Eğlence", Material.FEATHER, "Parkur veya oyun alanları bulunan adalar."),
    SERGI("Mimari Sergi", Material.PAINTING, "Özel yapılar ve estetik tasarımlar sergileyen adalar.");

    private final String displayName;
    private final Material icon;
    private final String description;

    WarpCategory(String displayName, Material icon, String description) {
        this.displayName = displayName;
        this.icon = icon;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public String getDescription() {
        return description;
    }
}
