package dev.jones.doorlock.command;

import dev.jones.doorlock.Doorlock;
import dev.jones.doorlock.util.ItemStackBuilder;
import dev.jones.doorlock.util.Messages;
import dev.jones.doorlock.util.SaveUtil;
import dev.jones.doorlock.util.Updater;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
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
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class DoorlockCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) return false;

        String sub = args[0];

        if(sub.equalsIgnoreCase("help")){
            sender.sendMessage(Messages.format("doorlock.help.line1", SaveUtil.getVersion()));
            sender.sendMessage(Messages.get("doorlock.help.line2"));
            sender.sendMessage(Messages.get("doorlock.help.line3"));
            sender.sendMessage(Messages.get("doorlock.help.line4"));
            sender.sendMessage(Messages.get("doorlock.help.line5"));
            sender.sendMessage(Messages.get("doorlock.help.line6"));
            if (sender.hasPermission("doorlock.admin")) {
                sender.sendMessage(Messages.get("doorlock.help.line6_extra")); // getkey [id]
                sender.sendMessage(Messages.get("doorlock.help.line8")); // getlocker
                sender.sendMessage(Messages.get("doorlock.help.line9")); // getdrill
                sender.sendMessage(Messages.get("doorlock.help.line_update")); // update
            }
            sender.sendMessage(Messages.get("doorlock.help.line7"));
            // Updater.fetchUpdates(); // Don't fetch updates on help, it's annoying and slow
            return true;
        }else if(sub.equalsIgnoreCase("update")){
            if (!sender.hasPermission("doorlock.admin")) {
                sender.sendMessage(Messages.get("command.no_permission"));
                return true;
            }
            sender.sendMessage(Messages.get("doorlock.update.header"));
            sender.sendMessage(Messages.format("doorlock.update.current", SaveUtil.getVersion()));
            sender.sendMessage(Messages.get("doorlock.update.checking"));
            sender.sendMessage(Messages.get("doorlock.update.console_info"));
            Updater.fetchUpdates();
            return true;
        }else if(sub.equalsIgnoreCase("reload")){
            if (!sender.hasPermission("doorlock.admin")) {
                sender.sendMessage(Messages.get("command.no_permission"));
                return true;
            }
            sender.sendMessage(Messages.get("doorlock.reload.start"));
            sender.sendMessage(Messages.get("doorlock.reload.may_take_time"));
            
            Doorlock.getInstance().reloadConfig();
            Messages.init();
            
            if (Doorlock.getInstance().getConfig().getBoolean("update", true)) {
                Updater.fetchUpdates();
            }

            sender.sendMessage(Messages.get("doorlock.reload.success"));
            return true;
        }else if(sub.equalsIgnoreCase("getkey")){
            if (!sender.hasPermission("doorlock.admin")) {
                sender.sendMessage(Messages.get("command.no_permission"));
                return true;
            }
            if (!(sender instanceof Player)) {
                sender.sendMessage(Messages.get("command.only_player"));
                return true;
            }
            Player player = (Player) sender;
            String uuid;
            if (args.length > 1) {
                try {
                    UUID.fromString(args[1]);
                    uuid = args[1];
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(Messages.get("command.getkey.invalid_uuid"));
                    return true;
                }
            } else {
                uuid = UUID.randomUUID().toString();
            }

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
        }else if(sub.equalsIgnoreCase("getlocker")){
            if (!sender.hasPermission("doorlock.admin")) {
                sender.sendMessage(Messages.get("command.no_permission"));
                return true;
            }
            if (!(sender instanceof Player)) {
                sender.sendMessage(Messages.get("command.only_player"));
                return true;
            }
            Player player = (Player) sender;
            ItemStack blockClaimerItem=new ItemStackBuilder(Material.IRON_AXE)
                    .setName(Messages.get("item.blocklocker.name"))
                    .setLore(Messages.get("item.blocklocker.lore1"))
                    .addNbtTag("isblocklocker","1")
                    .setCustomModelData(9999102)
                    .build();
            player.getInventory().addItem(blockClaimerItem);
            sender.sendMessage(Messages.get("command.getlocker.success"));
            return true;
        }else if(sub.equalsIgnoreCase("getdrill")){
            if (!sender.hasPermission("doorlock.admin")) {
                sender.sendMessage(Messages.get("command.no_permission"));
                return true;
            }
            if (!(sender instanceof Player)) {
                sender.sendMessage(Messages.get("command.only_player"));
                return true;
            }
            Player player = (Player) sender;
            ItemStack doorDrillItem=new ItemStackBuilder(Material.DIAMOND_AXE)
                    .setName(Messages.get("item.doordrill.name"))
                    .setLore(Messages.get("item.doordrill.lore1"))
                    .addNbtTag("isdoordrill","1")
                    .setCustomModelData(9999103)
                    .build();
            Damageable doorDrillMeta=(Damageable) doorDrillItem.getItemMeta();
            if (doorDrillMeta != null) {
                doorDrillMeta.setDamage(1550);
                doorDrillItem.setItemMeta((ItemMeta) doorDrillMeta);
            }
            player.getInventory().addItem(doorDrillItem);
            sender.sendMessage(Messages.get("command.getdrill.success"));
            return true;
        }else if(sub.equalsIgnoreCase("info")){
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
                        sendInteractiveKey(player, "command.info.item_key", key);
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
                            sendInteractiveKey(player, "command.info.key_id", key);
                        } else {
                            sender.sendMessage(Messages.get("command.info.no_key"));
                        }
                    } else {
                        sender.sendMessage(Messages.get("command.info.not_lockable"));
                    }
                }
            }
            return true;
        }else if(sub.equalsIgnoreCase("unlock")){
            if (!sender.hasPermission("doorlock.unlock")) {
                sender.sendMessage(Messages.get("command.no_permission"));
                return true;
            }
            if(!(sender instanceof Player)){
                sender.sendMessage(Messages.get("command.only_player"));
                return true;
            }
            Player p=(Player) sender;
            Block target=p.getTargetBlockExact(5);
            if(target==null){
                p.sendMessage(Messages.get("door.look_at_block"));
                return true;
            }
            Location door=null;
            try {
                Door d = (Door) target.getBlockData();
                if(d.getHalf()== Bisected.Half.BOTTOM){
                    door=target.getLocation();
                }else{
                    door=target.getLocation().subtract(0,1,0);
                }
            }catch (ClassCastException ex){
                try {
                    TrapDoor d = (TrapDoor) target.getBlockData();
                    door=target.getLocation();
                }catch (ClassCastException ignored){
                }
            }catch (Exception ignored){
            }
            if(door==null){
                if(SaveUtil.isLockable(target.getLocation())){
                    door=target.getLocation();
                }else {
                    p.sendMessage(Messages.get("door.look_at_door_or_locked"));
                    return true;
                }
            }
            if(SaveUtil.getKey(door)==null){
                p.sendMessage(Messages.get("door.not_locked"));
                return true;
            }
            SaveUtil.unlockDoor(door);
            p.sendMessage(Messages.get("command.unlock.success"));

            return true;
        }
        
        return false;
    }

    private void sendInteractiveKey(Player p, String formatPath, String key) {
        String raw = Messages.get(formatPath);
        int index = raw.indexOf("%s");
        if (index == -1) {
            p.sendMessage(Messages.format(formatPath, key));
            return;
        }
        String prefix = raw.substring(0, index);
        String suffix = raw.substring(index + 2);

        TextComponent message = new TextComponent("");
        for (net.md_5.bungee.api.chat.BaseComponent b : TextComponent.fromLegacyText(prefix)) {
            message.addExtra(b);
        }

        TextComponent keyComp = new TextComponent(key);
        // We assume the color comes from prefix, but let's force Yellow if it was intended to be emphasized.
        // In messages.yml it's usually "&e%s". The prefix will end with &e.
        // However, fromLegacyText might close colors.
        // Let's rely on Last Colors of prefix?
        // Actually, explicit color is safer if we know it should be yellow.
        // But the user might change color in config.
        // A simple workaround: Just use the key text. The client usually inherits color.
        
        keyComp.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/dl getkey " + key));
        keyComp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§aНажмите, чтобы получить копию ключа\n§7(Вставит команду в чат)")));

        message.addExtra(keyComp);
        
        for (net.md_5.bungee.api.chat.BaseComponent b : TextComponent.fromLegacyText(suffix)) {
            message.addExtra(b);
        }

        p.spigot().sendMessage(message);
    }
}
