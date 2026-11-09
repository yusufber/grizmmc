package com.grizmtech.towny.claim;

public enum PlotGroup {
    RESIDENT(0, "Kasaba Üyesi", "<green>Üye</green>"),
    ALLY(6, "Müttefik", "<aqua>Müttefik</aqua>"),
    OUTSIDER(12, "Yabancı", "<red>Yabancı</red>");

    private final int baseBitShift;
    private final String displayName;
    private final String formattedName;

    PlotGroup(int baseBitShift, String displayName, String formattedName) {
        this.baseBitShift = baseBitShift;
        this.displayName = displayName;
        this.formattedName = formattedName;
    }

    public int getBaseBitShift() {
        return baseBitShift;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getFormattedName() {
        return formattedName;
    }
}
