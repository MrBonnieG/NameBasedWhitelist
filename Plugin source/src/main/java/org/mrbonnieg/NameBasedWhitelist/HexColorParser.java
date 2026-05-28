package org.mrbonnieg.NameBasedWhitelist;

import org.bukkit.ChatColor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HexColorParser {
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public static String parseHexColors(String message) {
        if (message == null || message.isEmpty()) return message;
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuilder result = new StringBuilder(message.length());
        int lastIndex = 0;
        while (matcher.find()) {
            result.append(message, lastIndex, matcher.start());
            String hex = matcher.group(1);
            result.append(toSpigotHex(hex));
            lastIndex = matcher.end();
        }
        result.append(message.substring(lastIndex));
        return ChatColor.translateAlternateColorCodes('&', result.toString());
    }

    private static String toSpigotHex(String hex) {
        StringBuilder builder = new StringBuilder("§x");
        for (int i = 0; i < hex.length(); i++) {
            builder.append('§').append(hex.charAt(i));
        }
        return builder.toString();
    }
}
