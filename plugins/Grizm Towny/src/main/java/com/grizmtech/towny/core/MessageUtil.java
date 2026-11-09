package com.grizmtech.towny.core;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;

public final class MessageUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static String prefix = "<gradient:#ff8a00:#e52e71><b>GRIZM TOWNY</b></gradient> <dark_gray>»</dark_gray> ";

    private MessageUtil() {}

    public static void setPrefix(String newPrefix) {
        prefix = newPrefix;
    }

    public static String getPrefix() {
        return prefix;
    }

    public static Component parse(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return MINI_MESSAGE.deserialize(text);
    }

    public static Component parseWithPrefix(String text) {
        return MINI_MESSAGE.deserialize(prefix + text);
    }

    public static void sendMessage(CommandSender sender, String message) {
        if (sender != null && message != null && !message.isEmpty()) {
            sender.sendMessage(parseWithPrefix(message));
        }
    }

    public static void sendRawMessage(CommandSender sender, String message) {
        if (sender != null && message != null && !message.isEmpty()) {
            sender.sendMessage(parse(message));
        }
    }

    public static void sendActionBar(Player player, String message) {
        if (player != null && message != null) {
            player.sendActionBar(parse(message));
        }
    }

    public static void sendTitle(Player player, String title, String subtitle, int fadeInTicks, int stayTicks, int fadeOutTicks) {
        if (player == null) return;
        Title.Times times = Title.Times.times(
                Duration.ofMillis(fadeInTicks * 50L),
                Duration.ofMillis(stayTicks * 50L),
                Duration.ofMillis(fadeOutTicks * 50L)
        );
        Title titleObj = Title.title(parse(title), parse(subtitle), times);
        player.showTitle(titleObj);
    }

    public static void broadcast(String message) {
        Component comp = parseWithPrefix(message);
        Bukkit.broadcast(comp);
    }
}
