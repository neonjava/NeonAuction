package dev.neonjava.neonah.menus;

import dev.neonjava.neonah.managers.AHManager;
import dev.neonjava.neonah.models.AHItem;
import dev.neonjava.neonah.utils.AHFormatter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * AHMyListingsMenu - My Listings & Returns.
 * Shows active listings and expired items for reclamation.
 */
public class AHMyListingsMenu extends AHBaseMenu {
    private final Player player;

    public AHMyListingsMenu(Player player) {
        super(54, "§6§l" + AHFormatter.toSmallCaps("My Listings"));
        this.player = player;
    }

    @Override
    public void open(Player p) {
        render();
        p.openInventory(getInventory());
    }

    public void render() {
        inventory.clear();
        UUID uuid = player.getUniqueId();

        // Active Listings (Top 27 slots)
        List<AHItem> active = AHManager.get().getItems().stream()
                .filter(i -> i.getSeller().equals(uuid))
                .toList();

        for (int i = 0; i < Math.min(active.size(), 27); i++) {
            AHItem item = active.get(i);
            ItemStack stack = item.getItem().clone();
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add("§8§m                    ");
                lore.add("§7Type: §a§l" + AHFormatter.toSmallCaps("Active"));
                lore.add("§7Price: §e" + AHFormatter.formatPrice(item.getPrice()));
                lore.add("§c▶ Click to cancel listing");
                meta.setLore(lore);
                stack.setItemMeta(meta);
            }
            inventory.setItem(i, stack);
        }

        // Unclaimed Items (Middle 18 slots)
        List<ItemStack> unclaimed = AHManager.get().getUnclaimed(uuid);
        for (int i = 0; i < Math.min(unclaimed.size(), 18); i++) {
            ItemStack stack = unclaimed.get(i).clone();
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add("§8§m                    ");
                lore.add("§7Type: §c§l" + AHFormatter.toSmallCaps("Unlisted / Reclaim"));
                lore.add("§e▶ Click to reclaim all items");
                meta.setLore(lore);
                stack.setItemMeta(meta);
            }
            inventory.setItem(27 + i, stack);
        }

        // Bottom Bar
        for (int i = 45; i < 54; i++)
            inventory.setItem(i, create(Material.BLACK_STAINED_GLASS_PANE, " "));

        // Back button
        inventory.setItem(49, create(Material.RED_STAINED_GLASS_PANE, "§c§l" + AHFormatter.toSmallCaps("Back to AH")));
    }
}
