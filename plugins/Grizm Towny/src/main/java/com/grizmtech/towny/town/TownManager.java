package com.grizmtech.towny.town;

import com.grizmtech.towny.core.ConfigManager;
import com.grizmtech.towny.core.MessageUtil;
import com.grizmtech.towny.hooks.VaultHook;
import com.grizmtech.towny.storage.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TownManager {

    private final JavaPlugin plugin;
    private final DatabaseManager databaseManager;
    private final ConfigManager configManager;
    private final VaultHook vaultHook;

    private final Map<UUID, Town> towns = new ConcurrentHashMap<>();
    private final Map<String, UUID> townByName = new ConcurrentHashMap<>();
    private final Map<UUID, TownMember> members = new ConcurrentHashMap<>();
    private final Map<UUID, Long> townTruces = new ConcurrentHashMap<>();

    // Davetler: Oyuncu UUID -> (Town UUID, Süre)
    private final Map<UUID, InviteData> pendingInvites = new ConcurrentHashMap<>();

    public record InviteData(UUID townUuid, long expireTime) {}

    public TownManager(JavaPlugin plugin, DatabaseManager databaseManager, ConfigManager configManager, VaultHook vaultHook) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.configManager = configManager;
        this.vaultHook = vaultHook;

        loadAll();
    }

    private void loadAll() {
        towns.putAll(databaseManager.loadTowns());
        for (Town town : towns.values()) {
            townByName.put(town.getName().toLowerCase(), town.getUuid());
        }
        members.putAll(databaseManager.loadMembers());
        townTruces.putAll(databaseManager.loadTruces());

        plugin.getLogger().info(String.format("Yüklendi: %d kasaba, %d üye.", towns.size(), members.size()));
    }

    public Town getTown(UUID uuid) {
        return uuid != null ? towns.get(uuid) : null;
    }

    public Town getTown(String name) {
        if (name == null) return null;
        UUID uuid = townByName.get(name.toLowerCase());
        return uuid != null ? towns.get(uuid) : null;
    }

    public Town getTownByPlayer(UUID playerUuid) {
        TownMember member = members.get(playerUuid);
        if (member == null) return null;
        return towns.get(member.getTownUuid());
    }

    public TownMember getMember(UUID playerUuid) {
        return members.get(playerUuid);
    }

    public List<TownMember> getTownMembers(UUID townUuid) {
        List<TownMember> list = new ArrayList<>();
        for (TownMember member : members.values()) {
            if (member.getTownUuid().equals(townUuid)) {
                list.add(member);
            }
        }
        return list;
    }

    public Collection<Town> getAllTowns() {
        return Collections.unmodifiableCollection(towns.values());
    }

    public boolean createTown(Player leader, String name) {
        if (leader.getWorld().getName().equalsIgnoreCase("lobby")) {
            MessageUtil.sendMessage(leader, "<red>Lobi dünyasında kasaba kuramazsınız! Önce <yellow>/rtp</yellow> yazarak vahşi doğaya ışınlanın.");
            return false;
        }

        if (getTownByPlayer(leader.getUniqueId()) != null) {
            MessageUtil.sendMessage(leader, "<red>Zaten bir kasabanın üyesisiniz!");
            return false;
        }

        if (name.length() < 3 || name.length() > 16 || !name.matches("^[a-zA-Z0-9_]+$")) {
            MessageUtil.sendMessage(leader, "<red>Kasaba ismi 3-16 karakter uzunluğunda olmalı ve sadece harf/rakam içermelidir!");
            return false;
        }

        if (townByName.containsKey(name.toLowerCase())) {
            MessageUtil.sendMessage(leader, "<red>Bu isimde bir kasaba zaten mevcut!");
            return false;
        }

        double cost = configManager.getTownCreateCost();
        if (cost > 0 && !vaultHook.takeMoney(leader, cost)) {
            MessageUtil.sendMessage(leader, "<red>Kasaba kurmak için yeterli paranız yok! Gerekli: <yellow>" + cost + "$</yellow>");
            return false;
        }

        UUID townUuid = UUID.randomUUID();
        Location homeLoc = leader.getLocation();
        long now = System.currentTimeMillis();

        Town town = new Town(townUuid, name, leader.getUniqueId(), null, 0.0, 1, homeLoc, 0.0, 0, now);
        TownMember leaderMember = new TownMember(leader.getUniqueId(), townUuid, TownRole.MAYOR, now, 0.0);

        towns.put(townUuid, town);
        townByName.put(name.toLowerCase(), townUuid);
        members.put(leader.getUniqueId(), leaderMember);

        databaseManager.saveTownAsync(town);
        databaseManager.saveMemberAsync(leaderMember);

        MessageUtil.broadcast(String.format("<gold><b>%s</b></gold> adlı oyuncu yeni bir kasaba kurdu: <yellow><b>%s</b></yellow>!", leader.getName(), name));
        MessageUtil.sendMessage(leader, "<green>Kasabanız başarıyla kuruldu! Mevcut chunk merkez ilan edildi.</green>");

        return true;
    }

    public void disbandTown(Town town, Player mayor) {
        if (town == null) return;
        if (!town.getLeaderUuid().equals(mayor.getUniqueId()) && !mayor.hasPermission("grizmtowny.admin")) {
            MessageUtil.sendMessage(mayor, "<red>Sadece belediye başkanı kasabayı dağıtabilir!");
            return;
        }

        UUID townUuid = town.getUuid();
        String townName = town.getName();

        // Üyeleri temizle
        for (TownMember member : getTownMembers(townUuid)) {
            members.remove(member.getPlayerUuid());
        }

        towns.remove(townUuid);
        townByName.remove(townName.toLowerCase());

        databaseManager.deleteTownAsync(townUuid);

        MessageUtil.broadcast("<red><b>" + townName + "</b> kasabası başkanı tarafından dağıtıldı!</red>");
    }

    public boolean invitePlayer(Town town, Player inviter, Player target) {
        if (town == null || target == null) return false;
        TownMember inviterMember = getMember(inviter.getUniqueId());
        if (inviterMember == null || !inviterMember.getRole().canInvite()) {
            MessageUtil.sendMessage(inviter, "<red>Kasabaya oyuncu davet etme yetkiniz yok!");
            return false;
        }

        if (getTownByPlayer(target.getUniqueId()) != null) {
            MessageUtil.sendMessage(inviter, "<red>Bu oyuncu zaten bir kasabaya üye!");
            return false;
        }

        int maxMembers = town.getMaxMembers(configManager.getBaseMaxMembers(), configManager.getMembersPerLevel());
        if (getTownMembers(town.getUuid()).size() >= maxMembers) {
            MessageUtil.sendMessage(inviter, "<red>Kasabanız maksimum üye sınırına ulaştı (" + maxMembers + ")! Seviye atlayarak sınırı yükseltin.");
            return false;
        }

        long expireTime = System.currentTimeMillis() + 60000L;
        pendingInvites.put(target.getUniqueId(), new InviteData(town.getUuid(), expireTime));

        MessageUtil.sendMessage(inviter, "<green>" + target.getName() + " oyuncusuna kasaba daveti gönderildi.</green>");

        String msg = String.format("<gold><b>GRIZM TOWNY</b></gold> <gray>»</gray> <yellow><b>%s</b></yellow> kasabasından davet aldınız!\n" +
                "<click:run_command:'/t join %s'><hover:show_text:'<green>Kasabaya Katıl'><green><b>[✔ Katıl]</b></green></hover></click> <gray>(60 saniye geçerli)</gray>",
                town.getName(), town.getName());
        MessageUtil.sendRawMessage(target, msg);

        return true;
    }

    public boolean joinTown(Player player, String townName) {
        Town town = getTown(townName);
        if (town == null) {
            MessageUtil.sendMessage(player, "<red>Böyle bir kasaba bulunamadı!");
            return false;
        }

        if (getTownByPlayer(player.getUniqueId()) != null) {
            MessageUtil.sendMessage(player, "<red>Zaten bir kasabanın üyesisiniz!");
            return false;
        }

        InviteData invite = pendingInvites.get(player.getUniqueId());
        if (invite == null || !invite.townUuid.equals(town.getUuid()) || System.currentTimeMillis() > invite.expireTime) {
            MessageUtil.sendMessage(player, "<red>Bu kasabadan geçerli bir davetiniz yok veya davet süresi dolmuş!");
            return false;
        }

        int maxMembers = town.getMaxMembers(configManager.getBaseMaxMembers(), configManager.getMembersPerLevel());
        if (getTownMembers(town.getUuid()).size() >= maxMembers) {
            MessageUtil.sendMessage(player, "<red>Kasaba maksimum üye kapasitesine ulaşmış!");
            return false;
        }

        pendingInvites.remove(player.getUniqueId());

        TownMember member = new TownMember(player.getUniqueId(), town.getUuid(), TownRole.RESIDENT, System.currentTimeMillis(), 0.0);
        members.put(player.getUniqueId(), member);
        databaseManager.saveMemberAsync(member);

        MessageUtil.sendMessage(player, "<green><b>" + town.getName() + "</b> kasabasına başarıyla katıldınız!</green>");
        broadcastToTown(town.getUuid(), "<yellow>" + player.getName() + " kasabamıza yeni vatandaş olarak katıldı!</yellow>");

        return true;
    }

    public boolean kickPlayer(Town town, Player kicker, UUID targetUuid) {
        TownMember kickerMember = getMember(kicker.getUniqueId());
        TownMember targetMember = getMember(targetUuid);

        if (kickerMember == null || targetMember == null || !kickerMember.getTownUuid().equals(town.getUuid())) {
            return false;
        }

        if (!kickerMember.getRole().canKick(targetMember.getRole())) {
            MessageUtil.sendMessage(kicker, "<red>Bu rütbedeki bir üyeyi kasabadan atamazsınız!");
            return false;
        }

        members.remove(targetUuid);
        databaseManager.deleteMemberAsync(targetUuid);

        Player target = Bukkit.getPlayer(targetUuid);
        if (target != null) {
            MessageUtil.sendMessage(target, "<red><b>" + town.getName() + "</b> kasabasından atıldınız!</red>");
        }

        broadcastToTown(town.getUuid(), "<red>" + (target != null ? target.getName() : "Bir üye") + " kasabadan atıldı.</red>");
        return true;
    }

    public boolean setRole(Town town, Player setter, UUID targetUuid, TownRole newRole) {
        TownMember setterMember = getMember(setter.getUniqueId());
        TownMember targetMember = getMember(targetUuid);

        if (setterMember == null || targetMember == null || !setterMember.getTownUuid().equals(town.getUuid())) {
            return false;
        }

        if (!setterMember.getRole().isMayor()) {
            MessageUtil.sendMessage(setter, "<red>Sadece belediye başkanı rütbe atayabilir!");
            return false;
        }

        if (newRole == TownRole.MAYOR) {
            // Başkanlığı devret
            setterMember.setRole(TownRole.CO_MAYOR);
            targetMember.setRole(TownRole.MAYOR);
            town.setLeaderUuid(targetUuid);
            databaseManager.saveMemberAsync(setterMember);
            databaseManager.saveMemberAsync(targetMember);
            databaseManager.saveTownAsync(town);
            broadcastToTown(town.getUuid(), "<gold>Belediye başkanlığı yeni sahibine devredildi!</gold>");
            return true;
        }

        targetMember.setRole(newRole);
        databaseManager.saveMemberAsync(targetMember);

        Player target = Bukkit.getPlayer(targetUuid);
        if (target != null) {
            MessageUtil.sendMessage(target, "<green>Yeni rütbeniz: " + newRole.getFormattedPrefix() + "</green>");
        }
        MessageUtil.sendMessage(setter, "<green>Rütbe başarıyla güncellendi.</green>");
        return true;
    }

    public boolean deposit(Town town, Player player, double amount) {
        if (amount <= 0) return false;
        if (!vaultHook.takeMoney(player, amount)) {
            MessageUtil.sendMessage(player, "<red>Yeterli paranız bulunmuyor!");
            return false;
        }

        town.deposit(amount);
        TownMember member = getMember(player.getUniqueId());
        if (member != null) {
            member.addContribution(amount);
            databaseManager.saveMemberAsync(member);
        }
        databaseManager.saveTownAsync(town);

        MessageUtil.sendMessage(player, "<green>Kasaba bankasına <yellow>" + amount + "$</yellow> yatırıldı. Yeni bakiye: <yellow>" + town.getBankBalance() + "$</yellow></green>");
        return true;
    }

    public boolean withdraw(Town town, Player player, double amount) {
        if (amount <= 0) return false;
        TownMember member = getMember(player.getUniqueId());
        if (member == null || !member.getRole().canWithdraw()) {
            MessageUtil.sendMessage(player, "<red>Kasaba bankasından para çekme yetkiniz yok!");
            return false;
        }

        if (!town.withdraw(amount)) {
            MessageUtil.sendMessage(player, "<red>Kasaba bankasında yeterli para yok! Mevcut: <yellow>" + town.getBankBalance() + "$</yellow></red>");
            return false;
        }

        vaultHook.giveMoney(player, amount);
        databaseManager.saveTownAsync(town);

        MessageUtil.sendMessage(player, "<green>Kasaba bankasından <yellow>" + amount + "$</yellow> çekildi. Kalan bakiye: <yellow>" + town.getBankBalance() + "$</yellow></green>");
        return true;
    }

    public boolean levelUp(Town town, Player player) {
        TownMember member = getMember(player.getUniqueId());
        if (member == null || !member.getRole().isMayor()) {
            MessageUtil.sendMessage(player, "<red>Sadece belediye başkanı kasabayı yükseltebilir!");
            return false;
        }

        int currentLevel = town.getLevel();
        double cost = currentLevel * 5000.0;

        if (town.getBankBalance() < cost) {
            MessageUtil.sendMessage(player, "<red>Kasaba kasasında yeterli bakiye yok! Gerekli: <yellow>" + cost + "$</yellow></red>");
            return false;
        }

        town.withdraw(cost);
        town.setLevel(currentLevel + 1);
        databaseManager.saveTownAsync(town);

        broadcastToTown(town.getUuid(), "<gold><b>Tebrikler!</b> Kasabamız <yellow>Seviye " + town.getLevel() + "</yellow> oldu!</gold>");
        return true;
    }

    public boolean isUnderTruce(UUID townUuid) {
        Long truce = townTruces.get(townUuid);
        return truce != null && System.currentTimeMillis() < truce;
    }

    public void setTruce(UUID townUuid, long hours) {
        long end = System.currentTimeMillis() + (hours * 3600000L);
        townTruces.put(townUuid, end);
        databaseManager.saveTruceAsync(townUuid, end);
    }

    public void broadcastToTown(UUID townUuid, String message) {
        for (TownMember member : getTownMembers(townUuid)) {
            Player p = Bukkit.getPlayer(member.getPlayerUuid());
            if (p != null && p.isOnline()) {
                MessageUtil.sendMessage(p, message);
            }
        }
    }
}
