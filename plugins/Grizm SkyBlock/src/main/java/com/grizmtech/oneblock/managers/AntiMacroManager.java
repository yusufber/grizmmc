package com.grizmtech.oneblock.managers;

import com.grizmtech.oneblock.GrizmOneBlock;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AntiMacroManager {

    private final GrizmOneBlock plugin;
    private final Map<UUID, Float> lastPitches = new ConcurrentHashMap<>();
    private final Map<UUID, Float> lastYaws = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> staticAngleCount = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> correctCaptchaSlots = new ConcurrentHashMap<>();

    public AntiMacroManager(GrizmOneBlock plugin) {
        this.plugin = plugin;
    }

    public boolean isCaptchaActive(UUID uuid) {
        return correctCaptchaSlots.containsKey(uuid);
    }

    public boolean checkPlayer(Player player) {
        if (player.hasPermission("grizmoneblock.bypass")) return true;
        UUID uuid = player.getUniqueId();

        if (isCaptchaActive(uuid)) {
            openCaptchaGUI(player);
            return false;
        }

        float pitch = player.getLocation().getPitch();
        float yaw = player.getLocation().getYaw();

        Float oldPitch = lastPitches.put(uuid, pitch);
        Float oldYaw = lastYaws.put(uuid, yaw);

        if (oldPitch != null && oldYaw != null) {
            float diffPitch = Math.abs(pitch - oldPitch);
            float diffYaw = Math.abs(yaw - oldYaw);

            if (diffPitch < 0.1f && diffYaw < 0.1f) {
                int count = staticAngleCount.getOrDefault(uuid, 0) + 1;
                staticAngleCount.put(uuid, count);

                if (count >= 40) { // 40 blok boyunca milim oynamadan tıklayan şüpheli durum
                    triggerCaptcha(player);
                    return false;
                }
            } else {
                staticAngleCount.put(uuid, 0);
            }
        }

        return true;
    }

    public void triggerCaptcha(Player player) {
        UUID uuid = player.getUniqueId();
        staticAngleCount.put(uuid, 0);

        player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, Integer.MAX_VALUE, 4));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.5f);

        openCaptchaGUI(player);
    }

    public void openCaptchaGUI(Player player) {
        UUID uuid = player.getUniqueId();
        Inventory gui = Bukkit.createInventory(null, 27, Component.text("Doğrulama (Makro Koruması)").color(NamedTextColor.RED));

        ItemStack gray = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta gm = gray.getItemMeta();
        gm.displayName(Component.text(" "));
        gray.setItemMeta(gm);

        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, gray);
        }

        // Rastgele bir slota yeşil yün yerleştir
        int correctSlot = java.util.concurrent.ThreadLocalRandom.current().nextInt(27);
        correctCaptchaSlots.put(uuid, correctSlot);

        ItemStack green = new ItemStack(Material.LIME_WOOL);
        ItemMeta grm = green.getItemMeta();
        grm.displayName(Component.text("Devam etmek için buraya tıkla!").color(NamedTextColor.GREEN));
        green.setItemMeta(grm);
        gui.setItem(correctSlot, green);

        player.openInventory(gui);
    }

    public void handleCaptchaClick(Player player, int slot) {
        UUID uuid = player.getUniqueId();
        Integer correct = correctCaptchaSlots.get(uuid);

        if (correct != null && correct == slot) {
            correctCaptchaSlots.remove(uuid);
            player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
            player.closeInventory();
            player.sendMessage(Component.text("Doğrulama başarılı! Kazmaya devam edebilirsiniz.").color(NamedTextColor.GREEN));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.5f);
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
        }
    }
}
