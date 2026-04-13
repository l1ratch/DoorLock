package dev.jones.doorlock;

import dev.jones.doorlock.command.*;
import dev.jones.doorlock.listener.BlockClaimerListener;
import dev.jones.doorlock.listener.KeyListener;
import dev.jones.doorlock.util.DoorlockHearbeat;
import dev.jones.doorlock.util.ItemStackBuilder;
import dev.jones.doorlock.util.Messages;
import dev.jones.doorlock.util.SaveUtil;
import dev.jones.doorlock.util.WorldGuardSupport;
import dev.jones.doorlock.util.Updater;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
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
    private static final boolean DEBUG=false;
    private boolean updateCheckSetting;
    @Override
    public void onEnable() {
        /*
        Initialize Variables
         */
        instance=this;
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
        if (!SaveUtil.isReady()) {
            getLogger().severe("Database initialization failed. DoorLock will be disabled to avoid running without persistence.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        Messages.init();
        WorldGuardSupport.reload();
        WorldGuardSupport.logStartupState();
        registerCommands();
        registerGameplayContent();
        DoorlockHearbeat.start();
        scheduleUpdateCheck(updateCheckSetting);

        getLogger().info(Messages.get("plugin.enabled"));
        getLogger().info(Messages.get("plugin.translator"));
    }

    @Override
    public void onDisable() {
        unregisterGameplayContent();
        DoorlockHearbeat.stop();
        SaveUtil.shutdown();
    }

    public static Plugin getInstance() {
        return instance;
    }
    public static List<NamespacedKey> getRecipes() {
        return recipes;
    }

    public void refreshConfigState() {
        updateCheckSetting = getConfig().getBoolean("update", true);
    }

    public void reloadGameplayContent() {
        unregisterGameplayContent();
        registerGameplayContent();
    }

    public void scheduleUpdateCheck(boolean enabled) {
        if (!enabled) {
            getLogger().info(Messages.get("updater.disabled"));
            return;
        }
        Updater.fetchUpdatesAsync(this, null);
    }

    private void registerCommands() {
        this.getCommand("doorlock").setExecutor(new DoorlockCommand());
        this.getCommand("doorlock").setTabCompleter(new DoorlockCommandTabCompleter());
    }

    private void registerGameplayContent() {
        ItemStack keyItem = new ItemStackBuilder(Material.GOLD_NUGGET)
                .setName(Messages.get("item.key.name"))
                .setLore(Messages.get("item.key.lore1"), Messages.get("item.key.lore2"))
                .addNbtTag("iskey", "1")
                .setCustomModelData(9999101)
                .build();

        NamespacedKey keyKey = new NamespacedKey(this, "key");
        if (getConfig().getBoolean("items.key")) {
            recipes.add(keyKey);
            ShapedRecipe keyRecipe = new ShapedRecipe(keyKey, keyItem);
            keyRecipe.shape("GNN");
            keyRecipe.setIngredient('G', Material.GOLD_INGOT);
            keyRecipe.setIngredient('N', Material.GOLD_NUGGET);
            Bukkit.addRecipe(keyRecipe);
            Bukkit.getPluginManager().registerEvents(new KeyListener(), this);
        }

        ItemStack blockClaimerItem = new ItemStackBuilder(Material.IRON_AXE)
                .setName(Messages.get("item.blocklocker.name"))
                .setLore(Messages.get("item.blocklocker.lore1"))
                .addNbtTag("isblocklocker", "1")
                .setCustomModelData(9999102)
                .build();

        NamespacedKey blockClaimerKey = new NamespacedKey(this, "block_locker");
        if (getConfig().getBoolean("items.blocklocker")) {
            recipes.add(blockClaimerKey);
            ShapedRecipe blockClaimerRecipe = new ShapedRecipe(blockClaimerKey, blockClaimerItem);
            blockClaimerRecipe.shape(
                    "SGS",
                    "IHI",
                    "XPX");
            blockClaimerRecipe.setIngredient('S', Material.SAND);
            blockClaimerRecipe.setIngredient('G', Material.GRAVEL);
            blockClaimerRecipe.setIngredient('I', Material.IRON_BLOCK);
            blockClaimerRecipe.setIngredient('H', Material.HOPPER);
            blockClaimerRecipe.setIngredient('P', Material.IRON_PICKAXE);
            Bukkit.addRecipe(blockClaimerRecipe);
            Bukkit.getPluginManager().registerEvents(new BlockClaimerListener(), this);
        }

        if (getConfig().getBoolean("items.doordrill")) {
            ItemStack doorDrillItem = new ItemStackBuilder(Material.DIAMOND_AXE)
                    .setName(Messages.get("item.doordrill.name"))
                    .setLore(Messages.get("item.doordrill.lore1"))
                    .addNbtTag("isdoordrill", "1")
                    .setCustomModelData(9999103)
                    .build();
            Damageable doorDrillMeta = (Damageable) doorDrillItem.getItemMeta();
            doorDrillMeta.setDamage(1550);
            doorDrillItem.setItemMeta((ItemMeta) doorDrillMeta);

            NamespacedKey doorDrillKey = new NamespacedKey(this, "door_drill");
            recipes.add(doorDrillKey);

            ShapedRecipe doorDrillRecipe = new ShapedRecipe(doorDrillKey, doorDrillItem);
            doorDrillRecipe.shape(
                    "BDX",
                    "IND",
                    "BDX");
            doorDrillRecipe.setIngredient('B', Material.BEACON);
            doorDrillRecipe.setIngredient('D', Material.DIAMOND);
            doorDrillRecipe.setIngredient('I', Material.IRON_BLOCK);
            doorDrillRecipe.setIngredient('N', Material.NETHER_STAR);
            Bukkit.addRecipe(doorDrillRecipe);
        }
    }

    private void unregisterGameplayContent() {
        HandlerList.unregisterAll(this);
        for (NamespacedKey recipe : recipes) {
            Bukkit.removeRecipe(recipe);
        }
        recipes.clear();
    }
}
