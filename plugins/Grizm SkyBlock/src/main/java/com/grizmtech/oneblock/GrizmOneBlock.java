package com.grizmtech.oneblock;

import com.grizmtech.oneblock.commands.AdaAdminCommand;
import com.grizmtech.oneblock.commands.AdaCommand;
import com.grizmtech.oneblock.data.DataManager;
import com.grizmtech.oneblock.listeners.BlockBreakListener;
import com.grizmtech.oneblock.listeners.BossCombatListener;
import com.grizmtech.oneblock.listeners.CoopQuitListener;
import com.grizmtech.oneblock.listeners.HopperLimitListener;
import com.grizmtech.oneblock.listeners.InventoryClickListener;
import com.grizmtech.oneblock.listeners.IslandLimiterListener;
import com.grizmtech.oneblock.listeners.IslandProtectionListener;
import com.grizmtech.oneblock.listeners.IslandWorldRulesListener;
import com.grizmtech.oneblock.listeners.OreGeneratorListener;
import com.grizmtech.oneblock.listeners.StackerListener;
import com.grizmtech.oneblock.listeners.VoidProtectionListener;
import com.grizmtech.oneblock.listeners.GeneratorMinionListener;
import com.grizmtech.oneblock.hooks.GrizmOneBlockExpansion;
import com.grizmtech.oneblock.listeners.SkillListener;
import com.grizmtech.oneblock.managers.AntiMacroManager;
import com.grizmtech.oneblock.managers.AutoMinerManager;
import com.grizmtech.oneblock.managers.GeneratorMinionManager;
import com.grizmtech.oneblock.managers.BankManager;
import com.grizmtech.oneblock.managers.BiomeManager;
import com.grizmtech.oneblock.managers.BlockEventManager;
import com.grizmtech.oneblock.managers.BossManager;
import com.grizmtech.oneblock.managers.CrossServerManager;
import com.grizmtech.oneblock.managers.EconomyManager;
import com.grizmtech.oneblock.managers.GUIManager;
import com.grizmtech.oneblock.managers.HologramManager;
import com.grizmtech.oneblock.managers.IslandLevelManager;
import com.grizmtech.oneblock.managers.IslandManager;
import com.grizmtech.oneblock.managers.IslandSettingManager;
import com.grizmtech.oneblock.managers.IslandSocialManager;
import com.grizmtech.oneblock.managers.MissionManager;
import com.grizmtech.oneblock.managers.PhaseManager;
import com.grizmtech.oneblock.managers.RebirthManager;
import com.grizmtech.oneblock.managers.RoleManager;
import com.grizmtech.oneblock.managers.SkillManager;
import com.grizmtech.oneblock.managers.StackerManager;
import com.grizmtech.oneblock.managers.UpgradeManager;
import com.grizmtech.oneblock.world.BlockFailsafeTask;
import com.grizmtech.oneblock.world.VoidChunkGenerator;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;

public class GrizmOneBlock extends JavaPlugin {

    private DataManager dataManager;
    private PhaseManager phaseManager;
    private GUIManager guiManager;
    private IslandManager islandManager;
    private EconomyManager economyManager;
    private UpgradeManager upgradeManager;
    private IslandSettingManager islandSettingManager;
    private BossManager bossManager;
    private RebirthManager rebirthManager;
    private BankManager bankManager;
    private StackerManager stackerManager;
    private IslandLevelManager islandLevelManager;
    private RoleManager roleManager;
    private HologramManager hologramManager;
    private MissionManager missionManager;
    private BiomeManager biomeManager;
    private IslandSocialManager islandSocialManager;
    private AutoMinerManager autoMinerManager;
    private GeneratorMinionManager generatorMinionManager;
    private BlockEventManager blockEventManager;
    private SkillManager skillManager;
    private AntiMacroManager antiMacroManager;
    private CrossServerManager crossServerManager;

