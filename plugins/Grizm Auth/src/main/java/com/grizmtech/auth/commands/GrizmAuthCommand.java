package com.grizmtech.auth.commands;

import com.grizmtech.auth.GrizmAuth;
import com.grizmtech.auth.config.Settings;
import com.grizmtech.auth.model.UserProfileData;
import com.grizmtech.auth.session.SessionManager;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GrizmAuthCommand implements CommandExecutor, TabCompleter {

    private final GrizmAuth plugin;
    private final Settings settings;
    private final SessionManager sessionManager;

    public GrizmAuthCommand(GrizmAuth plugin) {
        this.plugin = plugin;
        this.settings = plugin.getSettings();
        this.sessionManager = plugin.getSessionManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Settings.format("&8--------------------------------------------------"));
            sender.sendMessage(Settings.format("&f&lGRIZM&7&lMC NETWORK &8» &fGrizmAuth v" + plugin.getDescription().getVersion()));
            sender.sendMessage(Settings.format("&7Resmi Launcher ve Web API Kimlik Dogrulama Sistemi"));
            if (sender.hasPermission("grizmauth.admin")) {
                sender.sendMessage(Settings.format("&e/grizmauth reload &8- &7Yapilandirmayi yeniden yukler"));
                sender.sendMessage(Settings.format("&e/grizmauth status &8- &7API ve aktif oturum durumunu gosterir"));
                sender.sendMessage(Settings.format("&e/grizmauth check <oyuncu> &8- &7Oyuncunun durumunu sorgular"));
            }
            if (sender instanceof Player player && !sessionManager.isAuthenticated(player)) {
                sender.sendMessage(Settings.format("&a/login <kod> &8- &7Web profilinizdeki 6 haneli kodu girin"));
            }
            sender.sendMessage(Settings.format("&8--------------------------------------------------"));
            return true;
        }

        String sub = args[0].toLowerCase();

        // Admin alt komutlari icin eger gonderici Player ise dogrulanmis olmasi zorunludur
        if ((sub.equals("reload") || sub.equals("status") || sub.equals("check"))
                && sender instanceof Player p && !sessionManager.isAuthenticated(p)) {
            sender.sendMessage(settings.commandBlocked);
            return true;
        }

        // 1. RELOAD
        if (sub.equals("reload")) {
            if (!sender.hasPermission("grizmauth.admin")) {
                sender.sendMessage(settings.noPermission);
                return true;
            }

            plugin.reloadConfig();
            settings.load(plugin.getConfig());
            sender.sendMessage(settings.reloadSuccess);
            return true;
        }

        // 2. STATUS
        if (sub.equals("status")) {
            if (!sender.hasPermission("grizmauth.admin")) {
                sender.sendMessage(settings.noPermission);
                return true;
            }

            sender.sendMessage(settings.prefix + "§7Web API durumu sorgulaniyor...");
            plugin.getApiClient().checkHealthAsync().thenAccept(online -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(Settings.format("&8--------------------------------------------------"));
                    sender.sendMessage(Settings.format("&f&lGRIZM AUTH DURUM RAPORU"));
                    sender.sendMessage(Settings.format("&7Web API Adresi: &f" + settings.apiUrl));
                    sender.sendMessage(Settings.format("&7Web API Durumu: " + (online ? "&a● Cevrimici" : "&c● Baglanti Yok")));
                    sender.sendMessage(Settings.format("&7Launcher Zorunlulugu: " + (settings.requireLauncher ? "&aAcik" : "&eKapali (Kod Modu)")));
                    sender.sendMessage(Settings.format("&7Sunucudaki Dogrulanmis Oyuncular: &e" + sessionManager.getAuthenticatedCount() + " &7/ &f" + Bukkit.getOnlinePlayers().size()));
                    sender.sendMessage(Settings.format("&8--------------------------------------------------"));
                });
            });
            return true;
        }

        // 3. CHECK
        if (sub.equals("check")) {
            if (!sender.hasPermission("grizmauth.admin")) {
                sender.sendMessage(settings.noPermission);
                return true;
            }

            if (args.length < 2) {
                sender.sendMessage(settings.prefix + "§cKullanim: /grizmauth check <oyuncu>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(settings.prefix + "§cOyuncu bulunamadi veya cevrimdisi.");
                return true;
            }

            UserProfileData profile = sessionManager.getProfile(target.getUniqueId());
            sender.sendMessage(Settings.format("&8--------------------------------------------------"));
            sender.sendMessage(Settings.format("&f&lOYUNCU AUTH BILGISI: &e" + target.getName()));
            if (profile != null) {
                sender.sendMessage(Settings.format("&7Durum: &a✔ Dogrulanmis"));
                sender.sendMessage(Settings.format("&7GrizmID: &f" + profile.getFullName()));
                sender.sendMessage(Settings.format("&7E-posta: &f" + (profile.getEmail().isEmpty() ? "Belirtilmedi" : profile.getEmail())));
                sender.sendMessage(Settings.format("&7Oturum ID: &e" + profile.getSessionId()));
            } else {
                sender.sendMessage(Settings.format("&7Durum: &c✖ Dogrulanmamis (Giris Bekliyor)"));
            }
            sender.sendMessage(Settings.format("&8--------------------------------------------------"));
            return true;
        }

        // 4. CODE DOGRULAMA (Oyuncu)
        if (sender instanceof Player player) {
            String code = sub.equals("code") && args.length > 1 ? args[1] : args[0];

            if (sessionManager.isAuthenticated(player)) {
                player.sendMessage(settings.alreadyLoggedIn);
                return true;
            }

            player.sendMessage(settings.prefix + "§7Kod dogrulaniyor...");
            plugin.getApiClient().verifyCodeAsync(player.getName(), code).thenAccept(result -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (result != null && result.isSuccess() && result.isAuthenticated()) {
                        sessionManager.authenticate(player, result.getProfile());
                        player.removePotionEffect(PotionEffectType.BLINDNESS);
                        player.sendMessage(settings.codeSuccess);

                        try {
                            player.playSound(player.getLocation(), Sound.valueOf(settings.soundSuccess), 1f, 1f);
                        } catch (Exception ignored) {
                        }
                    } else {
                        player.sendMessage(settings.codeInvalid);
                        try {
                            player.playSound(player.getLocation(), Sound.valueOf(settings.soundError), 1f, 1f);
                        } catch (Exception ignored) {
                        }
                    }
                });
            });
            return true;
        }

        sender.sendMessage(settings.prefix + "§cBilinmeyen komut. /grizmauth yaziniz.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1 && sender.hasPermission("grizmauth.admin")) {
            for (String s : Arrays.asList("reload", "status", "check")) {
                if (s.startsWith(args[0].toLowerCase())) {
                    list.add(s);
                }
            }
        }
        return list;
    }
}
