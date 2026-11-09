package com.prizmarin.crates.reward;

import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;

public class CrateReward {

    private final String id;
    private final int weight;
    private final String displayName;
    private final ItemStack item;
    private final List<String> commands;

    public CrateReward(String id, int weight, String displayName, ItemStack item, List<String> commands) {
        this.id = id;
        this.weight = Math.max(1, weight);
        this.displayName = displayName != null ? displayName : id;
        this.item = item;
        this.commands = commands != null ? commands : Collections.emptyList();
    }

    public String getId() {
        return id;
    }

    public int getWeight() {
        return weight;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ItemStack getItem() {
        return item != null ? item.clone() : null;
    }

    public List<String> getCommands() {
        return commands;
    }
}
