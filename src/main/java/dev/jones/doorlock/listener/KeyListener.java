package dev.jones.doorlock.listener;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import dev.jones.doorlock.Doorlock;
import dev.jones.doorlock.util.DoorlockHearbeat;
import dev.jones.doorlock.util.ItemStackBuilder;
import dev.jones.doorlock.util.Messages;
import dev.jones.doorlock.util.SaveUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class KeyListener implements Listener {
    private static List<Player> timeout=new ArrayList<>();
    @EventHandler
    public void onCraft(CraftItemEvent e) {
        ItemStack result = e.getRecipe().getResult();
        if (result == null || result.getType().isAir()) return;

        ItemMeta meta = result.getItemMeta();
        if (meta == null) return; // защита от NPE

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(new NamespacedKey(Doorlock.getInstance(), "iskey"), PersistentDataType.STRING)) {
            e.setCurrentItem(new ItemStackBuilder(result)
                    .addNbtTag("key", UUID.randomUUID().toString())
                    .build());
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getPlayer().hasPermission("doorlock.bypass")) {
            return;
        }
        if (timeout.contains(e.getPlayer())) return;
        timeout.add(e.getPlayer());
        DoorlockHearbeat.queueRunnable(() -> timeout.remove(e.getPlayer()));

        if (e.getClickedBlock() == null) return;

        Location door = null;
        try {
            Door d = (Door) e.getClickedBlock().getBlockData();
            if (d.getHalf() == Bisected.Half.BOTTOM) {
                door = e.getClickedBlock().getLocation();
            } else {
                door = e.getClickedBlock().getLocation().subtract(0, 1, 0);
            }
        } catch (ClassCastException ex) {
            try {
                TrapDoor d = (TrapDoor) e.getClickedBlock().getBlockData();
                door = e.getClickedBlock().getLocation();
            } catch (ClassCastException ignored) {
                // Не дверь
            }
        } catch (Exception ignored) {
            // Неверный блок
        }

        if (door == null) return;

        // --- достаем key у игрока ---
        String key = "missing";
        if (e.getPlayer().getInventory().getItemInMainHand().hasItemMeta()) {
            PersistentDataContainer container = e.getPlayer().getInventory().getItemInMainHand().getItemMeta().getPersistentDataContainer();
            String found = container.get(new NamespacedKey(Doorlock.getInstance(), "key"), PersistentDataType.STRING);
            if (found != null) {
                key = found;
            }
            if (container.has(new NamespacedKey(Doorlock.getInstance(), "isdoordrill"), PersistentDataType.STRING)) {
                if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
                    e.setCancelled(false);
                } else {
                    e.getPlayer().sendMessage(Messages.get("item.door_drill.cannot_interact"));
                }
                return;
            }
        }

        // --- если дверь еще не имеет ключа и у игрока есть ключ ---
        if (SaveUtil.getKey(door) == null && !key.equals("missing")) {
            if (!hasRegionAccess(e.getPlayer(), door)) {
                e.getPlayer().sendMessage(Messages.get("region.no_build"));
                e.setCancelled(true);
                return;
            }
            SaveUtil.lockDoor(key, door);
            e.getPlayer().sendMessage(Messages.get("door.locked"));
            e.setCancelled(true);
            return;
        }

        boolean locked = false;
        if (SaveUtil.getKey(door) != null) {
            e.setCancelled(true);
            locked = true;
        }

        if (e.getPlayer().getInventory().getItemInMainHand().getItemMeta() == null) {
            if (locked) {
                e.getPlayer().sendMessage(Messages.get("door.need_key"));
            }
            return;
        }

        // --- проверка ключа ---
        if (key.equals(SaveUtil.getKey(door))) {
            e.setCancelled(false);
            if (e.getPlayer().isSneaking() && e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                SaveUtil.unlockDoor(door);
                e.getPlayer().sendMessage(Messages.get("door.unlocked"));
            }
        } else if (SaveUtil.getKey(door) == null && !key.equals("missing")) {
            SaveUtil.lockDoor(key, door);
            e.getPlayer().sendMessage(Messages.get("door.locked"));
            e.setCancelled(true);
        } else if (SaveUtil.getKey(door) == null) {
            // игнорируем, если блок без ключа
        } else {
            e.getPlayer().sendMessage(Messages.get("door.need_key"));
        }
    }


    private boolean hasRegionAccess(Player player, Location location) {
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionQuery query = container.createQuery();
            ApplicableRegionSet regions = query.getApplicableRegions(BukkitAdapter.adapt(location));

            LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);

            // проверка bypass
            if (!WorldGuard.getInstance().getPlatform().getSessionManager().hasBypass(localPlayer, localPlayer.getWorld())) {
                return regions.testState(localPlayer, Flags.BUILD);
            }
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return true; // fallback: разрешить
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e){
        Location door=null;
        try {
            Door d = (Door) e.getBlock().getBlockData();
            if(d.getHalf()==Bisected.Half.BOTTOM){
                door=e.getBlock().getLocation();
            }else{
                door=e.getBlock().getLocation().subtract(0,1,0);
            }
        }catch (ClassCastException ex){
            try {
                TrapDoor d = (TrapDoor)e.getBlock().getBlockData();
                door=e.getBlock().getLocation();
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
            if(SaveUtil.isLockable(e.getBlock().getLocation())){
                door=e.getBlock().getLocation();
            }else{
                return;
            }
        }

        if (e.getPlayer().hasPermission("doorlock.bypass")) {
            return;
        }
        // Проверяем доступ к региону
        if(!hasRegionAccess(e.getPlayer(), door)) {
            e.getPlayer().sendMessage(Messages.get("region.cannot_remove_protection"));
            e.setCancelled(true);
            return;
        }

        SaveUtil.unlockDoor(door);
        SaveUtil.disableLocking(door);

    }
}
