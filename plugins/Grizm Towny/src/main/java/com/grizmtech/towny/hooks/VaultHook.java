package com.grizmtech.towny.hooks;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class VaultHook {

    private final JavaPlugin plugin;
    private Economy economy;

    public VaultHook(JavaPlugin plugin) {
        this.plugin = plugin;
        setupEconomy();
    }

    private void setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault eklentisi bulunamadı! Ekonomi özellikleri devre dışı.");
            return;
        }
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            this.economy = rsp.getProvider();
            plugin.getLogger().info("Vault Ekonomi entegrasyonu başarıyla kuruldu.");
        }
    }

    public boolean isAvailable() {
        if (economy == null) {
            setupEconomy();
        }
        return economy != null;
    }

    public double getBalance(OfflinePlayer player) {
        if (!isAvailable()) return 0.0;
        return economy.getBalance(player);
    }

    public boolean hasMoney(OfflinePlayer player, double amount) {
        if (!isAvailable()) return false;
        return economy.has(player, amount);
    }

    public boolean takeMoney(OfflinePlayer player, double amount) {
        if (!isAvailable()) return true; // Vault yoksa engelleme
        if (amount <= 0) return true;
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public boolean giveMoney(OfflinePlayer player, double amount) {
        if (!isAvailable()) return true;
        if (amount <= 0) return true;
        return economy.depositPlayer(player, amount).transactionSuccess();
    }
}
