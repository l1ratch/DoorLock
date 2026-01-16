package dev.jones.doorlock.command;

import dev.jones.doorlock.Doorlock;
import dev.jones.doorlock.util.ItemStackBuilder;
import dev.jones.doorlock.util.Messages;
import dev.jones.doorlock.util.SaveUtil;
import dev.jones.doorlock.util.Updater;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class DoorlockCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(args.length==1){
            if(args[0].equalsIgnoreCase("version")){
                sender.sendMessage(Messages.get("doorlock.version.header"));
                sender.sendMessage(Messages.format("doorlock.version.value", SaveUtil.getVersion()));
                sender.sendMessage(Messages.get("doorlock.version.translated_by"));
                return true;
            }else if(args[0].equalsIgnoreCase("help")){
                sender.sendMessage(Messages.get("doorlock.help.line1"));
                sender.sendMessage(Messages.get("doorlock.help.line2"));
                sender.sendMessage(Messages.get("doorlock.help.line3"));
                sender.sendMessage(Messages.get("doorlock.help.line4"));
                Updater.fetchUpdates();
                return true;
            }else if(args[0].equalsIgnoreCase("update")){
                sender.sendMessage(Messages.get("doorlock.update.checking"));
                sender.sendMessage(Messages.get("doorlock.update.console_info"));
                Updater.fetchUpdates();
                return true;
            }else if(args[0].equalsIgnoreCase("reload")){
                sender.sendMessage(Messages.get("doorlock.reload.start"));
                sender.sendMessage(Messages.get("doorlock.reload.may_take_time"));
                
                Doorlock.getInstance().reloadConfig();
                Messages.init();
                
                if (Doorlock.getInstance().getConfig().getBoolean("update", true)) {
                    Updater.fetchUpdates();
                }

                sender.sendMessage(Messages.get("doorlock.reload.success"));
                return true;
            }else if(args[0].equalsIgnoreCase("getkey")){
                if (!sender.hasPermission("doorlock.admin")) {
                    sender.sendMessage(Messages.get("command.no_permission"));
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage(Messages.get("command.only_player"));
                    return true;
                }
                Player player = (Player) sender;
                String uuid = UUID.randomUUID().toString();
                ItemStack keyItem = new ItemStackBuilder(Material.GOLD_NUGGET)
                        .setName(Messages.get("item.key.name"))
                        .setLore(Messages.get("item.key.lore1"), Messages.get("item.key.lore2"))
                        .addNbtTag("iskey", "1")
                        .setCustomModelData(9999101)
                        .addNbtTag("key", uuid)
                        .build();
                player.getInventory().addItem(keyItem);
                sender.sendMessage(Messages.format("command.getkey.success", uuid));
                return true;
            }else if(args[0].equalsIgnoreCase("info")){
                if (!sender.hasPermission("doorlock.admin")) {
                    sender.sendMessage(Messages.get("command.no_permission"));
                    return true;
                }
                sender.sendMessage(Messages.get("command.info.header"));
                sender.sendMessage(Messages.format("command.info.db_stats", SaveUtil.getKeysCount(), SaveUtil.getLocksCount()));
                sender.sendMessage(Messages.format("command.info.migration", SaveUtil.isMigrated() ? "Yes" : "No"));

                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    // Check item in hand
                    if (player.getInventory().getItemInMainHand().hasItemMeta()) {
                        PersistentDataContainer container = player.getInventory().getItemInMainHand().getItemMeta().getPersistentDataContainer();
                        String key = container.get(new NamespacedKey(Doorlock.getInstance(), "key"), PersistentDataType.STRING);
                        if (key != null) {
                            sender.sendMessage(Messages.format("command.info.item_key", key));
                        }
                    }

                    // Raytrace
                    Block block = player.getTargetBlockExact(5);
                    if (block != null && block.getType() != Material.AIR) {
                        sender.sendMessage(Messages.format("command.info.target_block", block.getType().toString()));
                        
                        Location doorLoc = null;
                        try {
                            Door d = (Door) block.getBlockData();
                            if (d.getHalf() == Bisected.Half.BOTTOM) {
                                doorLoc = block.getLocation();
                            } else {
                                doorLoc = block.getLocation().subtract(0, 1, 0);
                            }
                        } catch (ClassCastException ex) {
                            try {
                                TrapDoor d = (TrapDoor) block.getBlockData();
                                doorLoc = block.getLocation();
                            } catch (ClassCastException ignored) {}
                        } catch (Exception ignored) {}

                        if (doorLoc == null && SaveUtil.isLockable(block.getLocation())) {
                            doorLoc = block.getLocation();
                        }

                        if (doorLoc != null) {
                            String key = SaveUtil.getKey(doorLoc);
                            if (key != null) {
                                sender.sendMessage(Messages.format("command.info.key_id", key));
                            } else {
                                sender.sendMessage(Messages.get("command.info.no_key"));
                            }
                        } else {
                            sender.sendMessage(Messages.get("command.info.not_lockable"));
                        }
                    }
                }
                return true;
            }
        }
        return false;
    }
}
