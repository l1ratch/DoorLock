package dev.jones.doorlock.util;

import org.jetbrains.annotations.NotNull;
import dev.jones.doorlock.Doorlock;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class ItemStackBuilder {
    private String name;
    private Material material;
    private List<String> lore;
    private Hashtable<String,String> tags = new Hashtable<>();
    private ItemStack stack;
    private Integer customModelData; // Добавляем поле для CustomModelData
    private String dynamicName;

    public ItemStackBuilder(Material material) {
        this.material = material;
    }

    public ItemStackBuilder(@NotNull ItemStack stack) {
        this.material = stack.getType();
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            this.name = meta.getDisplayName();
            this.lore = meta.getLore();
            this.customModelData = meta.hasCustomModelData() ? meta.getCustomModelData() : null;
        }
        this.stack = stack;
    }

    public ItemStackBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public ItemStackBuilder setLore(String... lore) {
        this.lore = Arrays.asList(lore);
        return this;
    }

    public ItemStackBuilder addNbtTag(String key, String value) {
        tags.put(key, value);
        return this;
    }

    // Добавляем метод для установки CustomModelData
    public ItemStackBuilder setCustomModelData(int customModelData) {
        this.customModelData = customModelData;
        return this;
    }

    public ItemStackBuilder setDynamicName(String name) {
        this.dynamicName = name;
        return this;
    }

    public ItemStack build() {
        ItemStack stack = this.stack;
        if (stack == null) {
            stack = new ItemStack(material);
        }
        ItemMeta meta = stack.getItemMeta();
        if (name != null) meta.setDisplayName(name);
        if (lore != null) meta.setLore(lore);

        // Устанавливаем CustomModelData, если он задан
        if (customModelData != null) {
            meta.setCustomModelData(customModelData);
        }

        // Устанавливаем имя (статическое или динамическое)
        if (dynamicName != null) {
            meta.setDisplayName(dynamicName);
        } else if (name != null) {
            meta.setDisplayName(name);
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        tags.forEach((key, value) -> {
            NamespacedKey nkey = new NamespacedKey(Doorlock.getInstance(), key);
            container.set(nkey, PersistentDataType.STRING, value);
        });
        stack.setItemMeta(meta);
        return stack;
    }
}