package dev.jones.doorlock.command;

import dev.jones.doorlock.util.SaveUtil;
import dev.jones.doorlock.util.Updater;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UnlockCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(args.length!=0)return false;
        if(!(sender instanceof Player)){
            sender.sendMessage("§cЧтобы выполнить эту команду, вы должны быть игроком!");
            return true;
        }
        Player p=(Player) sender;
        Block target=p.getTargetBlockExact(5);
        if(target==null){
            p.sendMessage("§cПожалуйста, посмотрите на блок, который вы хотите разблокировать.");
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
                /*
                 * No Door Clicked
                 */
            }
        }catch (Exception ignored){
            /*
             *  Invalid block clicked
             */
        }
        if(door==null){
            if(SaveUtil.isLockable(target.getLocation())){
                door=target.getLocation();
            }else {
                p.sendMessage("§cПожалуйста, посмотрите на дверь или запертый блок!");
                return true;
            }
        }
        if(SaveUtil.getKey(door)==null){
            p.sendMessage("§cЭтот блок не заблокирован!");
            return true;
        }
        SaveUtil.unlockDoor(door);
        p.sendMessage("§aБлок был разблокирован!");


        return true;
    }
}
