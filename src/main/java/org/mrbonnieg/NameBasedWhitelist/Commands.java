package org.mrbonnieg.NameBasedWhitelist;

import com.google.common.collect.Lists;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class Commands implements CommandExecutor, TabCompleter {
    private final Main plugin;

    public Commands(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String noPermissions = HexColorParser.parseHexColors(plugin.getConfig().getString("messages.no-permissions"));
        String playerAdd = HexColorParser.parseHexColors(plugin.getConfig().getString("messages.player-add"));
        String playerRemove = HexColorParser.parseHexColors(plugin.getConfig().getString("messages.player-remove"));
        String playerAlreadyExists = HexColorParser.parseHexColors(plugin.getConfig().getString("messages.player-already-exists"));
        String playerNotFound = HexColorParser.parseHexColors(plugin.getConfig().getString("messages.player-not-found"));
        String pluginReload = HexColorParser.parseHexColors(plugin.getConfig().getString("messages.plugin-reload"));
        String pluginEnable = HexColorParser.parseHexColors(plugin.getConfig().getString("messages.plugin-enable"));
        String pluginDisable = HexColorParser.parseHexColors(plugin.getConfig().getString("messages.plugin-disable"));
        String pluginUsage = HexColorParser.parseHexColors(plugin.getConfig().getString("messages.plugin-usage"));

        if (args.length == 0) {
            sender.sendMessage(pluginUsage);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                if (!hasManagePermission(sender)) {
                    sender.sendMessage(noPermissions);
                    return true;
                } else {
                    plugin.reloadConfig();
                    sender.sendMessage(pluginReload);
                    return true;
                }
            case "add":
                if (!hasManagePermission(sender)) {
                    sender.sendMessage(noPermissions);
                    return true;
                } else {
                    String playerName = args[1].toLowerCase();
                    if (plugin.getStorage().getPlayers().contains(playerName)) {
                        sender.sendMessage(playerAlreadyExists.replace("%player%", playerName));
                        return true;
                    } else {
                        plugin.getStorage().addPlayer(playerName);
                        sender.sendMessage(playerAdd.replace("%player%", playerName));
                        return true;
                    }
                }
            case "remove":
                if (!hasModifyPermission(sender)) {
                    sender.sendMessage(noPermissions);
                    return true;
                } else {
                    String playerName = args[1].toLowerCase();
                    if (!plugin.getStorage().getPlayers().contains(playerName)) {
                        sender.sendMessage(playerNotFound.replace("%player%", playerName));
                        return true;
                    } else {
                        plugin.getStorage().removePlayer(playerName);
                        sender.sendMessage(playerRemove.replace("%player%", playerName));
                        return true;
                    }
                }
            case "enable":
                if (!hasManagePermission(sender)) {
                    sender.sendMessage(noPermissions);
                    return true;
                } else {
                    plugin.getConfig().set("settings.enable", true);
                    plugin.saveConfig();
                    sender.sendMessage(pluginEnable);
                    return true;
                }
            case "disable":
                if (!hasManagePermission(sender)) {
                    sender.sendMessage(noPermissions);
                    return true;
                } else {
                    plugin.getConfig().set("settings.enable", false);
                    plugin.saveConfig();
                    sender.sendMessage(pluginDisable);
                    return true;
                }
            default:
                sender.sendMessage(pluginUsage);
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Lists.newArrayList("add", "remove", "enable", "disable", "reload");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove"))) {
            List<String> playerNames = new ArrayList<>();
            for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                playerNames.add((player.getName()));
            }
            return playerNames;
        }
        return Lists.newArrayList();
    }

    private boolean hasManagePermission(CommandSender sender) {
        return sender.hasPermission("namebasedwhitelist.manage") || sender.hasPermission("namebasedwhitelist.*");
    }

    private boolean hasModifyPermission(CommandSender sender) {
        return sender.hasPermission("namebasedwhitelist.modify") || sender.hasPermission("namebasedwhitelist.*");
    }
}
