package dev.jones.doorlock.util;

import dev.jones.doorlock.Doorlock;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class SaveUtil {
    private static File configFile;
    private static FileConfiguration config;
    public static void saveToConfig(){
        configFile=new File(Doorlock.getInstance().getDataFolder(),"data.yml");
        try {
            if(configFile.createNewFile()){
                Doorlock.getInstance().getLogger().warning("Файл с данными не существует!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(config==null){
            config=YamlConfiguration.loadConfiguration(configFile);
        }
        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void lockDoor(String key,Location loc){
        saveToConfig();
        List<String> list=config.getStringList("key."+key+".locations");
        list.add(loc.getBlockX()+" "+loc.getBlockY()+" "+loc.getBlockZ());
        config.set("key."+key+".locations",list);
        saveToConfig();
    }
    public static void unlockDoor(Location door){
        String key=getKey(door);
        if(key==null)return;
        List<String> list=config.getStringList("key."+key+".locations");
        list.remove(door.getBlockX()+" "+door.getBlockY()+" "+door.getBlockZ());
        config.set("key."+key+".locations",list);
    }
    public static String getKey(Location door){
        saveToConfig();
        if(config.getConfigurationSection("key")==null)return null;
        for (String key : config.getConfigurationSection("key").getKeys(false)) {
            if(config.getStringList("key."+key+".locations").contains(door.getBlockX()+" "+door.getBlockY()+" "+door.getBlockZ())){
                return key;
            }
        }
        return null;
    }
    public static void enableLocking(Location location){
        saveToConfig();
        List<String> list=config.getStringList("lockable");
        list.add(location.getBlockX()+" "+location.getBlockY()+" "+location.getBlockZ());
        config.set("lockable",list);
        saveToConfig();
   }
    public static void disableLocking(Location location){
        saveToConfig();
        List<String> list=config.getStringList("lockable");
        list.remove(location.getBlockX()+" "+location.getBlockY()+" "+location.getBlockZ());
        config.set("lockable",list);
        saveToConfig();
    }
    public static boolean isLockable(Location location){
        saveToConfig();
        return config.getStringList("lockable").contains(location.getBlockX()+" "+location.getBlockY()+" "+location.getBlockZ());
    }
    public static String getVersion(){
        saveToConfig();
        return config.getString("version");
    }
    public static void setVersion(String ver){
        saveToConfig();
        config.set("version",ver);
        saveToConfig();
    }
}
