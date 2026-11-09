package com.grizmtech.oneblock.managers;

import com.grizmtech.oneblock.GrizmOneBlock;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MissionManager {

    private final GrizmOneBlock plugin;

    public static class MissionInfo {
        private final String id;
        private final String title;
        private final String description;
        private final Material targetMaterial;
        private final int requiredCount;
        private final double rewardMoney;
        private final PhaseManager.Phase phase;

        public MissionInfo(String id, String title, String description, Material targetMaterial, int requiredCount, double rewardMoney, PhaseManager.Phase phase) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.targetMaterial = targetMaterial;
            this.requiredCount = requiredCount;
            this.rewardMoney = rewardMoney;
            this.phase = phase;
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public Material getTargetMaterial() { return targetMaterial; }
        public int getRequiredCount() { return requiredCount; }
        public double getRewardMoney() { return rewardMoney; }
        public PhaseManager.Phase getPhase() { return phase; }
    }

    private final List<MissionInfo> missions = new ArrayList<>();

    public MissionManager(GrizmOneBlock plugin) {
        this.plugin = plugin;
        initMissions();
    }

    private void initMissions() {
        // Orman
        missions.add(new MissionInfo("m_wood_100", "Odun Ustası", "Ana bloktan 100 Meşe Kütüğü kır.", Material.OAK_LOG, 100, 15000, PhaseManager.Phase.ORMAN));
        missions.add(new MissionInfo("m_dirt_50", "Toprak İşçisi", "50 Toprak Bloğu topla.", Material.DIRT, 50, 8000, PhaseManager.Phase.ORMAN));
        missions.add(new MissionInfo("m_coal_20", "İlk Işık", "20 Kömür Cevheri kır.", Material.COAL_ORE, 20, 12000, PhaseManager.Phase.ORMAN));

        // Mağara
        missions.add(new MissionInfo("m_iron_50", "Demir Çağı", "50 Demir Cevheri kır.", Material.IRON_ORE, 50, 25000, PhaseManager.Phase.MAGARA));
        missions.add(new MissionInfo("m_stone_150", "Taş Ocağı", "150 Taş Bloğu kaz.", Material.STONE, 150, 15000, PhaseManager.Phase.MAGARA));
        missions.add(new MissionInfo("m_dia_5", "İlk Pırıltı", "5 Elmas Cevheri bul.", Material.DIAMOND_ORE, 5, 40000, PhaseManager.Phase.MAGARA));

        // Çöl
        missions.add(new MissionInfo("m_sand_100", "Çöl Kaşifi", "100 Kum Bloğu kaz.", Material.SAND, 100, 20000, PhaseManager.Phase.COL));
        missions.add(new MissionInfo("m_gold_30", "Firavun Altınları", "30 Altın Cevheri topla.", Material.GOLD_ORE, 30, 35000, PhaseManager.Phase.COL));

        // Nether
        missions.add(new MissionInfo("m_quartz_50", "Nether Kristali", "50 Nether Kuvarsı kır.", Material.NETHER_QUARTZ_ORE, 50, 30000, PhaseManager.Phase.NETHER));
        missions.add(new MissionInfo("m_netherrack_150", "Ateş Diyarı", "150 Nether Taşı topla.", Material.NETHERRACK, 150, 20000, PhaseManager.Phase.NETHER));
        missions.add(new MissionInfo("m_debris_2", "Antik Hazine", "2 Antik Kalıntı bul.", Material.ANCIENT_DEBRIS, 2, 75000, PhaseManager.Phase.NETHER));

        // End
        missions.add(new MissionInfo("m_endstone_100", "Sonun Başlangıcı", "100 End Taşı kır.", Material.END_STONE, 100, 50000, PhaseManager.Phase.END));
        missions.add(new MissionInfo("m_obsidian_20", "Kırılmaz Güç", "20 Obsidyen topla.", Material.OBSIDIAN, 20, 60000, PhaseManager.Phase.END));
    }

    public List<MissionInfo> getMissionsForPhase(PhaseManager.Phase phase) {
        List<MissionInfo> list = new ArrayList<>();
        for (MissionInfo m : missions) {
            if (m.getPhase() == phase) {
                list.add(m);
            }
        }
        return list;
    }

    public int getProgress(UUID uuid, String missionId) {
        return plugin.getDataManager().getConfig().getInt("players." + uuid + ".missions." + missionId + ".progress", 0);
    }

    public boolean isCompleted(UUID uuid, String missionId) {
        return plugin.getDataManager().getConfig().getBoolean("players." + uuid + ".missions." + missionId + ".completed", false);
    }

    public void addProgress(UUID uuid, Material brokenMat) {
        for (MissionInfo m : missions) {
            if (m.getTargetMaterial() == brokenMat && !isCompleted(uuid, m.getId())) {
                int current = getProgress(uuid, m.getId()) + 1;
                plugin.getDataManager().getConfig().set("players." + uuid + ".missions." + m.getId() + ".progress", current);

                if (current >= m.getRequiredCount()) {
                    plugin.getDataManager().getConfig().set("players." + uuid + ".missions." + m.getId() + ".completed", true);
                    plugin.getDataManager().saveConfig();

                    Player p = org.bukkit.Bukkit.getPlayer(uuid);
                    if (p != null && p.isOnline()) {
                        p.sendMessage(Component.text("★ Görev Tamamlandı: ").color(NamedTextColor.GOLD)
                                .append(Component.text(m.getTitle()).color(NamedTextColor.YELLOW)));
                        if (plugin.getEconomyManager().isAvailable()) {
                            plugin.getEconomyManager().giveMoney(p, m.getRewardMoney());
                            p.sendMessage(Component.text("+" + (long)m.getRewardMoney() + " Para ödülü kazandınız!").color(NamedTextColor.GREEN));
                        }
                        p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.4f);
                    }
                } else if (current % 10 == 0) {
                    plugin.getDataManager().saveConfig();
                }
            }
        }
    }
}
