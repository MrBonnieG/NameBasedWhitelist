package org.mrbonnieg.NameBasedWhitelist;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;

public final class Main extends JavaPlugin {
    private Storage storage;
    public Storage getStorage() { return storage; }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        String storageType = getConfig().getString("settings.storage-type", "yml");
        switch (storageType) {
            case "mysql":
                storage = new MySqlStorage(this);
                getLogger().log(Level.INFO, "Storage type: MySQL");
                break;
            default:
                storage = new YamlStorage(this);
                getLogger().log(Level.INFO, "Storage type: YAML");
                break;
        }

        getServer().getPluginManager().registerEvents(new Events(this), this);
        Commands commands = new Commands(this);
        getServer().getPluginCommand("nbwl").setExecutor(commands);
        getServer().getPluginCommand("nbwl").setTabCompleter(commands);
        getLogger().log(Level.INFO, "Plugin enabled");
    }

    @Override
    public void onDisable() {
        getLogger().log(Level.INFO, "Plugin disabled");
    }
}
