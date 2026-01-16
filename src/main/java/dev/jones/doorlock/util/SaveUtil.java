package dev.jones.doorlock.util;

import dev.jones.doorlock.Doorlock;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.List;

public class SaveUtil {
    private static Connection connection;

    public static synchronized void init() {
        if (connection != null) {
            return;
        }
        try {
            Class.forName("org.sqlite.JDBC");
            File dataFolder = Doorlock.getInstance().getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            File dbFile = new File(dataFolder, "doorlock.db");
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);
            connection.setAutoCommit(true);
            createTables();
            migrateFromYamlIfNeeded();
            Doorlock.getInstance().getLogger().info("Database connected successfully.");
        } catch (ClassNotFoundException | SQLException e) {
            Doorlock.getInstance().getLogger().severe("Failed to connect to database!");
            e.printStackTrace();
        }
    }

    public static synchronized void shutdown() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
            connection = null;
        }
    }

    private static void createTables() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS keys (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "key_uuid TEXT NOT NULL UNIQUE" +
                    ")");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS locked_doors (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "key_id INTEGER NOT NULL," +
                    "world TEXT NOT NULL," +
                    "x INTEGER NOT NULL," +
                    "y INTEGER NOT NULL," +
                    "z INTEGER NOT NULL," +
                    "UNIQUE(world, x, y, z)," +
                    "FOREIGN KEY(key_id) REFERENCES keys(id) ON DELETE CASCADE" +
                    ")");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS lockable_blocks (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "world TEXT NOT NULL," +
                    "x INTEGER NOT NULL," +
                    "y INTEGER NOT NULL," +
                    "z INTEGER NOT NULL," +
                    "UNIQUE(world, x, y, z)" +
                    ")");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS meta (" +
                    "key TEXT PRIMARY KEY," +
                    "value TEXT" +
                    ")");
        }
    }

    private static void migrateFromYamlIfNeeded() {
        File legacyFile = new File(Doorlock.getInstance().getDataFolder(), "data.yml");
        if (!legacyFile.exists()) {
            return;
        }
        try {
            if (getMeta("migrated") != null) {
                return;
            }
            FileConfiguration legacyConfig = YamlConfiguration.loadConfiguration(legacyFile);
            String defaultWorld = null;
            if (!Doorlock.getInstance().getServer().getWorlds().isEmpty()) {
                defaultWorld = Doorlock.getInstance().getServer().getWorlds().get(0).getName();
            }
            if (defaultWorld != null && legacyConfig.getConfigurationSection("key") != null) {
                for (String key : legacyConfig.getConfigurationSection("key").getKeys(false)) {
                    List<String> locations = legacyConfig.getStringList("key." + key + ".locations");
                    for (String value : locations) {
                        String[] split = value.split(" ");
                        if (split.length != 3) {
                            continue;
                        }
                        try {
                            int x = Integer.parseInt(split[0]);
                            int y = Integer.parseInt(split[1]);
                            int z = Integer.parseInt(split[2]);
                            insertLock(defaultWorld, x, y, z, key);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }
            if (defaultWorld != null) {
                List<String> lockable = legacyConfig.getStringList("lockable");
                for (String value : lockable) {
                    String[] split = value.split(" ");
                    if (split.length != 3) {
                        continue;
                    }
                    try {
                        int x = Integer.parseInt(split[0]);
                        int y = Integer.parseInt(split[1]);
                        int z = Integer.parseInt(split[2]);
                        insertLockable(defaultWorld, x, y, z);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            String version = legacyConfig.getString("version");
            if (version != null) {
                setVersion(version);
            }
            setMeta("migrated", "true");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static int ensureKeyId(String key) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement("INSERT OR IGNORE INTO keys(key_uuid) VALUES (?)")) {
            insert.setString(1, key);
            insert.executeUpdate();
        }
        try (PreparedStatement select = connection.prepareStatement("SELECT id FROM keys WHERE key_uuid = ?")) {
            select.setString(1, key);
            try (ResultSet rs = select.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        throw new SQLException("Failed to resolve key id");
    }

    private static void insertLock(String world, int x, int y, int z, String key) throws SQLException {
        int keyId = ensureKeyId(key);
        try (PreparedStatement statement = connection.prepareStatement("INSERT OR REPLACE INTO locked_doors(key_id, world, x, y, z) VALUES (?,?,?,?,?)")) {
            statement.setInt(1, keyId);
            statement.setString(2, world);
            statement.setInt(3, x);
            statement.setInt(4, y);
            statement.setInt(5, z);
            statement.executeUpdate();
        }
    }

    private static void insertLockable(String world, int x, int y, int z) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("INSERT OR IGNORE INTO lockable_blocks(world, x, y, z) VALUES (?,?,?,?)")) {
            statement.setString(1, world);
            statement.setInt(2, x);
            statement.setInt(3, y);
            statement.setInt(4, z);
            statement.executeUpdate();
        }
    }

    public static synchronized void lockDoor(String key, Location loc) {
        if (connection == null || loc == null || loc.getWorld() == null) {
            return;
        }
        try {
            insertLock(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), key);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static synchronized void unlockDoor(Location door) {
        if (connection == null || door == null || door.getWorld() == null) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM locked_doors WHERE world = ? AND x = ? AND y = ? AND z = ?")) {
            statement.setString(1, door.getWorld().getName());
            statement.setInt(2, door.getBlockX());
            statement.setInt(3, door.getBlockY());
            statement.setInt(4, door.getBlockZ());
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static synchronized String getKey(Location door) {
        if (connection == null || door == null || door.getWorld() == null) {
            return null;
        }
        try (PreparedStatement statement = connection.prepareStatement("SELECT k.key_uuid FROM locked_doors d JOIN keys k ON d.key_id = k.id WHERE d.world = ? AND d.x = ? AND d.y = ? AND d.z = ? LIMIT 1")) {
            statement.setString(1, door.getWorld().getName());
            statement.setInt(2, door.getBlockX());
            statement.setInt(3, door.getBlockY());
            statement.setInt(4, door.getBlockZ());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("key_uuid");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static synchronized void enableLocking(Location location) {
        if (connection == null || location == null || location.getWorld() == null) {
            return;
        }
        try {
            insertLockable(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static synchronized void disableLocking(Location location) {
        if (connection == null || location == null || location.getWorld() == null) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM lockable_blocks WHERE world = ? AND x = ? AND y = ? AND z = ?")) {
            statement.setString(1, location.getWorld().getName());
            statement.setInt(2, location.getBlockX());
            statement.setInt(3, location.getBlockY());
            statement.setInt(4, location.getBlockZ());
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static synchronized boolean isLockable(Location location) {
        if (connection == null || location == null || location.getWorld() == null) {
            return false;
        }
        try (PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM lockable_blocks WHERE world = ? AND x = ? AND y = ? AND z = ? LIMIT 1")) {
            statement.setString(1, location.getWorld().getName());
            statement.setInt(2, location.getBlockX());
            statement.setInt(3, location.getBlockY());
            statement.setInt(4, location.getBlockZ());
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static synchronized String getVersion() {
        return getMeta("version");
    }

    public static synchronized void setVersion(String ver) {
        setMeta("version", ver);
    }

    private static String getMeta(String key) {
        if (connection == null) {
            return null;
        }
        try (PreparedStatement statement = connection.prepareStatement("SELECT value FROM meta WHERE key = ?")) {
            statement.setString(1, key);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("value");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void setMeta(String key, String value) {
        if (connection == null) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement("INSERT OR REPLACE INTO meta(key, value) VALUES (?,?)")) {
            statement.setString(1, key);
            statement.setString(2, value);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
