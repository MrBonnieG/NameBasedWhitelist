package org.mrbonnieg.namebasedwhitelist;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.mrbonnieg.NameBasedWhitelist.HexColorParser;

public class Events implements Listener {
    private final Main plugin;

    public Events(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("settings.enable")) return;

        String username = event.getPlayer().getName().toLowerCase();
        if(!plugin.getStorage().getPlayers().contains(username)) {
            event.getPlayer().kickPlayer(HexColorParser.parseHexColors(plugin.getConfig().getString("messages.kick-message")));
        }
    }
}
