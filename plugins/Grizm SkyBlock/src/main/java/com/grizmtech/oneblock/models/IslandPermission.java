package com.grizmtech.oneblock.models;

import org.bukkit.Material;

public enum IslandPermission {
    BREAK_BLOCK("Blok Kırma", Material.IRON_PICKAXE, "Adada blok kırma izni."),
    PLACE_BLOCK("Blok Koyma", Material.GRASS_BLOCK, "Adada blok yerleştirme izni."),
    OPEN_CONTAINER("Sandık ve Depo Açma", Material.CHEST, "Sandık, varil ve depoları açma izni."),
    USE_REDSTONE("Kızıltaş & Mekanizma", Material.LEVER, "Şalter, buton ve kapıları kullanma izni."),
    INVITE_MEMBER("Üye Davet Etme", Material.WRITABLE_BOOK, "Adaya yeni üye davet etme izni."),
    KICK_MEMBER("Üye Çıkarma", Material.BARRIER, "Adadan üye çıkarma izni."),
    BANK_DEPOSIT("Bankaya Yatırma", Material.GOLD_INGOT, "Ada bankasına para ve XP yatırma izni."),
    BANK_WITHDRAW("Bankadan Çekme", Material.EMERALD, "Ada bankasından para ve XP çekme izni."),
    BUY_UPGRADE("Yükseltme Satın Alma", Material.ANVIL, "Ada geliştirmelerini satın alma izni."),
    CHANGE_BIOME("Biyom Değiştirme", Material.WATER_BUCKET, "Adanın biyomunu değiştirme izni."),
    MINE_ONEBLOCK("Tek Blok Kırma", Material.DIAMOND_PICKAXE, "Merkezdeki Tek Bloğu kazma yetkisi.");

    private final String displayName;
    private final Material icon;
    private final String description;

    IslandPermission(String displayName, Material icon, String description) {
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
