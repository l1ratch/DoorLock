package dev.jones.doorlock;

import dev.jones.doorlock.command.*;
import dev.jones.doorlock.listener.BlockClaimerListener;
import dev.jones.doorlock.listener.DebugListener;
import dev.jones.doorlock.listener.KeyListener;
import dev.jones.doorlock.util.DoorlockHearbeat;
import dev.jones.doorlock.util.ItemStackBuilder;
import dev.jones.doorlock.util.Messages;
import dev.jones.doorlock.util.SaveUtil;
import dev.jones.doorlock.util.Updater;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public final class Doorlock extends JavaPlugin {
    private static Plugin instance;
    private static List<NamespacedKey> recipes=new ArrayList<>();
    private static File file;
    private static final boolean DEBUG=false;
    private boolean updateCheckSetting;
    @Override
    public void onEnable() {
        /*
        Initialize Variables
         */
        instance=this;
        file=this.getFile();
        /*
        Load config
         */
        saveDefaultConfig(); // создаёт config.yml в папке плагина, если его нет

        // Загружаем дефолты из resources/config.yml
        FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(getResource("config.yml"))
        );
        getConfig().addDefaults(defaultConfig);
        getConfig().options().copyDefaults(true);

        for (String key : defaultConfig.getKeys(true)) {
            if (!getConfig().contains(key, true)) {
                getLogger().warning(String.format(Messages.get("config.missing_path"), key));
                getConfig().set(key, defaultConfig.get(key));
            }
        }
        saveConfig();
        updateCheckSetting = this.getConfig().getBoolean("update", true);

        SaveUtil.init();
        Messages.init();
        /*
        Scan for updates
         */
        // Если проверка обновлений ВКЛЮЧЕНА, то выполняем проверку
        if (updateCheckSetting) {
            if (Updater.fetchUpdates()) {
                getLogger().info(Messages.get("updater.available"));
                // Дополнительные действия при обнаружении обновлений (если нужно)
            } else getLogger().info(Messages.get("updater.up_to_date"));
        } else getLogger().info(Messages.get("updater.check_disabled"));
        // Если update = false, то ничего не делаем — проверки нет
        /*
        Register Generic Commands
         */
        if(DEBUG) {
            Bukkit.getPluginManager().registerEvents(new DebugListener(), this);
            this.getCommand("dldebug").setExecutor(new DebugCommand());
        }else{
            this.getCommand("dldebug").setExecutor(new DisabledCommand());
        }

        this.getCommand("doorlock").setExecutor(new DoorlockCommand());
        this.getCommand("doorlock").setTabCompleter(new DoorlockCommandTabCompleter());

        this.getCommand("unlock").setExecutor(new UnlockCommand());
        this.getCommand("unlock").setTabCompleter(new UnlockCommandTabCompleter());

        /*
        Register Key
         */
        ItemStack keyItem=new ItemStackBuilder(Material.GOLD_NUGGET)
                .setName(Messages.get("item.key.name"))
                .setLore(Messages.get("item.key.lore1"),Messages.get("item.key.lore2"))
                .addNbtTag("iskey","1")
                .setCustomModelData(9999101)
                .build();

        NamespacedKey keyKey=new NamespacedKey(this,"key");
        recipes.add(keyKey);

        ShapedRecipe keyRecipe=new ShapedRecipe(keyKey,keyItem);
        keyRecipe.shape("GNN");
        keyRecipe.setIngredient('G', Material.GOLD_INGOT);
        keyRecipe.setIngredient('N', Material.GOLD_NUGGET);

        if(getConfig().getBoolean("items.key")) {
            Bukkit.addRecipe(keyRecipe);
            Bukkit.getPluginManager().registerEvents(new KeyListener(), this);
        }
        /*
        Register BlockLocker
         */
        ItemStack blockClaimerItem=new ItemStackBuilder(Material.IRON_AXE)
                .setName(Messages.get("item.blocklocker.name"))
                .setLore(Messages.get("item.blocklocker.lore1"))
                .addNbtTag("isblocklocker","1")
                .setCustomModelData(9999102)
                .build();

        NamespacedKey blockClaimerKey=new NamespacedKey(this,"block_locker");
        recipes.add(blockClaimerKey);

        ShapedRecipe blockClaimerRecipe=new ShapedRecipe(blockClaimerKey,blockClaimerItem);
        blockClaimerRecipe.shape(
                "SGS",
                "IHI",
                "XPX");
        blockClaimerRecipe.setIngredient('S',Material.SAND);
        blockClaimerRecipe.setIngredient('G',Material.GRAVEL);
        blockClaimerRecipe.setIngredient('I',Material.IRON_BLOCK);
        blockClaimerRecipe.setIngredient('H',Material.HOPPER);
        blockClaimerRecipe.setIngredient('P',Material.IRON_PICKAXE);

        if(getConfig().getBoolean("items.blocklocker")) {
            Bukkit.addRecipe(blockClaimerRecipe);
            Bukkit.getPluginManager().registerEvents(new BlockClaimerListener(), this);
        }
        /*
        Register Doordrill
         */
        ItemStack doorDrillItem=new ItemStackBuilder(Material.DIAMOND_AXE)
                .setName(Messages.get("item.doordrill.name"))
                .setLore(Messages.get("item.doordrill.lore1"))
                .addNbtTag("isdoordrill","1")
                .setCustomModelData(9999103)
                .build();
        Damageable doorDrillMeta=(Damageable) doorDrillItem.getItemMeta();
        doorDrillMeta.setDamage(1550);
        doorDrillItem.setItemMeta((ItemMeta) doorDrillMeta);

        NamespacedKey doorDrillKey=new NamespacedKey(this,"door_drill");
        recipes.add(doorDrillKey);

        ShapedRecipe doorDrillRecipe=new ShapedRecipe(doorDrillKey,doorDrillItem);

        doorDrillRecipe.shape(
                "BDX",
                "IND",
                "BDX");
        doorDrillRecipe.setIngredient('B',Material.BEACON);
        doorDrillRecipe.setIngredient('D',Material.DIAMOND);
        doorDrillRecipe.setIngredient('I',Material.IRON_BLOCK);
        doorDrillRecipe.setIngredient('N',Material.NETHER_STAR);

        Bukkit.addRecipe(doorDrillRecipe);


        DoorlockHearbeat.start();

        getLogger().info(Messages.get("plugin.enabled"));
        getLogger().info(Messages.get("plugin.translator"));
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        this.getServer().resetRecipes();
        SaveUtil.shutdown();
    }

    public static Plugin getInstance() {
        return instance;
    }
    public static File getJarfile(){return file;}

    public static List<NamespacedKey> getRecipes() {
        return recipes;
    }
}
