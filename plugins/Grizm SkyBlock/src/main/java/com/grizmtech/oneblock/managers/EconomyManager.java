package com.grizmtech.oneblock.managers;

import com.grizmtech.oneblock.GrizmOneBlock;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyManager {

    private final GrizmOneBlock plugin;
    private Economy econ = null;

    public EconomyManager(GrizmOneBlock plugin) {
        this.plugin = plugin;
        setupEconomy();
    }

    public boolean isAvailable() {
        if (econ == null) {
            setupEconomy();
        }
        return econ != null;
    }

    private boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public double getBalance(OfflinePlayer player) {
        if (!isAvailable()) return 0.0;
        return econ.getBalance(player);
    }

    public boolean hasMoney(OfflinePlayer player, double amount) {
        if (!isAvailable()) return false;
        return econ.has(player, amount);
    }

    public boolean takeMoney(OfflinePlayer player, double amount) {
        if (!isAvailable()) return false;
        return econ.withdrawPlayer(player, amount).transactionSuccess();
    }

    public boolean giveMoney(OfflinePlayer player, double amount) {
        if (!isAvailable()) return false;
        return econ.depositPlayer(player, amount).transactionSuccess();
    }
}
