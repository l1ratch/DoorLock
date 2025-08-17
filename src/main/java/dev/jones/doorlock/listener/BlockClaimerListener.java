package dev.jones.doorlock.listener;

import dev.jones.doorlock.Doorlock;
import dev.jones.doorlock.util.DoorlockHearbeat;
import dev.jones.doorlock.util.SaveUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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

        e.setCancelled(true);

        if (SaveUtil.isLockable(e.getClickedBlock().getLocation()) && SaveUtil.getKey(e.getClickedBlock().getLocation()) == null) {
            SaveUtil.disableLocking(e.getClickedBlock().getLocation());
            e.getPlayer().sendMessage("§a§lБлок больше нельзя заблокировать!");
        } else if (!SaveUtil.isLockable(e.getClickedBlock().getLocation()) && SaveUtil.getKey(e.getClickedBlock().getLocation()) == null) {
            SaveUtil.enableLocking(e.getClickedBlock().getLocation());
            e.getPlayer().sendMessage("§a§lТеперь блок можно заблокировать!");
        } else {
            e.getPlayer().sendMessage("§c§lЭтот блок в данный момент заблокирован!");
        }
    }

}
