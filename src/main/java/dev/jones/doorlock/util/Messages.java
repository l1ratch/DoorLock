package dev.jones.doorlock.util;

import dev.jones.doorlock.Doorlock;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class Messages {
    private static FileConfiguration config;
    private static String activeLanguage = "en";

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

        File langFolder = new File(folder, "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        // Keep lang/en.yml and lang/ru.yml in sync whenever message keys change.
        ensureLanguageResource("en");
        ensureLanguageResource("ru");

        String configuredLanguage = Doorlock.getInstance().getConfig().getString("language", "en");
        if (configuredLanguage == null || configuredLanguage.isBlank()) {
            configuredLanguage = "en";
        }
        activeLanguage = configuredLanguage.trim().toLowerCase(Locale.ROOT);

        File selectedFile = new File(langFolder, activeLanguage + ".yml");
        if (!selectedFile.exists()) {
            Doorlock.getInstance().getLogger().warning("Language file lang/" + activeLanguage + ".yml is missing. Falling back to lang/en.yml.");
            activeLanguage = "en";
            selectedFile = new File(langFolder, "en.yml");
        }

        config = YamlConfiguration.loadConfiguration(selectedFile);

        File defaultFile = new File(langFolder, "en.yml");
        YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defaultFile);
        config.setDefaults(defConfig);
    }

    private static void ensureLanguageResource(String language) {
        File folder = new File(Doorlock.getInstance().getDataFolder(), "lang");
        File file = new File(folder, language + ".yml");
        if (!file.exists()) {
            Doorlock.getInstance().saveResource("lang/" + language + ".yml", false);
        }
    }

    public static String get(String path) {
        if (config == null) {
            init();
        }
        String raw = config.getString(path, "&cMissing message: " + path + " [" + activeLanguage + "]");
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    public static String format(String path, Object... args) {
        String base = get(path);
        return String.format(base, args);
    }
}
