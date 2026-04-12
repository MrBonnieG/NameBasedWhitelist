package org.mrbonnieg.NameBasedWhitelist;

import org.bukkit.ChatColor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HexColorParser {
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    public static String parseHexColors(String message) {
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer stringBuffer = new StringBuffer();

        while (matcher.find()) {
            String colorCode = matcher.group(1).toLowerCase();
            StringBuilder hexColor = new StringBuilder("§x");
            for (char c : colorCode.toCharArray()) {
                hexColor.append("§").append(c);
            }
            matcher.appendReplacement(stringBuffer, hexColor.toString());
        }
        matcher.appendTail(stringBuffer);
        return ChatColor.translateAlternateColorCodes('&', stringBuffer.toString());
    }
}
