package com.grizmtech.auth.commands;

import com.grizmtech.auth.GrizmAuth;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LoginCommand implements CommandExecutor {

    private final GrizmAuth plugin;

    public LoginCommand(GrizmAuth plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Bu komut yalnizca oyuncular tarafindan kullanilabilir.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(plugin.getSettings().codePrompt);
            return true;
        }

        // /grizmauth <kod> komutuna yonlendir
        player.performCommand("grizmauth " + args[0]);
        return true;
    }
}
