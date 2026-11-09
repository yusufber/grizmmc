package com.prizmarin.crates;

import com.prizmarin.crates.command.CrateCommand;
import com.prizmarin.crates.command.GiveKeyCommand;
import com.prizmarin.crates.hologram.CrateHologramManager;
import com.prizmarin.crates.key.KeyManager;
import com.prizmarin.crates.listener.CrateInteractListener;
import com.prizmarin.crates.listener.MenuProtectionListener;
import com.prizmarin.crates.reward.RewardManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class CratesPlugin extends JavaPlugin {

    private static CratesPlugin instance;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    private KeyManager keyManager;
    private RewardManager rewardManager;
    private CrateHologramManager hologramManager;
    private CrateRegistry crateRegistry;

    @Override
    public void onEnable() {
        instance = this;

        // Config setup
        saveDefaultConfig();

        // Managers
        this.keyManager = new KeyManager(this);
        this.rewardManager = new RewardManager(this);
        this.rewardManager.loadRewards();

        this.hologramManager = new CrateHologramManager(this);
        this.crateRegistry = new CrateRegistry(this);
        this.crateRegistry.load();

        // Register Commands
        CrateCommand crateCmd = new CrateCommand(this);
        Objects.requireNonNull(getCommand("kasakoy")).setExecutor(crateCmd);
        Objects.requireNonNull(getCommand("kasakoy")).setTabCompleter(crateCmd);

        Objects.requireNonNull(getCommand("kasasil")).setExecutor(crateCmd);
        Objects.requireNonNull(getCommand("kasasil")).setTabCompleter(crateCmd);

        Objects.requireNonNull(getCommand("kasalist")).setExecutor(crateCmd);
        Objects.requireNonNull(getCommand("kasalist")).setTabCompleter(crateCmd);

        Objects.requireNonNull(getCommand("kasareload")).setExecutor(crateCmd);
        Objects.requireNonNull(getCommand("kasareload")).setTabCompleter(crateCmd);

        GiveKeyCommand giveKeyCmd = new GiveKeyCommand(this);
        Objects.requireNonNull(getCommand("anahtarver")).setExecutor(giveKeyCmd);
        Objects.requireNonNull(getCommand("anahtarver")).setTabCompleter(giveKeyCmd);

        // Register Listeners
        getServer().getPluginManager().registerEvents(new CrateInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuProtectionListener(), this);

        getLogger().info("GrizmCrates 1.0.0 basariyla aktif edildi!");
    }

    @Override
    public void onDisable() {
        if (crateRegistry != null) {
            crateRegistry.save();
        }
        getLogger().info("GrizmCrates devre disi birakildi!");
    }

    public void reloadPlugin() {
        reloadConfig();
        if (rewardManager != null) {
            rewardManager.loadRewards();
        }
        if (crateRegistry != null) {
            crateRegistry.load();
        }
    }

    public static CratesPlugin getInstance() {
        return instance;
    }

    public KeyManager getKeyManager() {
        return keyManager;
    }

    public RewardManager getRewardManager() {
        return rewardManager;
    }

    public CrateHologramManager getHologramManager() {
        return hologramManager;
    }

    public CrateRegistry getCrateRegistry() {
        return crateRegistry;
    }

    public Component getPrefix() {
        String prefixStr = getConfig().getString("prefix", "<gradient:#ff8a00:#e52e71><b>[GrizmCrates]</b></gradient> <gray>»</gray> ");
        return miniMessage.deserialize(prefixStr);
    }

    public Component getPrefixedMessage(String configKey) {
        String msg = getConfig().getString("messages." + configKey, "<red>Mesaj bulunamadı: " + configKey + "</red>");
        return getPrefix().append(miniMessage.deserialize(msg));
    }
}
