package com.grizmtech.towny.claim;

public enum PlotType {
    DEFAULT("Standart / Konut", "<green>Konut</green>", "Kasaba üyelerinin ev inşa edebileceği temel parsel."),
    MARKET("Pazar Yeri", "<gold>Pazar</gold>", "Ticaret ve sandık dükkanlarına uygun parsel."),
    ARENA("Savaş Arenası", "<red>Arena</red>", "İçerisinde serbest PvP kurallarının geçerli olduğu parsel."),
    GUILD("Lonca Merkezi", "<light_purple>Lonca</light_purple>", "Ortak sandık ve birlik faaliyetleri için ayrılmış parsel."),
    FARM("Tarım & Hayvancılık", "<yellow>Tarla</yellow>", "Ekin ve hayvan yetiştiriciliğine odaklı korumalı parsel.");

    private final String displayName;
    private final String formattedName;
    private final String description;

    PlotType(String displayName, String formattedName, String description) {
        this.displayName = displayName;
        this.formattedName = formattedName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getFormattedName() {
        return formattedName;
    }

    public String getDescription() {
        return description;
    }
}
