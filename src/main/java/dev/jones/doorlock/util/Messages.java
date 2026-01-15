package dev.jones.doorlock.util;

import dev.jones.doorlock.Doorlock;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class Messages {
    private static FileConfiguration config;

    public static void init() {
        load();
    }

    public static void reload() {
        load();
    }

    private static void load() {
        File folder = Doorlock.getInstance().getDataFolder();
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File file = new File(folder, "messages.yml");
        if (!file.exists()) {
            Doorlock.getInstance().saveResource("messages.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
        InputStream defStream = Doorlock.getInstance().getResource("messages.yml");
        if (defStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defStream, StandardCharsets.UTF_8));
            config.setDefaults(defConfig);
        }
    }

    public static String get(String path) {
        if (config == null) {
            init();
        }
        String raw = config.getString(path, "&cMissing message: " + path);
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    public static String format(String path, Object... args) {
        String base = get(path);
        return String.format(base, args);
    }
}

