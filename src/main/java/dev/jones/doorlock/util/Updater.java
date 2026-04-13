package dev.jones.doorlock.util;

import dev.jones.doorlock.Doorlock;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class Updater {
    private static final String UPDATE_URL="https://api.github.com/repos/l1ratch/DoorLock/releases/latest";

    public static boolean fetchUpdates() {
        try {
            Doorlock.getInstance().getLogger().info(Messages.get("updater.scanning"));
            JSONObject obj = readLatestRelease();
            String latestVersion = normalizeVersion(obj.optString("tag_name", ""));
            String currentVersion = normalizeVersion(SaveUtil.getVersion());

            if (latestVersion.isEmpty()) {
                Doorlock.getInstance().getLogger().warning("Update check failed: latest release tag is empty.");
                return false;
            }

            if (!latestVersion.equalsIgnoreCase(currentVersion)) {
                Doorlock.getInstance().getLogger().info(Messages.get("updater.available"));
                Doorlock.getInstance().getLogger().info("Installed version: " + currentVersion + ", latest version: " + latestVersion);
                return true;
            }

            Doorlock.getInstance().getLogger().info(Messages.get("updater.latest_installed"));
        } catch (FileNotFoundException e) {
            Doorlock.getInstance().getLogger().warning("Update check failed: Release not found (404). This is expected if no releases exist yet.");
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    public static void fetchUpdatesAsync(JavaPlugin plugin, Consumer<Boolean> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean updateAvailable = fetchUpdates();
            if (callback != null) {
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(updateAvailable));
            }
        });
    }

    private static JSONObject readLatestRelease() throws IOException {
        URL url = new URL(UPDATE_URL);
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("User-Agent", "DoorLock-Update-Check");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        try (InputStream stream = connection.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line);
            }
            return new JSONObject(out.toString());
        }
    }

    private static String normalizeVersion(String version) {
        if (version == null) {
            return "";
        }
        String normalized = version.trim();
        if (normalized.startsWith("v") || normalized.startsWith("V")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }
}
