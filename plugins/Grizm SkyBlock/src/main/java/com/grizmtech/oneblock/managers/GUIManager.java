package com.grizmtech.oneblock.managers;

import com.grizmtech.oneblock.GrizmOneBlock;
import com.grizmtech.oneblock.models.IslandSetting;
import com.grizmtech.oneblock.models.IslandWarp;
import com.grizmtech.oneblock.models.WarpCategory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GUIManager {

    private final GrizmOneBlock plugin;

    public static final Map<Integer, IslandSetting> SETTING_SLOTS = new HashMap<>();

    static {
        // 1. Ziyaretçi ve Güvenlik İzinleri
        SETTING_SLOTS.put(10, IslandSetting.VISITOR_LOCK);
        SETTING_SLOTS.put(11, IslandSetting.CONTAINER_ACCESS);
        SETTING_SLOTS.put(12, IslandSetting.REDSTONE_USE);
        SETTING_SLOTS.put(13, IslandSetting.ITEM_PICKUP_DROP);
        SETTING_SLOTS.put(14, IslandSetting.VILLAGER_TRADE);
        SETTING_SLOTS.put(15, IslandSetting.ENTITY_INTERACT);

        // 2. Tek Blok Özel Ayarları
        SETTING_SLOTS.put(20, IslandSetting.CORE_BLOCK_MINE);
        SETTING_SLOTS.put(21, IslandSetting.PHASE_LOCK);
        SETTING_SLOTS.put(22, IslandSetting.CORE_MOB_SPAWN);
        SETTING_SLOTS.put(23, IslandSetting.CORE_PARTICLES);

        // 3. Çevre ve Dünya Kuralları
        SETTING_SLOTS.put(29, IslandSetting.NATURAL_MOB_SPAWN);
        SETTING_SLOTS.put(30, IslandSetting.MOB_GRIEFING);
        SETTING_SLOTS.put(31, IslandSetting.FIRE_SPREAD);
        SETTING_SLOTS.put(32, IslandSetting.ISLAND_PVP);

        // 4. Atmosfer ve Görünüm
        SETTING_SLOTS.put(39, IslandSetting.TIME_WEATHER);
        SETTING_SLOTS.put(41, IslandSetting.BORDER_TOGGLE);
    }

    public GUIManager(GrizmOneBlock plugin) {
        this.plugin = plugin;
    }

    private ItemStack createGlass() {
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.displayName(Component.text(" "));
        glass.setItemMeta(glassMeta);
        return glass;
    }

    public void openAdaMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 36, Component.text("Ada Menüsü").color(NamedTextColor.GOLD));
        ItemStack glass = createGlass();
        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, glass);
        }

        UUID uuid = player.getUniqueId();
        int blocksBroken = plugin.getDataManager().getBlocksBroken(uuid);
        PhaseManager.Phase currentPhase = plugin.getPhaseManager().getCurrentPhase(blocksBroken);
        PhaseManager.Phase nextPhase = plugin.getPhaseManager().getNextPhase(blocksBroken);
        int prestige = plugin.getRebirthManager().getPrestige(uuid);

        // Durum eşyası (Grass Block)
        ItemStack infoItem = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.displayName(Component.text("Adanın Durumu").color(NamedTextColor.GREEN));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(" "));
        lore.add(Component.text("Mevcut Biyom: ").color(NamedTextColor.YELLOW).append(Component.text(currentPhase.getName()).color(NamedTextColor.WHITE)));
        lore.add(Component.text("Kırılan Blok: ").color(NamedTextColor.YELLOW).append(Component.text(blocksBroken).color(NamedTextColor.WHITE)));
        if (prestige > 0) {
            lore.add(Component.text("Prestij Seviyesi: ").color(NamedTextColor.YELLOW).append(Component.text("★" + prestige).color(NamedTextColor.GOLD)));
        }
        if (nextPhase != null) {
            int needed = nextPhase.getRequiredBlocks() - blocksBroken;
            lore.add(Component.text("Sıradaki Aşama: ").color(NamedTextColor.YELLOW).append(Component.text(nextPhase.getName()).color(NamedTextColor.WHITE)));
            lore.add(Component.text("Gereken Blok: ").color(NamedTextColor.YELLOW).append(Component.text(needed).color(NamedTextColor.WHITE)));
        } else {
            lore.add(Component.text("Tüm aşamaları tamamladın!").color(NamedTextColor.LIGHT_PURPLE));
        }
        infoMeta.lore(lore);
        infoItem.setItemMeta(infoMeta);

        // Işınlanma / Ada Oluşturma eşyası
        boolean hasIsland = plugin.getDataManager().hasIsland(uuid);
        ItemStack tpItem = new ItemStack(hasIsland ? Material.ENDER_PEARL : Material.EMERALD);
        ItemMeta tpMeta = tpItem.getItemMeta();
        tpMeta.displayName(Component.text(hasIsland ? "Adaya Git" : "Ada Oluştur").color(hasIsland ? NamedTextColor.AQUA : NamedTextColor.GREEN));
        tpItem.setItemMeta(tpMeta);

        // Geliştirmeler (Anvil)
        ItemStack upgradeItem = new ItemStack(Material.ANVIL);
        ItemMeta upgradeMeta = upgradeItem.getItemMeta();
        upgradeMeta.displayName(Component.text("Geliştirmeler").color(NamedTextColor.LIGHT_PURPLE));
        upgradeItem.setItemMeta(upgradeMeta);

        // Ada Bankası (Gold Ingot)
        ItemStack bankItem = new ItemStack(Material.GOLD_INGOT);
        ItemMeta bankMeta = bankItem.getItemMeta();
        bankMeta.displayName(Component.text("Ada Bankası").color(NamedTextColor.GOLD));
        List<Component> bankLore = new ArrayList<>();
        bankLore.add(Component.text("Bakiye: " + (long)plugin.getBankManager().getBalance(uuid) + " Para").color(NamedTextColor.YELLOW));
        bankLore.add(Component.text("XP Havuzu: " + plugin.getBankManager().getExp(uuid) + " XP").color(NamedTextColor.AQUA));
        bankMeta.lore(bankLore);
        bankItem.setItemMeta(bankMeta);

        // Görevler (Book)
        ItemStack missionItem = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta missionMeta = missionItem.getItemMeta();
        missionMeta.displayName(Component.text("Aşama Görevleri").color(NamedTextColor.AQUA));
        missionItem.setItemMeta(missionMeta);

        // Ayarlar (Repeater)
        ItemStack settingsItem = new ItemStack(Material.REPEATER);
        ItemMeta settingsMeta = settingsItem.getItemMeta();
        settingsMeta.displayName(Component.text("Ada Ayarları ve Güvenlik").color(NamedTextColor.YELLOW));
        settingsItem.setItemMeta(settingsMeta);

        // Biyom Değiştirici (Water Bucket)
        ItemStack biomeItem = new ItemStack(Material.WATER_BUCKET);
        ItemMeta biomeMeta = biomeItem.getItemMeta();
        biomeMeta.displayName(Component.text("Biyom Değiştirici").color(NamedTextColor.DARK_AQUA));
        biomeItem.setItemMeta(biomeMeta);

        // Otomatik Kırıcı / Minyon (Dispenser)
        ItemStack minerItem = new ItemStack(Material.DISPENSER);
        ItemMeta minerMeta = minerItem.getItemMeta();
        minerMeta.displayName(Component.text("Otomatik Kırıcı (Minyon)").color(NamedTextColor.GOLD));
        List<Component> minerLore = new ArrayList<>();
        boolean minerActive = plugin.getAutoMinerManager().isEnabled(uuid);
        minerLore.add(Component.text("Durum: " + (minerActive ? "[AÇIK]" : "[KAPALI]")).color(minerActive ? NamedTextColor.GREEN : NamedTextColor.RED));
        minerLore.add(Component.text("Kalan Yakıt: " + plugin.getAutoMinerManager().getFuel(uuid)).color(NamedTextColor.YELLOW));
        minerMeta.lore(minerLore);
        minerItem.setItemMeta(minerMeta);

        // Yetenek Ağacı / Beceriler (Diamond Sword)
        ItemStack skillItem = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta skillMeta = skillItem.getItemMeta();
        skillMeta.displayName(Component.text("Ada Yetenek Ağacı (/ada beceri)").color(NamedTextColor.GREEN));
        List<Component> skillLore = new ArrayList<>();
        skillLore.add(Component.text("Yetenek Puanı: " + plugin.getSkillManager().getSkillPoints(uuid)).color(NamedTextColor.GOLD));
        skillMeta.lore(skillLore);
        skillItem.setItemMeta(skillMeta);

        // Ada İlan Panosu & Warplar (Beacon)
        ItemStack warpItem = new ItemStack(Material.BEACON);
        ItemMeta warpMeta = warpItem.getItemMeta();
        warpMeta.displayName(Component.text("Ada İlan Panosu & Warplar").color(NamedTextColor.AQUA));
        warpItem.setItemMeta(warpMeta);

        // Prestij (Nether Star)
        ItemStack prestigeItem = new ItemStack(Material.NETHER_STAR);
        ItemMeta presMeta = prestigeItem.getItemMeta();
        presMeta.displayName(Component.text("Prestij (Rebirth)").color(NamedTextColor.GOLD));
        prestigeItem.setItemMeta(presMeta);

        gui.setItem(10, upgradeItem);
        gui.setItem(11, bankItem);
        gui.setItem(12, infoItem);
        gui.setItem(13, missionItem);
        gui.setItem(14, settingsItem);
        gui.setItem(15, biomeItem);
        gui.setItem(16, tpItem);

        gui.setItem(20, minerItem);
        gui.setItem(21, skillItem);
        gui.setItem(22, prestigeItem);
        gui.setItem(24, warpItem);

        player.openInventory(gui);
    }

    public void openBankMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 36, Component.text("Ada Bankası").color(NamedTextColor.GOLD));
        ItemStack glass = createGlass();
        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, glass);
        }

        UUID uuid = player.getUniqueId();
        double balance = plugin.getBankManager().getBalance(uuid);
        int exp = plugin.getBankManager().getExp(uuid);

        ItemStack info = new ItemStack(Material.CHEST);
        ItemMeta im = info.getItemMeta();
        im.displayName(Component.text("Ada Bankası Kasası").color(NamedTextColor.GOLD));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Mevcut Para: ").color(NamedTextColor.WHITE).append(Component.text((long)balance + " Para").color(NamedTextColor.YELLOW)));
        lore.add(Component.text("Mevcut XP: ").color(NamedTextColor.WHITE).append(Component.text(exp + " XP").color(NamedTextColor.AQUA)));
        lore.add(Component.text(" "));
        lore.add(Component.text("Son İşlemler:").color(NamedTextColor.GRAY));
        List<String> hist = plugin.getBankManager().getHistory(uuid);
        for (String h : hist) {
            lore.add(Component.text(h).color(NamedTextColor.DARK_GRAY));
        }
        im.lore(lore);
        info.setItemMeta(im);
        gui.setItem(4, info);

        gui.setItem(11, createButtonItem(Material.GOLD_NUGGET, "10.000 Para Yatır", NamedTextColor.GREEN));
        gui.setItem(12, createButtonItem(Material.GOLD_INGOT, "100.000 Para Yatır", NamedTextColor.GREEN));
        gui.setItem(13, createButtonItem(Material.GOLD_BLOCK, "Tüm Paranı Yatır", NamedTextColor.GREEN));

        gui.setItem(19, createButtonItem(Material.IRON_NUGGET, "10.000 Para Çek", NamedTextColor.RED));
        gui.setItem(20, createButtonItem(Material.IRON_INGOT, "100.000 Para Çek", NamedTextColor.RED));
        gui.setItem(21, createButtonItem(Material.IRON_BLOCK, "Tüm Parayı Çek", NamedTextColor.RED));

        gui.setItem(15, createButtonItem(Material.EXPERIENCE_BOTTLE, "100 XP Yatır", NamedTextColor.AQUA));
        gui.setItem(24, createButtonItem(Material.GLASS_BOTTLE, "100 XP Çek", NamedTextColor.DARK_AQUA));

        gui.setItem(31, createButtonItem(Material.ARROW, "← Geri Dön", NamedTextColor.YELLOW));
        player.openInventory(gui);
    }

    public void openSkillsMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 36, Component.text("Ada Yetenek Ağacı").color(NamedTextColor.GREEN));
        ItemStack glass = createGlass();
        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, glass);
        }

        UUID uuid = player.getUniqueId();
        int points = plugin.getSkillManager().getSkillPoints(uuid);

        ItemStack info = new ItemStack(Material.NETHER_STAR);
        ItemMeta im = info.getItemMeta();
        im.displayName(Component.text("Yetenek Puanı: " + points).color(NamedTextColor.GOLD));
        List<Component> ilore = new ArrayList<>();
        ilore.add(Component.text("Her 250 blok kırdığınızda 1 Puan kazanırsınız.").color(NamedTextColor.GRAY));
        im.lore(ilore);
        info.setItemMeta(im);
        gui.setItem(4, info);

        // 1. Dal: Madenci (Pickaxe)
        int dura = plugin.getSkillManager().getSkillLevel(uuid, "durability");
        int doubleDrop = plugin.getSkillManager().getSkillLevel(uuid, "double_drop");
        gui.setItem(10, createSkillItem(Material.DIAMOND_PICKAXE, "Kazma Dayanıklılığı", dura, 3, "Kazmanız %" + (dura * 15) + " daha az aşınır.", 1));
        gui.setItem(19, createSkillItem(Material.RAW_IRON, "Çift Eşya Şansı (Double Drop)", doubleDrop, 5, "Bloktan çift maden düşme şansı: %" + (doubleDrop * 5), 2));

        // 2. Dal: Savaşçı (Sword)
        int mobDmg = plugin.getSkillManager().getSkillLevel(uuid, "mob_damage");
        int extraXp = plugin.getSkillManager().getSkillLevel(uuid, "extra_xp");
        gui.setItem(12, createSkillItem(Material.DIAMOND_SWORD, "Canavar Hasarı", mobDmg, 3, "Yaratıklara +%" + (mobDmg * 15) + " ekstra hasar.", 1));
        gui.setItem(21, createSkillItem(Material.EXPERIENCE_BOTTLE, "Ekstra Deneyim (XP)", extraXp, 3, "Yaratıklardan +%" + (extraXp * 25) + " daha fazla XP.", 2));

        // 3. Dal: Mimar & Çiftçi
        int fly = plugin.getSkillManager().getSkillLevel(uuid, "fly");
        int autoReplant = plugin.getSkillManager().getSkillLevel(uuid, "auto_replant");
        gui.setItem(14, createSkillItem(Material.FEATHER, "Ada İçi Uçuş (Fly)", fly, 1, "Kendi adanızda sınırsız uçuş yetkisi sağlar.", 5));
        gui.setItem(23, createSkillItem(Material.WHEAT, "Otomatik Ekin Yenileme", autoReplant, 1, "Hasat edilen ekinleri otomatik yeniden eker.", 3));

        gui.setItem(31, createButtonItem(Material.ARROW, "← Geri Dön", NamedTextColor.YELLOW));
        player.openInventory(gui);
    }

    public void openAutoMinerMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 36, Component.text("Otomatik Kırıcı (Minyon)").color(NamedTextColor.GOLD));
        ItemStack glass = createGlass();
        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, glass);
        }

        UUID uuid = player.getUniqueId();
        boolean active = plugin.getAutoMinerManager().isEnabled(uuid);
        int fuel = plugin.getAutoMinerManager().getFuel(uuid);
        int speedLvl = plugin.getAutoMinerManager().getSpeedLevel(uuid);

        // Durum Göstergesi
        ItemStack status = new ItemStack(active ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta sm = status.getItemMeta();
        sm.displayName(Component.text("Oto-Kırıcı: " + (active ? "ÇALIŞIYOR [AÇIK]" : "DURDURULDU [KAPALI]")).color(active ? NamedTextColor.GREEN : NamedTextColor.RED));
        List<Component> slore = new ArrayList<>();
        slore.add(Component.text("Kalan Yakıt: " + fuel + " Blok").color(NamedTextColor.YELLOW));
        slore.add(Component.text("Kırma Hızı: Her " + (6 - speedLvl) + " saniyede bir").color(NamedTextColor.AQUA));
        slore.add(Component.text("Durumu değiştirmek için tıklayın.").color(NamedTextColor.GRAY));
        sm.lore(slore);
        status.setItemMeta(sm);
        gui.setItem(4, status);

        // Yakıt Doldurma Butonları
        gui.setItem(11, createButtonItem(Material.COAL, "Kömür ile Yakıt Doldur (+20)", NamedTextColor.GRAY));
        gui.setItem(12, createButtonItem(Material.COAL_BLOCK, "Kömür Bloğu ile Yakıt Doldur (+180)", NamedTextColor.DARK_GRAY));
        gui.setItem(13, createButtonItem(Material.LAVA_BUCKET, "Lav Kovası ile Yakıt Doldur (+500)", NamedTextColor.GOLD));
        gui.setItem(15, createButtonItem(Material.GOLD_INGOT, "10.000 Para ile Satın Al (+1.000 Yakıt)", NamedTextColor.GREEN));

        // Hız Yükseltmesi
        ItemStack speedItem = new ItemStack(Material.SUGAR);
        ItemMeta spm = speedItem.getItemMeta();
        spm.displayName(Component.text("Kırma Hızı Yükseltmesi (Seviye " + speedLvl + "/5)").color(NamedTextColor.AQUA));
        List<Component> splore = new ArrayList<>();
        splore.add(Component.text("Ücret: " + (speedLvl * 50000) + " Para").color(NamedTextColor.YELLOW));
        spm.lore(splore);
        speedItem.setItemMeta(spm);
        gui.setItem(22, speedItem);

        // Su-Lav Jeneratör Minyonu Satın Al
        ItemStack minionBuy = new ItemStack(Material.ARMOR_STAND);
        ItemMeta mbm = minionBuy.getItemMeta();
        if (mbm != null) {
            mbm.displayName(Component.text("Su-Lav Jeneratör Minyonu Satın Al").color(NamedTextColor.AQUA).decorate(net.kyori.adventure.text.format.TextDecoration.BOLD));
            List<Component> mblore = new ArrayList<>();
            mblore.add(Component.text("Önündeki su-lav jeneratörünü").color(NamedTextColor.GRAY));
            mblore.add(Component.text("otomatik kırıp depolayan fiziksel minyon.").color(NamedTextColor.GRAY));
            mblore.add(Component.text("Ücret: 25.000 Para").color(NamedTextColor.YELLOW));
            mblore.add(Component.empty());
            mblore.add(Component.text("▸ Satın almak için tıklayın.").color(NamedTextColor.GREEN));
            mbm.lore(mblore);
            minionBuy.setItemMeta(mbm);
        }
        gui.setItem(24, minionBuy);

        gui.setItem(31, createButtonItem(Material.ARROW, "← Geri Dön", NamedTextColor.YELLOW));
        player.openInventory(gui);
    }

    public void openWarpsMenu(Player player, WarpCategory filter) {
        Inventory gui = Bukkit.createInventory(null, 54, Component.text("Ada İlan Panosu & Warplar").color(NamedTextColor.DARK_AQUA));
        ItemStack glass = createGlass();
        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, glass);
        }

        // Filtre Butonları
        gui.setItem(1, createFilterItem(Material.NETHER_STAR, "Tümü", filter == null));
        gui.setItem(3, createFilterItem(Material.EMERALD, "Marketler", filter == WarpCategory.MARKET));
        gui.setItem(5, createFilterItem(Material.EXPERIENCE_BOTTLE, "XP Farmları", filter == WarpCategory.XP_FARM));
        gui.setItem(7, createFilterItem(Material.PAINTING, "Sergi & Parkur", filter == WarpCategory.SERGI));

        List<IslandWarp> list = plugin.getIslandSocialManager().getWarps(filter);
        int slot = 19;
        for (IslandWarp w : list) {
            if (slot > 43) break;
            ItemStack item = new ItemStack(w.getCategory().getIcon());
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(w.getOwnerName() + " Adasına Git").color(NamedTextColor.GOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Kategori: " + w.getCategory().getDisplayName()).color(NamedTextColor.YELLOW));
            lore.add(Component.text("Açıklama: " + w.getDescription()).color(NamedTextColor.GRAY));
            lore.add(Component.text("Beğeni: " + w.getLikes() + " ★").color(NamedTextColor.GREEN));
            lore.add(Component.text("▶ Işınlanmak için tıklayın.").color(NamedTextColor.AQUA));
            meta.lore(lore);
            item.setItemMeta(meta);
            gui.setItem(slot++, item);
            if (slot == 26) slot = 28;
            if (slot == 35) slot = 37;
        }

        gui.setItem(49, createButtonItem(Material.ARROW, "← Geri Dön", NamedTextColor.YELLOW));
        player.openInventory(gui);
    }

    public void openMissionsMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, Component.text("Aşama Görevleri").color(NamedTextColor.AQUA));
        ItemStack glass = createGlass();
        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, glass);
        }

        UUID uuid = player.getUniqueId();
        int blocks = plugin.getDataManager().getBlocksBroken(uuid);
        PhaseManager.Phase phase = plugin.getPhaseManager().getCurrentPhase(blocks);

        List<MissionManager.MissionInfo> missionList = plugin.getMissionManager().getMissionsForPhase(phase);

        int slot = 11;
        for (MissionManager.MissionInfo m : missionList) {
            ItemStack item = new ItemStack(m.getTargetMaterial());
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(m.getTitle()).color(NamedTextColor.GOLD));

            int progress = plugin.getMissionManager().getProgress(uuid, m.getId());
            boolean done = plugin.getMissionManager().isCompleted(uuid, m.getId());

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(m.getDescription()).color(NamedTextColor.GRAY));
            lore.add(Component.text(" "));
            lore.add(Component.text("İlerleme: ").color(NamedTextColor.WHITE)
                    .append(Component.text(Math.min(progress, m.getRequiredCount()) + " / " + m.getRequiredCount()).color(NamedTextColor.YELLOW)));
            lore.add(Component.text("Ödül: " + (long)m.getRewardMoney() + " Para").color(NamedTextColor.GREEN));
            lore.add(Component.text("Durum: " + (done ? "TAMAMLANDI ✓" : "DEVAM EDİYOR")).color(done ? NamedTextColor.GREEN : NamedTextColor.YELLOW));

            meta.lore(lore);
            item.setItemMeta(meta);
            gui.setItem(slot, item);
            slot += 2;
        }

        gui.setItem(22, createButtonItem(Material.ARROW, "← Geri Dön", NamedTextColor.YELLOW));
        player.openInventory(gui);
    }

    public void openBiomesMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, Component.text("Biyom Değiştirici").color(NamedTextColor.DARK_AQUA));
        ItemStack glass = createGlass();
        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, glass);
        }

        int slot = 9;
        for (BiomeManager.IslandBiome b : BiomeManager.IslandBiome.values()) {
            ItemStack item = new ItemStack(b.getIcon());
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(b.getDisplayName()).color(NamedTextColor.AQUA));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Ücret: " + (long)b.getCost() + " Para").color(NamedTextColor.GOLD));
            lore.add(Component.text("Adanızın biyomunu değiştirmek için tıklayın.").color(NamedTextColor.GRAY));
            meta.lore(lore);
            item.setItemMeta(meta);
            gui.setItem(slot++, item);
        }

        gui.setItem(22, createButtonItem(Material.ARROW, "← Geri Dön", NamedTextColor.YELLOW));
        player.openInventory(gui);
    }

    public void openUpgradeMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 36, Component.text("Ada Geliştirmeleri").color(NamedTextColor.DARK_PURPLE));
        ItemStack glass = createGlass();
        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, glass);
        }

        UUID uuid = player.getUniqueId();
        int borderLevel = plugin.getDataManager().getBorderLevel(uuid);
        int generatorLevel = plugin.getDataManager().getGeneratorLevel(uuid);
        int hopperLevel = plugin.getDataManager().getHopperLevel(uuid);
        int boosterLevel = plugin.getUpgradeManager().getGeneratorBooster(uuid);
        int speedLevel = plugin.getUpgradeManager().getSpeedBooster(uuid);

        gui.setItem(10, createUpgradeCard(Material.OAK_FENCE, "Dünya Sınırı Yükseltmesi", borderLevel, 5,
                plugin.getUpgradeManager().getBorderSize(borderLevel) + " Blok",
                plugin.getUpgradeManager().getBorderUpgradeCost(borderLevel)));

        gui.setItem(11, createUpgradeCard(Material.BLAST_FURNACE, "Maden Jeneratörü (Su & Lav)", generatorLevel, 5,
                plugin.getUpgradeManager().getGeneratorDescription(generatorLevel),
                plugin.getUpgradeManager().getGeneratorUpgradeCost(generatorLevel)));

        gui.setItem(12, createUpgradeCard(Material.HOPPER, "Huni Sınırı Yükseltmesi", hopperLevel, 5,
                plugin.getUpgradeManager().getHopperLimit(hopperLevel) + " Huni",
                plugin.getUpgradeManager().getHopperUpgradeCost(hopperLevel)));

        gui.setItem(14, createUpgradeCard(Material.DIAMOND_BLOCK, "Ana Blok Zenginliği", boosterLevel, 5,
                "+%" + (boosterLevel * 5) + " Değerli Maden Şansı",
                plugin.getUpgradeManager().getGeneratorBoosterCost(boosterLevel)));

        gui.setItem(15, createUpgradeCard(Material.WHEAT, "Ekin ve Spawner Hızı", speedLevel, 5,
                "+%" + (speedLevel * 10) + " Büyüme ve Doğurma Hızı",
                plugin.getUpgradeManager().getSpeedBoosterCost(speedLevel)));

        gui.setItem(31, createButtonItem(Material.ARROW, "← Geri Dön", NamedTextColor.YELLOW));
        player.openInventory(gui);
    }

    public void openSettingsMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, Component.text("Ada Ayarları ve İzinler").color(NamedTextColor.DARK_AQUA));
        ItemStack glass = createGlass();
        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, glass);
        }

        UUID ownerUUID = player.getUniqueId();

        ItemStack titleItem = new ItemStack(Material.NETHER_STAR);
        ItemMeta titleMeta = titleItem.getItemMeta();
        titleMeta.displayName(Component.text("Ada Ayarları ve Güvenlik Yönetimi").color(NamedTextColor.GOLD));
        titleItem.setItemMeta(titleMeta);
        gui.setItem(4, titleItem);

        gui.setItem(1, createCategoryItem(Material.SHIELD, "1. Ziyaretçi ve Güvenlik İzinleri", NamedTextColor.GOLD));
        gui.setItem(19, createCategoryItem(Material.BEACON, "2. Tek Blok Özel Ayarları", NamedTextColor.GREEN));
        gui.setItem(28, createCategoryItem(Material.GRASS_BLOCK, "3. Çevre ve Dünya Kuralları", NamedTextColor.AQUA));
        gui.setItem(37, createCategoryItem(Material.SUNFLOWER, "4. Atmosfer ve Görünüm", NamedTextColor.LIGHT_PURPLE));

        for (Map.Entry<Integer, IslandSetting> entry : SETTING_SLOTS.entrySet()) {
            int slot = entry.getKey();
            IslandSetting setting = entry.getValue();

            ItemStack item = new ItemStack(setting.getIcon());
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(setting.getDisplayName()).color(NamedTextColor.YELLOW));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Kategori: ").color(NamedTextColor.DARK_GRAY)
                    .append(Component.text(setting.getCategory().getDisplayName()).color(NamedTextColor.GRAY)));
            lore.add(Component.text(" "));

            for (String descLine : setting.getDescription()) {
                lore.add(Component.text(descLine).color(NamedTextColor.GRAY));
            }

            lore.add(Component.text(" "));

            if (setting.isBoolean()) {
                boolean val = plugin.getIslandSettingManager().getBoolean(ownerUUID, setting);
                lore.add(Component.text("Mevcut Durum: ").color(NamedTextColor.WHITE)
                        .append(val ? Component.text("[AÇIK]").color(NamedTextColor.GREEN) : Component.text("[KAPALI]").color(NamedTextColor.RED)));
                lore.add(Component.text("▶ Değiştirmek için tıklayın.").color(NamedTextColor.DARK_AQUA));
            } else if (setting == IslandSetting.CORE_BLOCK_MINE) {
                String val = plugin.getIslandSettingManager().getString(ownerUUID, setting);
                String label = val.equals("MEMBERS_ONLY") ? "Sadece Ada Sahibi" : "Herkes (Ziyaretçiler Dahil)";
                NamedTextColor col = val.equals("MEMBERS_ONLY") ? NamedTextColor.GREEN : NamedTextColor.GOLD;
                lore.add(Component.text("Yetki: ").color(NamedTextColor.WHITE).append(Component.text("[" + label + "]").color(col)));
                lore.add(Component.text("▶ Değiştirmek için tıklayın.").color(NamedTextColor.DARK_AQUA));
            } else if (setting == IslandSetting.TIME_WEATHER) {
                String val = plugin.getIslandSettingManager().getString(ownerUUID, setting);
                String label = plugin.getIslandSettingManager().getTimeWeatherLabel(val);
                lore.add(Component.text("Mevcut Mod: ").color(NamedTextColor.WHITE).append(Component.text("[" + label + "]").color(NamedTextColor.AQUA)));
                lore.add(Component.text("▶ Sonraki moda geçmek için tıklayın.").color(NamedTextColor.DARK_AQUA));
            }

            meta.lore(lore);
            item.setItemMeta(meta);
            gui.setItem(slot, item);
        }

        gui.setItem(49, createButtonItem(Material.ARROW, "← Geri Dön", NamedTextColor.YELLOW));
        player.openInventory(gui);
    }

    public void openAdminSpyMenu(Player admin, OfflinePlayer target) {
        Inventory gui = Bukkit.createInventory(null, 27, Component.text("Ajan (Spy): " + target.getName()).color(NamedTextColor.RED));
        ItemStack glass = createGlass();
        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, glass);
        }

        UUID uuid = target.getUniqueId();
        int blocks = plugin.getDataManager().getBlocksBroken(uuid);
        PhaseManager.Phase phase = plugin.getPhaseManager().getCurrentPhase(blocks);
        double bank = plugin.getBankManager().getBalance(uuid);
        int prestige = plugin.getRebirthManager().getPrestige(uuid);
        long islandLevel = plugin.getIslandLevelManager().getIslandLevel(uuid);

        ItemStack info = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta im = info.getItemMeta();
        im.displayName(Component.text(target.getName() + " - Ada İstatistikleri").color(NamedTextColor.GOLD));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Kırılan Blok: " + blocks).color(NamedTextColor.YELLOW));
        lore.add(Component.text("Mevcut Aşama: " + phase.getName()).color(NamedTextColor.YELLOW));
        lore.add(Component.text("Ada Seviyesi: " + islandLevel).color(NamedTextColor.AQUA));
        lore.add(Component.text("Prestij Seviyesi: ★" + prestige).color(NamedTextColor.GOLD));
        lore.add(Component.text("Banka Bakiyesi: " + (long)bank + " Para").color(NamedTextColor.GREEN));
        im.lore(lore);
        info.setItemMeta(im);

        gui.setItem(13, info);
        admin.openInventory(gui);
    }

    private ItemStack createSkillItem(Material mat, String name, int currentLvl, int maxLvl, String desc, int cost) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name).color(NamedTextColor.GOLD));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Seviye: " + currentLvl + " / " + maxLvl).color(NamedTextColor.YELLOW));
        lore.add(Component.text(desc).color(NamedTextColor.WHITE));
        if (currentLvl < maxLvl) {
            lore.add(Component.text("Gerekli Puan: " + cost + " Yetenek Puanı").color(NamedTextColor.GREEN));
            lore.add(Component.text("Yükseltmek için tıklayın.").color(NamedTextColor.AQUA));
        } else {
            lore.add(Component.text("MAKSİMUM SEVİYE ✓").color(NamedTextColor.GREEN));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createUpgradeCard(Material mat, String name, int currentLvl, int maxLvl, String desc, double cost) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name).color(NamedTextColor.GOLD));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Seviye: " + currentLvl + " / " + maxLvl).color(NamedTextColor.YELLOW));
        lore.add(Component.text("Özellik: " + desc).color(NamedTextColor.WHITE));
        if (cost != -1) {
            lore.add(Component.text("Ücret: " + (long)cost + " Para").color(NamedTextColor.GREEN));
            lore.add(Component.text("Satın almak için tıklayın.").color(NamedTextColor.GRAY));
        } else {
            lore.add(Component.text("Maksimum Seviyedesiniz!").color(NamedTextColor.RED));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createButtonItem(Material mat, String name, NamedTextColor color) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name).color(color));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createFilterItem(Material mat, String name, boolean active) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name).color(active ? NamedTextColor.GREEN : NamedTextColor.GRAY));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCategoryItem(Material mat, String name, NamedTextColor color) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name).color(color));
        item.setItemMeta(meta);
        return item;
    }
}
