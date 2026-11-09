package com.prizmarin.crates.reward;

import com.prizmarin.crates.CrateTier;
import com.prizmarin.crates.CratesPlugin;
import com.prizmarin.crates.util.ItemSerializer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class RewardManager {

    private final CratesPlugin plugin;
    private final Map<CrateTier, List<CrateReward>> rewardsByTier = new EnumMap<>(CrateTier.class);
    private final Map<CrateTier, Integer> totalWeightByTier = new EnumMap<>(CrateTier.class);

    public RewardManager(CratesPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadRewards() {
        rewardsByTier.clear();
        totalWeightByTier.clear();

        FileConfiguration config = plugin.getConfig();
        ConfigurationSection tiersSection = config.getConfigurationSection("tiers");
        if (tiersSection == null) {
            return;
        }

        for (CrateTier tier : CrateTier.values()) {
            ConfigurationSection tierSec = tiersSection.getConfigurationSection(tier.getId());
            if (tierSec == null) {
                continue;
            }

            List<Map<?, ?>> rewardList = tierSec.getMapList("rewards");
            List<CrateReward> list = new ArrayList<>();
            int totalWeight = 0;

            for (Map<?, ?> rawMap : rewardList) {
                try {
                    String id = String.valueOf(rawMap.get("id"));
                    int weight = rawMap.containsKey("weight") ? ((Number) rawMap.get("weight")).intValue() : 1;
                    String displayName = rawMap.containsKey("display-name") ? String.valueOf(rawMap.get("display-name")) : id;
                    
                    @SuppressWarnings("unchecked")
                    List<String> commands = rawMap.containsKey("commands") ? (List<String>) rawMap.get("commands") : Collections.emptyList();

                    ItemStack item = null;
                    if (rawMap.containsKey("item")) {
                        // Create a temporary configuration section to parse item
                        ConfigurationSection tempSec = config.createSection("temp_item_parse");
                        for (Map.Entry<?, ?> entry : ((Map<?, ?>) rawMap.get("item")).entrySet()) {
                            tempSec.set(String.valueOf(entry.getKey()), entry.getValue());
                        }
                        item = ItemSerializer.deserialize(tempSec);
                        config.set("temp_item_parse", null);
                    }

                    CrateReward reward = new CrateReward(id, weight, displayName, item, commands);
                    list.add(reward);
                    totalWeight += reward.getWeight();
                } catch (Exception e) {
                    plugin.getLogger().warning("Ödül yüklenirken hata oluştu (" + tier.getId() + "): " + e.getMessage());
                }
            }

            rewardsByTier.put(tier, list);
            totalWeightByTier.put(tier, totalWeight);
        }
    }

    public List<CrateReward> getRewards(CrateTier tier) {
        return rewardsByTier.getOrDefault(tier, Collections.emptyList());
    }

    public int getTotalWeight(CrateTier tier) {
        return totalWeightByTier.getOrDefault(tier, 0);
    }

    public CrateReward getRandomReward(CrateTier tier) {
        List<CrateReward> list = getRewards(tier);
        if (list.isEmpty()) {
            return null;
        }

        int totalWeight = getTotalWeight(tier);
        if (totalWeight <= 0) {
            return list.get(ThreadLocalRandom.current().nextInt(list.size()));
        }

        int randomVal = ThreadLocalRandom.current().nextInt(totalWeight);
        int currentWeight = 0;

        for (CrateReward reward : list) {
            currentWeight += reward.getWeight();
            if (randomVal < currentWeight) {
                return reward;
            }
        }

        return list.get(list.size() - 1);
    }
}
