package dev.jones.doorlock.command;

import dev.jones.doorlock.util.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class DisabledCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage(Messages.get("debug.command_disabled"));
        return true;
    }
}
