package org.mrbonnieg.NameBasedWhitelist;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class MySqlStorage implements Storage {
    private final Main plugin;
    private final String host, database, username, password;

    private final Set<String> cache= ConcurrentHashMap.newKeySet();
    private final Set<String> pendingAdds = ConcurrentHashMap.newKeySet();
    private final Set<String> pendingRemoves = ConcurrentHashMap.newKeySet();

    private volatile Connection connection;
    private volatile boolean ready = false;

    private static final long FLUSH_INTERVAL_TICKS = 40L;
    private static final long SYNC_INTERVAL_TICKS = 20L * 60;

    public MySqlStorage(Main plugin) {
        this.plugin = plugin;
        FileConfiguration config = plugin.getConfig();
        this.host = config.getString("database.host", "localhost:3306");
        this.database = config.getString("database.name", "database");
        this.username = config.getString("database.username", "root");
        this.password = config.getString("database.password", "");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            initConnection();
            if (!ready) return;

            Bukkit.getScheduler().runTaskTimerAsynchronously(
                    plugin, this::flushPending,
                    FLUSH_INTERVAL_TICKS, FLUSH_INTERVAL_TICKS
            );
            Bukkit.getScheduler().runTaskTimerAsynchronously(
                    plugin, this::syncCacheFromDb,
                    SYNC_INTERVAL_TICKS, SYNC_INTERVAL_TICKS
            );
        });
    }

    private synchronized void initConnection() {
        try {
            if (connection != null && !connection.isClosed()) return;
            String url = "jdbc:mysql://" + host + "/" + database
                    + "?autoReconnect=true"
                    + "&connectTimeout=90000"
                    + "&socketTimeout=180000"
                    + "&useSSL=false"
                    + "&characterEncoding=utf8"
                    + "&allowPublicKeyRetrieval=true"
                    + "&rewriteBatchedStatements=true"
                    + "&cachePrepStmts=true"
                    + "&prepStmtCacheSize=10";
            connection = DriverManager.getConnection(url, username, password);
            createTable();
            loadCache();
            ready = true;

            plugin.getLogger().log(Level.INFO, "MySQL connected. Loaded " + cache.size() + " players into cache.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "MySQL connection failed!", e);
        }
    }

    private synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) initConnection();
        return connection;
    }

    private void createTable() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS whitelist (username VARCHAR(255) PRIMARY KEY);");
        }
    }

    private void loadCache() throws SQLException {
        Set<String> fresh = new HashSet<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT username FROM whitelist;")) {
            while (resultSet.next()) {
                fresh.add(resultSet.getString("username").toLowerCase());
            }
        }
        cache.clear();
        cache.addAll(fresh);
    }

    private synchronized void syncCacheFromDb() {
        try {
            flushPending();
            loadCache();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Cache sync from DB failed!", e);
        }
    }
    private synchronized void flushPending() {
        if (pendingAdds.isEmpty() && pendingRemoves.isEmpty()) return;

        Set<String> toAdd = new HashSet<>(pendingAdds);
        Set<String> toRemove = new HashSet<>(pendingRemoves);
        pendingAdds.clear();
        pendingRemoves.clear();

        try {
            Connection connection = getConnection();
            connection.setAutoCommit(false);
            try {
                if (!toAdd.isEmpty()) {
                    try (PreparedStatement ps = connection.prepareStatement(
                            "INSERT INTO whitelist (username) VALUES (?)" +
                                    " ON DUPLICATE KEY UPDATE username = username;")) {
                        for (String name : toAdd) {
                            ps.setString(1, name);
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                }
                if (!toRemove.isEmpty()) {
                    try (PreparedStatement ps = connection.prepareStatement(
                            "DELETE FROM whitelist WHERE username = ?;")) {
                        for (String name : toRemove) {
                            ps.setString(1, name);
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                pendingAdds.addAll(toAdd);
                pendingRemoves.addAll(toRemove);
                plugin.getLogger().log(Level.SEVERE, "MySQL flush failed, will retry in " + (FLUSH_INTERVAL_TICKS / 20) + "s", e);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "MySQL connection error during flush!", e);
        }
    }


    @Override
    public List<String> getPlayers() {
        return new ArrayList<>(cache);
    }

    @Override
    public boolean containsPlayer(String username) {
        return cache.contains(username.toLowerCase());
    }

    @Override
    public boolean addPlayer(String username) {
        String lower = username.toLowerCase();
        if (!cache.add(lower)) return false;
        pendingRemoves.remove(lower);
        pendingAdds.add(lower);
        return true;
    }

    @Override
    public boolean removePlayer(String username) {
        String lower = username.toLowerCase();
        if (!cache.remove(lower)) return false;
        pendingAdds.remove(lower);
        pendingRemoves.add(lower);
        return true;
    }

    @Override
    public void saveWhitelist() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::flushPending);
    }
}
