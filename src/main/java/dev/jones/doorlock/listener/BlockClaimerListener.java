package dev.jones.doorlock.listener;

import dev.jones.doorlock.Doorlock;
import dev.jones.doorlock.util.DoorlockHearbeat;
import dev.jones.doorlock.util.Messages;
import dev.jones.doorlock.util.SaveUtil;
import dev.jones.doorlock.util.WorldGuardChecks;
import dev.jones.doorlock.util.WorldGuardSupport;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class BlockClaimerListener implements Listener {
    List<Player> timeout=new ArrayList<>();
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        // Проверяем, что игрок кликнул по блоку (не по воздуху)
        if (e.getClickedBlock() == null) return;

        // Получаем предмет в руке
        ItemStack itemInHand = e.getPlayer().getInventory().getItemInMainHand();

        // Если в руке ничего нет или это не специальный ключ — игнорируем
        if (itemInHand.getType() == Material.AIR) return;

        // Проверяем, есть ли у предмета нужный NBT-тег
        if (itemInHand.hasItemMeta()) {
            PersistentDataContainer container = itemInHand.getItemMeta().getPersistentDataContainer();
            boolean isBlockLocker = container.has(
                    new NamespacedKey(Doorlock.getInstance(), "isblocklocker"),
                    PersistentDataType.STRING
            );

            // Если это не ключ — выходим без сообщения
            if (!isBlockLocker) return;
        } else {
            // Если у предмета нет ItemMeta (например, обычный блок) — тоже игнорируем
            return;
        }

        // --- Дальше идёт логика для ключей ---
        if (timeout.contains(e.getPlayer())) {
            e.setCancelled(true);
            return;
        }
        timeout.add(e.getPlayer());
        DoorlockHearbeat.queueRunnable(() -> timeout.remove(e.getPlayer()));

        Location clicked = e.getClickedBlock().getLocation();
        if (WorldGuardSupport.denyIfStrictUnavailable(e.getPlayer(), "block locker interaction")) {
            e.setCancelled(true);
            return;
        }

        Boolean access = checkWorldGuardAccess(e.getPlayer(), clicked, "toggle lockable state");
        if (Boolean.FALSE.equals(access)) {
            e.getPlayer().sendMessage(Messages.get("region.no_build"));
            e.setCancelled(true);
            return;
        }

        e.setCancelled(true);

        if (SaveUtil.isLockable(clicked) && SaveUtil.getKey(clicked) == null) {
            SaveUtil.disableLocking(clicked);
            e.getPlayer().sendMessage(Messages.get("lockable.now_not_lockable"));
        } else if (!SaveUtil.isLockable(clicked) && SaveUtil.getKey(clicked) == null) {
            SaveUtil.enableLocking(clicked);
            e.getPlayer().sendMessage(Messages.get("lockable.now_lockable"));
        } else {
            e.getPlayer().sendMessage(Messages.get("lockable.currently_locked"));
        }
    }

    private Boolean checkWorldGuardAccess(Player player, Location location, String action) {
        if (WorldGuardSupport.shouldSkipChecks()) {
            return null;
        }

        try {
            return WorldGuardChecks.canBuild(player, location);
        } catch (Throwable ex) {
            boolean deny = WorldGuardSupport.handleFailure(player, action, ex);
            return deny ? Boolean.FALSE : null;
        }
    }

}
