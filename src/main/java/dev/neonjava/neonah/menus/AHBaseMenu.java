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

    public AHBaseMenu(int size, String title) {
        this.inventory = Bukkit.createInventory(this, size, title);
    }

    public abstract void open(Player player);

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    protected ItemStack create(Material mat, String name, String... lore) {
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0)
                meta.setLore(List.of(lore));
            stack.setItemMeta(meta);
        }
        return stack;
    }
}
