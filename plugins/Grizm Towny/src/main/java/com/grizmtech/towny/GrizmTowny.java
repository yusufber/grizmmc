package com.grizmtech.towny;

import com.grizmtech.towny.claim.ClaimManager;
import com.grizmtech.towny.combat.CombatTagManager;
import com.grizmtech.towny.commands.TeleportCommands;
import com.grizmtech.towny.commands.TownCommand;
import com.grizmtech.towny.commands.WarCommand;
import com.grizmtech.towny.core.ConfigManager;
import com.grizmtech.towny.core.MessageUtil;
import com.grizmtech.towny.hooks.TownyPlaceholderExpansion;
import com.grizmtech.towny.hooks.VaultHook;
import com.grizmtech.towny.listeners.*;
import com.grizmtech.towny.storage.DatabaseManager;
import com.grizmtech.towny.teleport.RTPManager;
import com.grizmtech.towny.teleport.TPAManager;
import com.grizmtech.towny.teleport.WarpManager;
import com.grizmtech.towny.town.TownManager;
import com.grizmtech.towny.visual.BorderParticleTask;
import com.grizmtech.towny.visual.UpkeepTask;
import com.grizmtech.towny.war.BlockRollbackManager;
import com.grizmtech.towny.war.WarManager;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class GrizmTowny extends JavaPlugin {

    private static GrizmTowny instance;

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private VaultHook vaultHook;
    private TownManager townManager;
    private CombatTagManager combatTagManager;
    private BlockRollbackManager rollbackManager;
    private ClaimManager claimManager;
    private WarManager warManager;
    private RTPManager rtpManager;
    private WarpManager warpManager;
    private TPAManager tpaManager;
    private BorderParticleTask borderParticleTask;
    private UpkeepTask upkeepTask;

    @Override
    public void onEnable() {
        instance = this;

        // 1. Yapılandırma ve Çekirdek
        this.configManager = new ConfigManager(this);

        // 2. Veritabanı ve Depolama
        this.databaseManager = new DatabaseManager(this, configManager);

        // 3. Vault Entegrasyonu
        this.vaultHook = new VaultHook(this);

        // 4. Kasaba ve Üye Yöneticisi
        this.townManager = new TownManager(this, databaseManager, configManager, vaultHook);

        // 5. Combat Tag ve Geri Alma (Rollback)
        this.combatTagManager = new CombatTagManager(this);
        this.rollbackManager = new BlockRollbackManager(this);

        // 6. Arazi (Claim) ve Parsel Yöneticisi
        this.claimManager = new ClaimManager(this, databaseManager, configManager, townManager, vaultHook);

        // 7. Savaş ve Kuşatma Motoru
        this.warManager = new WarManager(this, databaseManager, configManager, townManager, claimManager, rollbackManager);
        this.claimManager.setWarManager(warManager);

        // 8. Işınlanma Motoru (RTP, Warps, TPA)
        this.rtpManager = new RTPManager(this, configManager, claimManager, combatTagManager);
        this.warpManager = new WarpManager(this, databaseManager, townManager, warManager, combatTagManager, vaultHook);
        this.tpaManager = new TPAManager(configManager, townManager, claimManager, warManager, combatTagManager);

        // 9. Görsel Efektler ve Görevler
        this.borderParticleTask = new BorderParticleTask(this, claimManager, townManager, warManager);
        this.upkeepTask = new UpkeepTask(this, townManager, claimManager, databaseManager, configManager);

        // 10. Olay Dinleyicileri (Listeners)
        registerListeners();

        // 11. Komutlar
        registerCommands();

        // 12. PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new TownyPlaceholderExpansion(this, townManager, claimManager, warManager, combatTagManager).register();
            getLogger().info("PlaceholderAPI entegrasyonu başarıyla etkinleştirildi.");
        }

        getLogger().info("=================================================");
        getLogger().info(" GrizmTowny (Paper 1.21.4) basariyla yuklendi!");
        getLogger().info(" Bütünleşik Towny, Siege War & Teleport Motoru Aktif");
        getLogger().info("=================================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("GrizmTowny kapatılıyor, veriler güvenceye alınıyor...");

        if (borderParticleTask != null) borderParticleTask.shutdown();
        if (upkeepTask != null) upkeepTask.shutdown();
        if (warManager != null) warManager.shutdown();
        if (combatTagManager != null) combatTagManager.shutdown();
        if (rollbackManager != null) rollbackManager.shutdown();
        if (databaseManager != null) databaseManager.shutdown();

        getLogger().info("GrizmTowny güvenli bir şekilde kapatıldı.");
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new ClaimProtectionListener(claimManager, townManager, warManager, combatTagManager, rollbackManager, configManager), this);
        pm.registerEvents(new PlayerMoveListener(claimManager, townManager, warManager, configManager), this);
        pm.registerEvents(new CombatListener(combatTagManager, warManager, configManager), this);
        pm.registerEvents(new SiegeBannerListener(warManager), this);
        pm.registerEvents(new InventoryClickListener(townManager, claimManager, warManager, warpManager, databaseManager, vaultHook), this);
    }

    private void registerCommands() {
        TownCommand townCmd = new TownCommand(townManager, claimManager, warManager, warpManager, databaseManager, borderParticleTask);
        registerCmd("town", townCmd);

        WarCommand warCmd = new WarCommand(warManager, townManager);
        registerCmd("war", warCmd);

        TeleportCommands tpCmd = new TeleportCommands(rtpManager, warpManager, tpaManager, townManager);
        registerCmd("rtp", tpCmd);
        registerCmd("warp", tpCmd);
        registerCmd("setwarp", tpCmd);
        registerCmd("delwarp", tpCmd);
        registerCmd("tpa", tpCmd);
        registerCmd("tpahere", tpCmd);
        registerCmd("tpaccept", tpCmd);
        registerCmd("tpdeny", tpCmd);
    }

    private void registerCmd(String name, org.bukkit.command.CommandExecutor executor) {
        PluginCommand cmd = getCommand(name);
        if (cmd != null) {
            cmd.setExecutor(executor);
            if (executor instanceof org.bukkit.command.TabCompleter completer) {
                cmd.setTabCompleter(completer);
            }
        } else {
            getLogger().warning("Komut kaydedilemedi (plugin.yml içinde tanımlı değil): " + name);
        }
    }

    public static GrizmTowny getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() { return configManager; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public VaultHook getVaultHook() { return vaultHook; }
    public TownManager getTownManager() { return townManager; }
    public CombatTagManager getCombatTagManager() { return combatTagManager; }
    public ClaimManager getClaimManager() { return claimManager; }
    public WarManager getWarManager() { return warManager; }
    public RTPManager getRtpManager() { return rtpManager; }
    public WarpManager getWarpManager() { return warpManager; }
    public TPAManager getTpaManager() { return tpaManager; }
}
