package dev.jones.doorlock.command;

import dev.jones.doorlock.Doorlock;
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
                sender.sendMessage("§cНа этом сервере установлен Doorlock от _joones");
                sender.sendMessage("§7Версия плагина: "+ SaveUtil.getVersion());
                sender.sendMessage("§6Обновил и перевел плагин: l1ratch");
                return true;
            }else if(args[0].equalsIgnoreCase("help")){
                sender.sendMessage("§6Плагин DoorLock от _joones (https://github.com/SJones-BWGY/DoorLock)");
                sender.sendMessage("§6Обновил и перевел на русский язык: l1ratch (https://github.com/l1ratch)");
                sender.sendMessage("§6Spigot официального плагина: https://www.spigotmc.org/resources/doorlock.96169");
                sender.sendMessage("§6Страница моего форка плагина: --");
                Updater.fetchUpdates();
                return true;
            }else if(args[0].equalsIgnoreCase("update")){
                sender.sendMessage("§cПроверяю наличие обновлений...");
                sender.sendMessage("§7Дополнительную информацию можно найти в консоли.");
                Updater.fetchUpdates();
                return true;
            }else if(args[0].equalsIgnoreCase("reload")){
                sender.sendMessage("§cПерезагружаем плагин...");
                sender.sendMessage("§7Это может занять некоторое время.");
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
