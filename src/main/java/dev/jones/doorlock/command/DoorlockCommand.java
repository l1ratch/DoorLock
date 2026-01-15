package dev.jones.doorlock.command;

import dev.jones.doorlock.Doorlock;
import dev.jones.doorlock.util.Messages;
import dev.jones.doorlock.util.SaveUtil;
import dev.jones.doorlock.util.Updater;
import org.bukkit.Bukkit;
import org.bukkit.block.data.type.Door;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;

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
                Bukkit.getScheduler().scheduleSyncDelayedTask(Doorlock.getInstance(),()->{
                    Doorlock.getInstance().getServer().resetRecipes();
                    Doorlock.getInstance().onEnable();
                },1);

                return true;
            }
        }
        return false;
    }
}
