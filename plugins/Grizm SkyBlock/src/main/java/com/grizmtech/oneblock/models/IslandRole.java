package com.grizmtech.oneblock.models;

import net.kyori.adventure.text.format.NamedTextColor;

public enum IslandRole {
    VISITOR("Ziyaretçi", 0, NamedTextColor.GRAY),
    COOP("Geçici Ortak (Co-op)", 1, NamedTextColor.LIGHT_PURPLE),
    MEMBER("Üye", 2, NamedTextColor.GREEN),
    MODERATOR("Moderatör", 3, NamedTextColor.AQUA),
    OWNER("Ada Lideri", 4, NamedTextColor.GOLD);

    private final String displayName;
    private final int level;
    private final NamedTextColor color;

    IslandRole(String displayName, int level, NamedTextColor color) {
        this.displayName = displayName;
        this.level = level;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getLevel() {
        return level;
    }

    public NamedTextColor getColor() {
        return color;
    }

    public boolean isAtLeast(IslandRole other) {
        return this.level >= other.level;
    }
}
