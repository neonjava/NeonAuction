package dev.neonjava.neonah.menus;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.List;

public abstract class AHBaseMenu implements InventoryHolder {
    protected final Inventory inventory;

    public AHBaseMenu(int size, net.kyori.adventure.text.Component title) {
        this.inventory = Bukkit.createInventory(this, size, title);
    }

    public abstract void open(Player player);

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    protected ItemStack create(Material mat, net.kyori.adventure.text.Component name,
            net.kyori.adventure.text.Component... lore) {
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(name.decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
            if (lore.length > 0)
                meta.lore(List.of(lore).stream()
                        .map(c -> c.decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false)).toList());
            stack.setItemMeta(meta);
        }
        return stack;
    }

    protected ItemStack create(Material mat, String name, String... lore) {
        return create(mat, dev.neonjava.neonah.managers.ConfigManager.parse(name),
                java.util.Arrays.stream(lore)
                        .map(dev.neonjava.neonah.managers.ConfigManager::parse)
                        .toArray(net.kyori.adventure.text.Component[]::new));
    }
    protected void fill(Material mat) {
        ItemStack filler = create(mat, net.kyori.adventure.text.Component.empty());
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null || inventory.getItem(i).getType() == Material.AIR) {
                inventory.setItem(i, filler);
            }
        }
    }

    protected void fillRange(int start, int end, Material mat) {
        ItemStack filler = create(mat, net.kyori.adventure.text.Component.empty());
        for (int i = start; i < end; i++) {
            inventory.setItem(i, filler);
        }
    }
}
