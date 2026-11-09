package com.grizmtech.towny.town;

public enum TownRole {
    MAYOR("Belediye Başkanı", "<gradient:#ff8a00:#e52e71>Başkan</gradient>", 5),
    CO_MAYOR("Başkan Yardımcısı", "<gold>Başkan Yrd.</gold>", 4),
    GENERAL("General", "<red>General</red>", 3),
    BUILDER("Mimar", "<aqua>Mimar</aqua>", 2),
    RESIDENT("Vatandaş", "<green>Vatandaş</green>", 1);

    private final String displayName;
    private final String formattedPrefix;
    private final int level;

    TownRole(String displayName, String formattedPrefix, int level) {
        this.displayName = displayName;
        this.formattedPrefix = formattedPrefix;
        this.level = level;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getFormattedPrefix() {
        return formattedPrefix;
    }

    public int getLevel() {
        return level;
    }

    public boolean isHigherOrEqual(TownRole other) {
        return this.level >= other.level;
    }

    public boolean isMayor() {
        return this == MAYOR;
    }

    public boolean canClaim() {
        return this.level >= CO_MAYOR.level;
    }

    public boolean canUnclaim() {
        return this.level >= CO_MAYOR.level;
    }

    public boolean canInvite() {
        return this.level >= GENERAL.level;
    }

    public boolean canKick(TownRole targetRole) {
        return this.level >= CO_MAYOR.level && this.level > targetRole.level;
    }

    public boolean canWithdraw() {
        return this.level >= CO_MAYOR.level;
    }

    public boolean canDeclareWar() {
        return this.level >= GENERAL.level;
    }

    public boolean canSetSpawn() {
        return this.level >= CO_MAYOR.level;
    }

    public boolean canManagePlots() {
        return this.level >= BUILDER.level;
    }
}
