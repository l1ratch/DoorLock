package dev.jones.doorlock.util;

import dev.jones.doorlock.Doorlock;
import org.bukkit.Bukkit;
import org.bukkit.block.data.type.Door;
import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.bukkit.Bukkit.getLogger;

public class Updater {
    private static final String UPDATE_URL="https://api.github.com/repos/SJones-BWGY/DoorLock/releases/latest";
    private static final boolean DONT_DOWNLOAD=false;
    private static final boolean ALWAYS_DOWNLOAD=false;

    private static File file;
    private static boolean updated=false;

    private static final String UPD_False="false";

    public static boolean fetchUpdates() {
        if (UPD_False == "false") {
            getLogger().info("Функция проверки обновлений деактивирована!");
            return false;
        }
        if (DONT_DOWNLOAD) return false;
        try {
            Doorlock.getInstance().getLogger().info("Scanning for updates...");
            URL url = new URL(UPDATE_URL);
            InputStream stream = null;
            stream = url.openStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            StringBuilder out = new StringBuilder();
            while (true) {
                String str = reader.readLine();
                if (str == null) break;
                out.append(str).append(" ");
            }
            out = new StringBuilder(out.toString().trim());
            JSONObject obj = new JSONObject(out.toString());

            String pluginVersion = SaveUtil.getVersion();
            String currentVersion = obj.getString("tag_name");

            if (pluginVersion == null || !pluginVersion.equals(currentVersion) || ALWAYS_DOWNLOAD) {
                Doorlock.getInstance().getLogger().info("Downloading update...");
                URL dl = new URL(obj.getJSONArray("assets").getJSONObject(0).getString("browser_download_url"));
                file=new File(Doorlock.getJarfile().getParentFile(),Doorlock.getJarfile().getName()+".new");
                file.createNewFile();
                try (BufferedInputStream in = new BufferedInputStream(dl.openStream());
                     FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                    byte dataBuffer[] = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                        fileOutputStream.write(dataBuffer, 0, bytesRead);
                    }
                } catch (IOException e) {
                    // handle exception
                }
                Doorlock.getInstance().getLogger().info("Update downloaded. Installing...");
                SaveUtil.setVersion(currentVersion);
                updated=true;

                return true;
            }else{
                Doorlock.getInstance().getLogger().info("You plugin has the newest version installed!");
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }
    public static void pluginDisabled(){
        File old=Doorlock.getJarfile();
        Thread thr = new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                Files.delete(old.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
            getLogger().info("Update installed. Reloading.");
            Bukkit.getServer().reload();
            if (!file.renameTo(Doorlock.getJarfile()))
                getLogger().severe("Update failed. Could not copy to jar!");
        });
        if(updated)thr.start();
    }
}
