package dev.jones.doorlock.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class DoorlockCommandTabCompleter implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> l=new ArrayList<>();
        if(args.length==1){
            l.add("help");
            l.add("version");
            l.add("update");
            l.add("reload");
        }
        return l;
    }
}
