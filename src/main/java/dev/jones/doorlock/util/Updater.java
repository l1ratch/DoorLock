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
    private static final String UPDATE_URL="https://api.github.com/repos/l1ratch/DoorLock/releases/latest";

    private static File file;
    private static boolean updated=false;

    public static boolean fetchUpdates() {
        try {
            Doorlock.getInstance().getLogger().info(Messages.get("updater.scanning"));
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

            if (pluginVersion == null || !pluginVersion.equals(currentVersion)) {
                Doorlock.getInstance().getLogger().info(Messages.get("updater.downloading"));
                URL dl = new URL(obj.getJSONArray("assets").getJSONObject(0).getString("browser_download_url"));
                
                File updateFolder = Bukkit.getUpdateFolderFile();
                if (!updateFolder.exists()) {
                    updateFolder.mkdirs();
                }
                
                file = new File(updateFolder, Doorlock.getJarfile().getName());
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
                Doorlock.getInstance().getLogger().info(Messages.get("updater.downloaded"));
                SaveUtil.setVersion(currentVersion);
                updated=true;

                return true;
            }else{
                Doorlock.getInstance().getLogger().info(Messages.get("updater.latest_installed"));
            }
        } catch (FileNotFoundException e) {
            Doorlock.getInstance().getLogger().warning("Update check failed: Release not found (404). This is expected if no releases exist yet.");
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }
}