    @Override
    public void onEnable() {
        // Yöneticileri başlat
        dataManager = new DataManager(this);
        phaseManager = new PhaseManager();
        economyManager = new EconomyManager(this);
        upgradeManager = new UpgradeManager(this);
        islandSettingManager = new IslandSettingManager(this);
        bossManager = new BossManager(this);
        rebirthManager = new RebirthManager(this);
        bankManager = new BankManager(this);
        stackerManager = new StackerManager(this);
        islandLevelManager = new IslandLevelManager(this);
        roleManager = new RoleManager(this);
        hologramManager = new HologramManager(this);
        missionManager = new MissionManager(this);
        biomeManager = new BiomeManager(this);
        guiManager = new GUIManager(this);
        islandManager = new IslandManager(this);
        islandSocialManager = new IslandSocialManager(this);
        autoMinerManager = new AutoMinerManager(this);
        generatorMinionManager = new GeneratorMinionManager(this);
        blockEventManager = new BlockEventManager(this);
        skillManager = new SkillManager(this);
        antiMacroManager = new AntiMacroManager(this);
        crossServerManager = new CrossServerManager(this);

        // BungeeCord / Velocity Plugin Messaging Kanalı
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        // Dünyaları yükle ve hologramları başlat
        List<String> islandWorlds = dataManager.getAllIslandWorlds();
        for (String worldName : islandWorlds) {
            WorldCreator wc = new WorldCreator(worldName);
            wc.generator(new VoidChunkGenerator());
            World world = getServer().createWorld(wc);

            if (world != null) {
                world.setGameRule(org.bukkit.GameRule.DO_IMMEDIATE_RESPAWN, true);
                world.setGameRule(org.bukkit.GameRule.SPAWN_RADIUS, 0);
                try {
                    UUID ownerUUID = UUID.fromString(worldName.replace("island_", ""));
                    islandSettingManager.applyBorder(ownerUUID, world);
                    hologramManager.updateHologram(ownerUUID);
                } catch (Exception ignored) {}
                getLogger().info("Dünya yüklendi: " + worldName);
            }
        }

        // Failsafe Task: Her 3 saniyede bir merkez bloğu kontrol et
        new BlockFailsafeTask(this).runTaskTimer(this, 60L, 60L);

        // Event dinleyicilerini kayıt et
        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
        getServer().getPluginManager().registerEvents(new HopperLimitListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryClickListener(this), this);
        getServer().getPluginManager().registerEvents(new IslandProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new IslandWorldRulesListener(this), this);
        getServer().getPluginManager().registerEvents(new OreGeneratorListener(this), this);
        getServer().getPluginManager().registerEvents(new StackerListener(this), this);
        getServer().getPluginManager().registerEvents(new IslandLimiterListener(this), this);
        getServer().getPluginManager().registerEvents(new CoopQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new VoidProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new BossCombatListener(this), this);
        getServer().getPluginManager().registerEvents(new SkillListener(this), this);
        getServer().getPluginManager().registerEvents(new GeneratorMinionListener(this, generatorMinionManager), this);
        getServer().getPluginManager().registerEvents(crossServerManager, this);

        // PlaceholderAPI Entegrasyonu
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new GrizmOneBlockExpansion(this).register();
            getLogger().info("PlaceholderAPI kancası (Expansion) başarıyla yüklendi!");
        }

        // Komutları kayıt et
        AdaCommand adaCmd = new AdaCommand(this);
        if (getCommand("ada") != null) {
            getCommand("ada").setExecutor(adaCmd);
            getCommand("ada").setTabCompleter(adaCmd);
        }

        AdaAdminCommand adminCmd = new AdaAdminCommand(this);
        if (getCommand("adaadmin") != null) {
            getCommand("adaadmin").setExecutor(adminCmd);
            getCommand("adaadmin").setTabCompleter(adminCmd);
        }

        getLogger().info("GrizmOneBlock Enterprise Edition başarıyla aktif edildi!");
    }

    @Override
    public void onDisable() {
        if (hologramManager != null) {
            hologramManager.removeAll();
        }
        if (generatorMinionManager != null) {
            generatorMinionManager.saveMinions();
        }
        if (dataManager != null) {
            dataManager.saveConfig();
        }
        getLogger().info("GrizmOneBlock başarıyla devre dışı bırakıldı!");
    }

    public DataManager getDataManager() { return dataManager; }
    public PhaseManager getPhaseManager() { return phaseManager; }
    public GUIManager getGuiManager() { return guiManager; }
    public IslandManager getIslandManager() { return islandManager; }
    public EconomyManager getEconomyManager() { return economyManager; }
    public UpgradeManager getUpgradeManager() { return upgradeManager; }
    public IslandSettingManager getIslandSettingManager() { return islandSettingManager; }
    public BossManager getBossManager() { return bossManager; }
    public RebirthManager getRebirthManager() { return rebirthManager; }
    public BankManager getBankManager() { return bankManager; }
    public StackerManager getStackerManager() { return stackerManager; }
    public IslandLevelManager getIslandLevelManager() { return islandLevelManager; }
    public RoleManager getRoleManager() { return roleManager; }
    public HologramManager getHologramManager() { return hologramManager; }
    public MissionManager getMissionManager() { return missionManager; }
    public BiomeManager getBiomeManager() { return biomeManager; }
    public IslandSocialManager getIslandSocialManager() { return islandSocialManager; }
    public AutoMinerManager getAutoMinerManager() { return autoMinerManager; }
    public GeneratorMinionManager getGeneratorMinionManager() { return generatorMinionManager; }
    public BlockEventManager getBlockEventManager() { return blockEventManager; }
    public SkillManager getSkillManager() { return skillManager; }
    public AntiMacroManager getAntiMacroManager() { return antiMacroManager; }
    public CrossServerManager getCrossServerManager() { return crossServerManager; }

    @Override
    public org.bukkit.generator.ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        return new VoidChunkGenerator();
    }
}
