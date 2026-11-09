package com.grizmtech.towny.teleport;

import com.grizmtech.towny.claim.ClaimChunk;
import com.grizmtech.towny.claim.ClaimManager;
import com.grizmtech.towny.combat.CombatTagManager;
import com.grizmtech.towny.core.ConfigManager;
import com.grizmtech.towny.core.MessageUtil;
import com.grizmtech.towny.town.Town;
import com.grizmtech.towny.town.TownManager;
import com.grizmtech.towny.war.SiegeWar;
import com.grizmtech.towny.war.WarManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TPAManager {

    private final ConfigManager configManager;
    private final TownManager townManager;
    private final ClaimManager claimManager;
    private final WarManager warManager;
    private final CombatTagManager combatTagManager;

    // Hedef Oyuncu UUID -> TPARequest
    private final Map<UUID, TPARequest> pendingRequests = new ConcurrentHashMap<>();

    public TPAManager(ConfigManager configManager, TownManager townManager, ClaimManager claimManager,
                      WarManager warManager, CombatTagManager combatTagManager) {
        this.configManager = configManager;
        this.townManager = townManager;
        this.claimManager = claimManager;
        this.warManager = warManager;
        this.combatTagManager = combatTagManager;
    }

    public void sendRequest(Player sender, Player target, TPARequest.Type type) {
        if (sender.getUniqueId().equals(target.getUniqueId())) {
            MessageUtil.sendMessage(sender, "<red>Kendinize ışınlanma isteği gönderemezsiniz!</red>");
            return;
        }

        if (combatTagManager.isTagged(sender.getUniqueId())) {
            MessageUtil.sendMessage(sender, "<red>Savaş halindeyken ışınlanma isteği gönderemezsiniz!</red>");
            return;
        }

        TPARequest request = new TPARequest(sender.getUniqueId(), target.getUniqueId(), type);
        pendingRequests.put(target.getUniqueId(), request);

        String typeDesc = (type == TPARequest.Type.TPA) ? "yanınıza ışınlanmak istiyor" : "sizi yanına çağırmak istiyor";
        MessageUtil.sendMessage(sender, "<yellow>" + target.getName() + " oyuncusuna ışınlanma isteği gönderildi.</yellow>");

        String msg = String.format("<gold><b>GRIZM TOWNY</b></gold> <gray>»</gray> <yellow><b>%s</b></yellow> %s!\n" +
                        " <click:run_command:'/tpaccept'><hover:show_text:'<green>İsteği Kabul Et'><green><b>[✔ Kabul Et]</b></green></hover></click>      " +
                        "<click:run_command:'/tpdeny'><hover:show_text:'<red>İsteği Reddet'><red><b>[✖ Reddet]</b></red></hover></click>\n" +
                        "<dark_gray>(İstek %d saniye sonra geçerliliğini yitirecektir)</dark_gray>",
                sender.getName(), typeDesc, configManager.getTpaTimeoutSeconds());

        MessageUtil.sendRawMessage(target, msg);
    }

    public void acceptRequest(Player target) {
        TPARequest request = pendingRequests.get(target.getUniqueId());
        if (request == null || request.isExpired(configManager.getTpaTimeoutSeconds())) {
            pendingRequests.remove(target.getUniqueId());
            MessageUtil.sendMessage(target, "<red>Bekleyen geçerli bir ışınlanma isteğiniz bulunmuyor!</red>");
            return;
        }

        Player sender = Bukkit.getPlayer(request.getSenderUuid());
        if (sender == null || !sender.isOnline()) {
            pendingRequests.remove(target.getUniqueId());
            MessageUtil.sendMessage(target, "<red>İsteği gönderen oyuncu oyunda değil!</red>");
            return;
        }

        // Kimin kime ışınlanacağını belirle
        Player movingPlayer = (request.getType() == TPARequest.Type.TPA) ? sender : target;
        Player destinationPlayer = (request.getType() == TPARequest.Type.TPA) ? target : sender;

        if (combatTagManager.isTagged(movingPlayer.getUniqueId()) || combatTagManager.isTagged(destinationPlayer.getUniqueId())) {
            MessageUtil.sendMessage(target, "<red>Taraflardan biri savaş halinde olduğu için ışınlanma iptal edildi!</red>");
            MessageUtil.sendMessage(sender, "<red>Taraflardan biri savaş halinde olduğu için ışınlanma iptal edildi!</red>");
            pendingRequests.remove(target.getUniqueId());
            return;
        }

        Location destLoc = destinationPlayer.getLocation();

        // 1. Tuzak Koruması: Altında tam blok yoksa, lav veya boşluksa iptal et
        Block groundBlock = destLoc.clone().subtract(0, 1, 0).getBlock();
        if (groundBlock.getType() == Material.LAVA || groundBlock.getType() == Material.VOID_AIR || groundBlock.getType() == Material.AIR) {
            MessageUtil.sendMessage(target, "<red>Işınlanma noktası güvensiz (lav veya boşluk tuzağı) olduğu için iptal edildi!</red>");
            MessageUtil.sendMessage(sender, "<red>Işınlanma noktası güvensiz (lav veya boşluk tuzağı) olduğu için iptal edildi!</red>");
            pendingRequests.remove(target.getUniqueId());
            return;
        }

        // 2. Kuşatma ve Düşman Arazi Koruması
        ClaimChunk claim = claimManager.getClaim(destLoc);
        if (claim != null) {
            Town destTown = townManager.getTown(claim.getTownUuid());
            if (destTown != null) {
                // Aktif kuşatma alanı mı?
                if (warManager.isBesieged(destTown.getUuid())) {
                    MessageUtil.sendMessage(movingPlayer, "<red>Hedef bölge aktif kuşatma altında olduğu için ışınlanma engellendi!</red>");
                    pendingRequests.remove(target.getUniqueId());
                    return;
                }

                // Düşman toprağı mı?
                Town movingTown = townManager.getTownByPlayer(movingPlayer.getUniqueId());
                if (movingTown != null && !movingTown.getUuid().equals(destTown.getUuid())) {
                    SiegeWar war = warManager.getWarByTown(destTown.getUuid());
                    if (war != null && (war.getAttackerTownUuid().equals(movingTown.getUuid()) || war.getDefenderTownUuid().equals(movingTown.getUuid()))) {
                        MessageUtil.sendMessage(movingPlayer, "<red>Düşman kasabanın arazisine TPA ile sızamazsınız!</red>");
                        pendingRequests.remove(target.getUniqueId());
                        return;
                    }
                }
            }
        }

        pendingRequests.remove(target.getUniqueId());

        movingPlayer.teleportAsync(destLoc).thenAccept(success -> {
            if (success) {
                MessageUtil.sendMessage(movingPlayer, "<green>Işınlanma başarıyla tamamlandı!</green>");
                MessageUtil.sendMessage(destinationPlayer, "<green>Işınlanma başarıyla tamamlandı!</green>");
            }
        });
    }

    public void denyRequest(Player target) {
        TPARequest request = pendingRequests.remove(target.getUniqueId());
        if (request == null) {
            MessageUtil.sendMessage(target, "<red>Bekleyen bir ışınlanma isteğiniz bulunmuyor!</red>");
            return;
        }

        MessageUtil.sendMessage(target, "<yellow>Işınlanma isteği reddedildi.</yellow>");
        Player sender = Bukkit.getPlayer(request.getSenderUuid());
        if (sender != null && sender.isOnline()) {
            MessageUtil.sendMessage(sender, "<red>" + target.getName() + " ışınlanma isteğinizi reddetti.</red>");
        }
    }
}
