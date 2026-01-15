package dev.jones.doorlock.command;

import dev.jones.doorlock.util.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DebugCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("getitemtags")) {
            if (sender instanceof Player) {
                Player p = (Player) sender;
                p.sendMessage(Messages.get("debug.command_removed"));
            } else {
                sender.sendMessage(Messages.get("debug.command_removed"));
            }
            return true;
        }
        return false;
    }
}
