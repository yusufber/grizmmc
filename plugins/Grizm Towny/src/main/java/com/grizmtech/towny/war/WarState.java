package com.grizmtech.towny.war;

public enum WarState {
    NONE("Yok", "<gray>Savaş Yok</gray>"),
    DECLARED("Savaş İlanı", "<yellow>Hazırlık Aşaması</yellow>"),
    ACTIVE_SIEGE("Kuşatma Aktif", "<red><b>Aktif Kuşatma</b></red>"),
    PEACE_WINDOW("Ateşkes / Gece Koruması", "<aqua>Gece Ateşkesi</aqua>"),
    RESOLUTION("Sonuçlandı", "<green>Zafer & Yağma</green>");

    private final String displayName;
    private final String formattedName;

    WarState(String displayName, String formattedName) {
        this.displayName = displayName;
        this.formattedName = formattedName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getFormattedName() {
        return formattedName;
    }

    public boolean isSiegeActive() {
        return this == ACTIVE_SIEGE;
    }
}
