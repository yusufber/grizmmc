package com.prizmarin.crates.animation;

import com.prizmarin.crates.CrateTier;
import com.prizmarin.crates.CratesPlugin;
import com.prizmarin.crates.reward.CrateReward;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CrateRoulette implements InventoryHolder {

    private static final Set<UUID> ACTIVE_ROULETTES = ConcurrentHashMap.newKeySet();

    private final CratesPlugin plugin;
    private final Player player;
    private final CrateTier tier;
    private final Inventory inventory;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    // 27 slot inventory:
    // Row 0: 0-8. Slot 4 is red indicator glass.
    // Row 1: 9-17. The roulette reel! Center pointer slot is 13.
    // Row 2: 18-26. Slot 22 is red indicator glass.
    private static final int[] BORDER_SLOTS = {
            0, 1, 2, 3, 5, 6, 7, 8,
            18, 19, 20, 21, 23, 24, 25, 26
    };
    private static final int[] REEL_SLOTS = {9, 10, 11, 12, 13, 14, 15, 16, 17};
    private static final int POINTER_TOP_SLOT = 4;
    private static final int POINTER_BOTTOM_SLOT = 22;
    private static final int WINNER_CENTER_SLOT = 13;

    private final List<CrateReward> reelRewards = new ArrayList<>();
    private final CrateReward selectedWinningReward;
    private boolean completed = false;

    public CrateRoulette(CratesPlugin plugin, Player player, CrateTier tier) {
        this.plugin = plugin;
        this.player = player;
        this.tier = tier;

        // Choose the winner upfront via weighted logic
        this.selectedWinningReward = plugin.getRewardManager().getRandomReward(tier);

        Component title = miniMessage.deserialize(tier.getDisplayName() + " <dark_gray>Rulet</dark_gray>");
        this.inventory = Bukkit.createInventory(this, 27, title);

        setupStaticGUI();
        prefillReel();
    }

    public static boolean isPlayerOpening(UUID uuid) {
        return ACTIVE_ROULETTES.contains(uuid);
    }

    public static void stopOpening(UUID uuid) {
        ACTIVE_ROULETTES.remove(uuid);
    }

    private void setupStaticGUI() {
        ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta blackMeta = blackGlass.getItemMeta();
        if (blackMeta != null) {
            blackMeta.displayName(Component.text(" "));
            blackGlass.setItemMeta(blackMeta);
        }
        for (int slot : BORDER_SLOTS) {
            inventory.setItem(slot, blackGlass);
        }

        ItemStack redGlass = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta redMeta = redGlass.getItemMeta();
        if (redMeta != null) {
            redMeta.displayName(miniMessage.deserialize("<red>▼ <bold>KAZANAN ÖDÜL</bold> ▼</red>"));
            redGlass.setItemMeta(redMeta);
        }
        inventory.setItem(POINTER_TOP_SLOT, redGlass);

        ItemStack redGlassBottom = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta redMetaBottom = redGlassBottom.getItemMeta();
        if (redMetaBottom != null) {
            redMetaBottom.displayName(miniMessage.deserialize("<red>▲ <bold>KAZANAN ÖDÜL</bold> ▲</red>"));
            redGlassBottom.setItemMeta(redMetaBottom);
        }
        inventory.setItem(POINTER_BOTTOM_SLOT, redGlassBottom);
    }

    private void prefillReel() {
        // Prepare 40 items in the tape
        int totalReelLength = 40;
        int winningIndex = 30; // The winning item will land at index 30 when stopped at center slot 13

        for (int i = 0; i < totalReelLength; i++) {
            if (i == winningIndex) {
                reelRewards.add(selectedWinningReward);
            } else {
                reelRewards.add(plugin.getRewardManager().getRandomReward(tier));
            }
        }

        // Render initial 9 slots (0 to 8)
        updateReelSlots(0);
    }

    private void updateReelSlots(int offset) {
        for (int i = 0; i < REEL_SLOTS.length; i++) {
            int slot = REEL_SLOTS[i];
            int itemIndex = offset + i;
            if (itemIndex < reelRewards.size()) {
                CrateReward reward = reelRewards.get(itemIndex);
                if (reward != null && reward.getItem() != null) {
                    inventory.setItem(slot, reward.getItem());
                } else {
                    inventory.setItem(slot, new ItemStack(Material.BARRIER));
                }
            }
        }
    }

    public void start() {
        ACTIVE_ROULETTES.add(player.getUniqueId());
        player.openInventory(inventory);

        // Roulette deceleration schedule
        new BukkitRunnable() {
            int currentOffset = 0;
            final int maxOffset = 26; // winning item is at index 30, slot 13 is offset + 4 -> offset 26
            int ticksElapsed = 0;
            int delay = 1;
            int nextTick = 1;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    finish(true);
                    cancel();
                    return;
                }

                ticksElapsed++;
                if (ticksElapsed < nextTick) {
                    return;
                }

                currentOffset++;
                updateReelSlots(currentOffset);

                // Play tick sound
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.4f);

                // Dynamically adjust delay to create realistic deceleration
                int remainingSteps = maxOffset - currentOffset;
                if (remainingSteps <= 3) {
                    delay = 9;
                } else if (remainingSteps <= 6) {
                    delay = 6;
                } else if (remainingSteps <= 10) {
                    delay = 4;
                } else if (remainingSteps <= 16) {
                    delay = 2;
                } else {
                    delay = 1;
                }

                nextTick = ticksElapsed + delay;

                if (currentOffset >= maxOffset) {
                    cancel();
                    // Victory fanfare after short delay
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        finish(false);
                    }, 15L);
                }
            }
        }.runTaskTimer(plugin, 5L, 1L);
    }

    public synchronized void finish(boolean forceFallback) {
        if (completed) return;
        completed = true;
        ACTIVE_ROULETTES.remove(player.getUniqueId());

        if (selectedWinningReward != null) {
            // Deliver item
            if (selectedWinningReward.getItem() != null) {
                var leftovers = player.getInventory().addItem(selectedWinningReward.getItem());
                for (ItemStack leftover : leftovers.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                }
            }

            // Run commands
            for (String cmd : selectedWinningReward.getCommands()) {
                String parsed = cmd.replace("%player%", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
            }

            if (!forceFallback && player.isOnline()) {
                // Victory effects
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                player.sendMessage(miniMessage.deserialize("<green>Tebrikler! Kazandığınız ödül: </green>" + selectedWinningReward.getDisplayName()));

                // Close inventory after 2 seconds
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.getOpenInventory().getTopInventory().equals(inventory)) {
                        player.closeInventory();
                    }
                }, 40L);
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
