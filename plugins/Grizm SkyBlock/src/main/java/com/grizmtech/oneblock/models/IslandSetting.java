package com.grizmtech.oneblock.models;

import org.bukkit.Material;

public enum IslandSetting {

    // 1. Güvenlik ve Ziyaretçi İzinleri (Default: false)
    VISITOR_LOCK("Ada Kilidi (Ziyaretçi Girişi)", IslandCategory.SECURITY, Material.IRON_DOOR, false,
            "Adanın dışarıdan gelen ziyaretçilere açık olup olmadığını belirler.",
            "Kilitlendiğinde içerideki ziyaretçiler spawna gönderilir ve yeni giriş engellenir."),

    CONTAINER_ACCESS("Konteyner ve Depo Erişimi", IslandCategory.SECURITY, Material.CHEST, false,
            "Ziyaretçilerin sandık, varil, shulker kutusu, fırın ve huni gibi",
            "envanter barındıran blokları açıp açamayacağını denetler."),

    REDSTONE_USE("Kızıltaş ve Mekanizma Kullanımı", IslandCategory.SECURITY, Material.LEVER, false,
            "Ziyaretçilerin şalter, düğme, basınç plakası, kapı ve tuzak kapılarını",
            "kullanabilmesini kontrol eder. Otomatik sistemlerinizi korur."),

    ITEM_PICKUP_DROP("Eşya Alma ve Atma (Drop)", IslandCategory.SECURITY, Material.HOPPER, false,
            "Ziyaretçilerin yerdeki eşyaları toplamasını ve yere eşya atmasını kontrol eder.",
            "Madenlerinizin çalınmasını veya sistemlerin çöp eşyalarla tıkanmasını önler."),

    VILLAGER_TRADE("Köylü Ticareti", IslandCategory.SECURITY, Material.EMERALD, false,
            "Ziyaretçilerin adadaki köylülerle ticaret yapıp yapamayacağını belirler.",
            "Özel köylülerinizin stoklarının yabancılar tarafından tüketilmesini engeller."),

    ENTITY_INTERACT("Varlık ve Dekorasyon Etkileşimi", IslandCategory.SECURITY, Material.ARMOR_STAND, false,
            "Zırh askıları, eşya çerçeveleri (item frame), tablolar ve saksıların",
            "ziyaretçiler tarafından kırılmasını veya eşyaların alınmasını engeller."),

    // 2. Tek Blok (Ana Blok / Jeneratör) Özel Ayarları
    CORE_BLOCK_MINE("Ana Blok Kırma Yetkisi", IslandCategory.CORE_BLOCK, Material.DIAMOND_PICKAXE, "MEMBERS_ONLY",
            "Merkezdeki Tek Bloğu kimlerin kırabileceğini belirler.",
            "Sadece Ada Sahibi veya Herkes (Ziyaretçiler Dahil) olarak seçilebilir."),

    PHASE_LOCK("Aşama (Phase) Kilidi", IslandCategory.CORE_BLOCK, Material.REPEATER, false,
            "Mevcut aşamayı sabitler. Blok kırılmaya devam eder ancak bir sonraki",
            "aşamaya geçmez; mevcut aşamanın kaynaklarını ve sandıklarını vermeye devam eder."),

    CORE_MOB_SPAWN("Ana Blok Yaratık Doğumu", IslandCategory.CORE_BLOCK, Material.CREEPER_HEAD, true,
            "Tek blok kırılırken içinden çıkan sürpriz yaratıkların (Creeper, Zombi vb.)",
            "çıkıp çıkmayacağını kontrol eder."),

    CORE_PARTICLES("Ana Blok Partikül ve Sesleri", IslandCategory.CORE_BLOCK, Material.NOTE_BLOCK, true,
            "Blok her kırıldığında çıkan yoğun partikülleri ve sesleri açıp kapatır.",
            "Düşük sistemli bilgisayarlarda yüksek FPS ve konfor artışı sağlar."),

    // 3. Çevre, Dünya ve Fizik Kuralları
    NATURAL_MOB_SPAWN("Doğal Yaratık Doğumu", IslandCategory.WORLD_RULES, Material.SPAWNER, true,
            "Adanın karanlık bölgelerinde doğal yollarla düşman veya dost yaratıkların",
            "doğup doğmayacağını belirler (Spawner ve Ana Blok hariç)."),

    MOB_GRIEFING("Patlama ve Mob Hasarı", IslandCategory.WORLD_RULES, Material.TNT, false,
            "Creeper patlamaları, Ghast ateş topları veya Enderman blok çalmasının",
            "adaya zarar verip vermeyeceğini belirler (Kapalı = Bloklar patlamaz)."),

    FIRE_SPREAD("Ateş Yayılımı ve Blok Yanması", IslandCategory.WORLD_RULES, Material.FLINT_AND_STEEL, false,
            "Yıldırım, lav veya çakmak ateşi sonucu oluşan yangının ahşap bloklara",
            "sıçramasını ve blokları yok etmesini engeller."),

    ISLAND_PVP("Ada İçi Oyuncu Hasarı (PvP)", IslandCategory.WORLD_RULES, Material.DIAMOND_SWORD, false,
            "Ada sınırları içerisinde oyuncuların birbirine hasar verip",
            "veremeyeceğini belirler."),

    // 4. Atmosfer ve Görünüm Ayarları
    TIME_WEATHER("Ada Saati ve Hava Durumu", IslandCategory.COSMETICS, Material.CLOCK, "SYNC",
            "Adanın zamanını ve hava durumunu sunucu saatinden bağımsız olarak sabitler.",
            "Modlar: Sunucuyla Eşzamanlı, Gündüz, Gece, Güneşli, Yağmurlu."),

    BORDER_TOGGLE("Dünya Sınırı Görünümü", IslandCategory.COSMETICS, Material.BEACON, true,
            "Adanın bittiği noktadaki görsel sınır çizgisinin (WorldBorder)",
            "görünür olup olmayacağını kontrol eder.");

    public enum IslandCategory {
        SECURITY("1. Ziyaretçi ve Güvenlik İzinleri"),
        CORE_BLOCK("2. Tek Blok Özel Ayarları"),
        WORLD_RULES("3. Çevre ve Dünya Kuralları"),
        COSMETICS("4. Atmosfer ve Görünüm");

        private final String displayName;

        IslandCategory(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private final String displayName;
    private final IslandCategory category;
    private final Material icon;
    private final Object defaultValue;
    private final String[] description;

    IslandSetting(String displayName, IslandCategory category, Material icon, Object defaultValue, String... description) {
        this.displayName = displayName;
        this.category = category;
        this.icon = icon;
        this.defaultValue = defaultValue;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public IslandCategory getCategory() {
        return category;
    }

    public Material getIcon() {
        return icon;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public String[] getDescription() {
        return description;
    }

    public boolean isBoolean() {
        return defaultValue instanceof Boolean;
    }
}
