package com.grizmtech.oneblock.managers;

import com.grizmtech.oneblock.GrizmOneBlock;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class BankManager {

    private final GrizmOneBlock plugin;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM HH:mm");

    public BankManager(GrizmOneBlock plugin) {
        this.plugin = plugin;
    }

    public double getBalance(UUID islandId) {
        return plugin.getDataManager().getConfig().getDouble("players." + islandId.toString() + ".bank.balance", 0.0);
    }

    public void setBalance(UUID islandId, double amount) {
        plugin.getDataManager().getConfig().set("players." + islandId.toString() + ".bank.balance", amount);
        plugin.getDataManager().saveConfig();
    }

    public int getExp(UUID islandId) {
        return plugin.getDataManager().getConfig().getInt("players." + islandId.toString() + ".bank.exp", 0);
    }

    public void setExp(UUID islandId, int amount) {
        plugin.getDataManager().getConfig().set("players." + islandId.toString() + ".bank.exp", amount);
        plugin.getDataManager().saveConfig();
    }

    public List<String> getHistory(UUID islandId) {
        return plugin.getDataManager().getConfig().getStringList("players." + islandId.toString() + ".bank.history");
    }

    public void logTransaction(UUID islandId, String log) {
        String entry = "[" + dateFormat.format(new Date()) + "] " + log;
        List<String> list = new ArrayList<>(getHistory(islandId));
        list.add(0, entry);
        if (list.size() > 10) {
            list = list.subList(0, 10);
        }
        plugin.getDataManager().getConfig().set("players." + islandId.toString() + ".bank.history", list);
        plugin.getDataManager().saveConfig();
    }

    public boolean deposit(UUID islandId, Player player, double amount) {
        if (!Double.isFinite(amount) || amount <= 0) {
            player.sendMessage(Component.text("Yatırılacak miktar 0'dan büyük olmalıdır!").color(NamedTextColor.RED));
            return false;
        }

        if (!plugin.getEconomyManager().hasMoney(player, amount)) {
            player.sendMessage(Component.text("Yetersiz bakiye!").color(NamedTextColor.RED));
            return false;
        }

        if (plugin.getEconomyManager().takeMoney(player, amount)) {
            double current = getBalance(islandId);
            setBalance(islandId, current + amount);
            logTransaction(islandId, player.getName() + " +" + (long)amount + " Para yatırdı.");
            player.sendMessage(Component.text("Ada bankasına ").color(NamedTextColor.GREEN)
                    .append(Component.text((long)amount + " Para").color(NamedTextColor.GOLD))
                    .append(Component.text(" başarıyla yatırıldı.").color(NamedTextColor.GREEN)));
            return true;
        }
        return false;
    }

    public boolean withdraw(UUID islandId, Player player, double amount) {
        if (!Double.isFinite(amount) || amount <= 0) {
            player.sendMessage(Component.text("Çekilecek miktar 0'dan büyük olmalıdır!").color(NamedTextColor.RED));
            return false;
        }

        double current = getBalance(islandId);
        if (current < amount) {
            player.sendMessage(Component.text("Ada bankasında yeterli bakiye yok! Mevcut: " + (long)current).color(NamedTextColor.RED));
            return false;
        }

        setBalance(islandId, current - amount);
        plugin.getEconomyManager().giveMoney(player, amount);
        logTransaction(islandId, player.getName() + " -" + (long)amount + " Para çekti.");
        player.sendMessage(Component.text("Ada bankasından ").color(NamedTextColor.GREEN)
                .append(Component.text((long)amount + " Para").color(NamedTextColor.GOLD))
                .append(Component.text(" çekildi.").color(NamedTextColor.GREEN)));
        return true;
    }

    public boolean depositExp(UUID islandId, Player player, int amount) {
        if (amount <= 0 || player.getTotalExperience() < amount) {
            player.sendMessage(Component.text("Yetersiz XP puanı!").color(NamedTextColor.RED));
            return false;
        }

        player.giveExp(-amount);
        int current = getExp(islandId);
        setExp(islandId, current + amount);
        logTransaction(islandId, player.getName() + " +" + amount + " XP yatırdı.");
        player.sendMessage(Component.text("Ada bankasına ").color(NamedTextColor.GREEN)
                .append(Component.text(amount + " XP").color(NamedTextColor.AQUA))
                .append(Component.text(" yatırıldı.").color(NamedTextColor.GREEN)));
        return true;
    }

    public boolean withdrawExp(UUID islandId, Player player, int amount) {
        if (amount <= 0) return false;
        int current = getExp(islandId);
        if (current < amount) {
            player.sendMessage(Component.text("Ada bankasında yeterli XP yok!").color(NamedTextColor.RED));
            return false;
        }

        setExp(islandId, current - amount);
        player.giveExp(amount);
        logTransaction(islandId, player.getName() + " -" + amount + " XP çekti.");
        player.sendMessage(Component.text("Ada bankasından ").color(NamedTextColor.GREEN)
                .append(Component.text(amount + " XP").color(NamedTextColor.AQUA))
                .append(Component.text(" çekildi.").color(NamedTextColor.GREEN)));
        return true;
    }
}
