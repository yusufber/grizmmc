package com.grizmtech.perms.util;

import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)[&§][0-9A-FK-ORX]");

    private ColorUtil() {
    }

    @SuppressWarnings("deprecation")
    public static String colorize(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        Matcher matcher = HEX_PATTERN.matcher(input);
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append('§').append(c);
            }
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    /**
     * Prefix metninin görünür kısmının sonunda boşluk yoksa otomatik boşluk ekler
     * (Örn: "&4&l[KURUCU]&c" -> "&4&l[KURUCU] &c").
     */
    public static String ensurePrefixSpacing(String rawPrefix) {
        if (rawPrefix == null || rawPrefix.isEmpty()) {
            return "";
        }
        String visible = HEX_PATTERN.matcher(rawPrefix).replaceAll("");
        visible = STRIP_COLOR_PATTERN.matcher(visible).replaceAll("");
        if (visible.isEmpty() || visible.endsWith(" ")) {
            return rawPrefix;
        }
        // Sonunda renk kodları varsa boşluğu renk kodlarından önce veya sonuna ekle
        int trailingColorStart = rawPrefix.length();
        while (trailingColorStart >= 2) {
            char c1 = rawPrefix.charAt(trailingColorStart - 2);
            char c2 = Character.toLowerCase(rawPrefix.charAt(trailingColorStart - 1));
            if ((c1 == '&' || c1 == '§') && "0123456789abcdefklmnor".indexOf(c2) >= 0) {
                trailingColorStart -= 2;
            } else {
                break;
            }
        }
        if (trailingColorStart < rawPrefix.length()) {
            return rawPrefix.substring(0, trailingColorStart) + " " + rawPrefix.substring(trailingColorStart);
        }
        return rawPrefix + " ";
    }

    @SuppressWarnings("deprecation")
    public static ChatColor extractLastChatColor(String rawPrefix) {
        if (rawPrefix == null || rawPrefix.isEmpty()) {
            return ChatColor.GRAY;
        }
        ChatColor last = ChatColor.GRAY;
        for (int i = 0; i < rawPrefix.length() - 1; i++) {
            char c = rawPrefix.charAt(i);
            if (c == '&' || c == '§') {
                char code = Character.toLowerCase(rawPrefix.charAt(i + 1));
                ChatColor found = ChatColor.getByChar(code);
                if (found != null && found.isColor()) {
                    last = found;
                } else if (code == 'r') {
                    last = ChatColor.WHITE;
                }
                i++;
            }
        }
        return last;
    }

    public static String formatRemainingSeconds(long expiresAtEpochSec) {
        if (expiresAtEpochSec <= 0) {
            return "Süresiz";
        }
        long now = System.currentTimeMillis() / 1000L;
        long diff = expiresAtEpochSec - now;
        if (diff <= 0) {
            return "Süresi Doldu";
        }
        long days = diff / 86400L;
        long hours = (diff % 86400L) / 3600L;
        long mins = (diff % 3600L) / 60L;
        if (days > 0) {
            return days + " gün " + hours + " saat";
        }
        if (hours > 0) {
            return hours + " saat " + mins + " dk";
        }
        return Math.max(1L, mins) + " dakika";
    }
}
