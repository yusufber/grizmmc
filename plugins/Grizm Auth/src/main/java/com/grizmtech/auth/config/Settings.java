package com.grizmtech.auth.config;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Settings {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    // API
    public String apiUrl;
    public String fallbackUrl;
    public String serverKey;
    public int timeoutSeconds;

    // Security
    public boolean requireLauncher;
    public boolean allowIpVerification;
    public boolean allowCodeLogin;
    public int sessionTimeoutSeconds;
    public Set<String> whitelistBypass = new HashSet<>();

    // Protection
    public boolean freezeUnauthenticated;
    public boolean blindness;
    public int loginTimeoutSeconds;

    // Sounds
    public String soundSuccess;
    public String soundError;
    public String soundPrompt;

    // Messages
    public String prefix;
    public String kickLauncherRequired;
    public String kickNotRegistered;
    public String welcomeTitle;
    public String welcomeSubtitle;
    public String actionbarVerified;
    public String codePrompt;
    public String codeSuccess;
    public String codeInvalid;
    public String timeoutKick;
    public String commandBlocked;
    public String chatBlocked;
    public String alreadyLoggedIn;
    public String reloadSuccess;
    public String noPermission;

    public void load(FileConfiguration config) {
        apiUrl = config.getString("api.url", "https://www.grizm.tech/mc/api/auth.php");
        fallbackUrl = config.getString("api.fallback-url", "https://grizmmc.dlmtd.com/api/auth.php");
        serverKey = config.getString("api.server-key", "grizm_mc_sec_auth_9471b028");
        timeoutSeconds = config.getInt("api.timeout-seconds", 5);

        requireLauncher = config.getBoolean("security.require-launcher", true);
        allowIpVerification = config.getBoolean("security.allow-ip-verification", false);
        allowCodeLogin = config.getBoolean("security.allow-code-login", true);
        sessionTimeoutSeconds = config.getInt("security.session-timeout-seconds", 300);

        whitelistBypass.clear();
        List<String> bypassList = config.getStringList("security.whitelist-bypass");
        for (String s : bypassList) {
            whitelistBypass.add(s.toLowerCase());
        }

        freezeUnauthenticated = config.getBoolean("protection.freeze-unauthenticated", true);
        blindness = config.getBoolean("protection.blindness", true);
        loginTimeoutSeconds = config.getInt("protection.login-timeout-seconds", 60);

        soundSuccess = config.getString("sounds.success", "ENTITY_PLAYER_LEVELUP");
        soundError = config.getString("sounds.error", "ENTITY_VILLAGER_NO");
        soundPrompt = config.getString("sounds.prompt", "BLOCK_NOTE_BLOCK_PLING");

        prefix = format(config.getString("messages.prefix", "&8[&f&lGRIZM&8] "));
        kickLauncherRequired = format(config.getString("messages.kick-launcher-required", "&cBu sunucuya yalnizca GrizmMC Launcher ile giris yapabilirsiniz."));
        kickNotRegistered = format(config.getString("messages.kick-not-registered", "&cHesabiniz web sitesinde kayitli degil."));
        welcomeTitle = format(config.getString("messages.welcome-title", "&f&lGRIZM&7&lMC"));
        welcomeSubtitle = format(config.getString("messages.welcome-subtitle", "&a✔ Launcher ile dogrulandiniz!"));
        actionbarVerified = format(config.getString("messages.actionbar-verified", "&a✔ GrizmMC Launcher Dogrulamasi Aktif"));
        codePrompt = format(config.getString("messages.code-prompt", "&eGrizmID kodunuzu yaziniz: /login <kod>"));
        codeSuccess = format(prefix + config.getString("messages.code-success", "&aHesabiniz basariyla dogrulandi!"));
        codeInvalid = format(prefix + config.getString("messages.code-invalid", "&cGecersiz giris kodu!"));
        timeoutKick = format(config.getString("messages.timeout-kick", "&cSureniz doldu!"));
        commandBlocked = format(prefix + config.getString("messages.command-blocked", "&cGiris yapmadan komut kullanamazsiniz!"));
        chatBlocked = format(prefix + config.getString("messages.chat-blocked", "&cGiris yapmadan sohbete yazamazsiniz!"));
        alreadyLoggedIn = format(prefix + config.getString("messages.already-logged-in", "&aZaten giris yapmissiniz."));
        reloadSuccess = format(prefix + config.getString("messages.reload-success", "&aYapilandirma yeniden yuklendi!"));
        noPermission = format(prefix + config.getString("messages.no-permission", "&cYetkiniz yok!"));
    }

    public static String format(String message) {
        if (message == null) return "";

        // Hex Renk Kodları (&#FFFFFF)
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append('§').append(c);
            }
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);

        // Standart Bukkit Renk Kodları (&a, &l vb.)
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }
}
