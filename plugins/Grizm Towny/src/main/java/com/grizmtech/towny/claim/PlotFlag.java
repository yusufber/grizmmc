package com.grizmtech.towny.claim;

public enum PlotFlag {
    BLOCK_BREAK(0, "Blok Kırma", "Parseldeki blokları kırma izni"),
    BLOCK_PLACE(1, "Blok Koyma", "Parsele blok yerleştirme izni"),
    CHEST_ACCESS(2, "Sandık Erişimi", "Sandık, varil ve fırınları açabilme izni"),
    DOOR_USE(3, "Kapı ve Buton", "Kapı, tuzak kapısı, düğme ve şalter kullanabilme izni"),
    PVP(4, "PvP", "Oyuncuların birbirine hasar verebilme izni"),
    EXPLOSION(5, "Patlama Hasarı", "TNT ve Creeper patlamalarının hasar verebilme izni");

    private final int bitOffset;
    private final String displayName;
    private final String description;

    PlotFlag(int bitOffset, String displayName, String description) {
        this.bitOffset = bitOffset;
        this.displayName = displayName;
        this.description = description;
    }

    public int getBitOffset() {
        return bitOffset;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
