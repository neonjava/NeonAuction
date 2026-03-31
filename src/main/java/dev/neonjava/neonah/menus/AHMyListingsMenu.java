package dev.neonjava.neonah.menus;

import dev.neonjava.neonah.managers.AHManager;
import dev.neonjava.neonah.managers.ConfigManager;
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
 * AHMyListingsMenu - My Listings & Returns with Pagination.
 * Shows active listings (paginated) and expired items.
 */
public class AHMyListingsMenu extends AHBaseMenu {
    private final Player player;
    private int page = 0;

    public AHMyListingsMenu(Player player) {
        this(player, 0);
    }

    public AHMyListingsMenu(Player player, int page) {
        super(54, "§6§l" + AHFormatter.toSmallCaps("My Listings"));
        this.player = player;
        this.page = page;
    }

    @Override
    public void open(Player p) {
        render();
        p.openInventory(getInventory());
    }

    public void render() {
        inventory.clear();
        UUID uuid = player.getUniqueId();

        // Active Listings (Top 27 slots, paginated)
        List<AHItem> active = AHManager.get().getItems().stream()
                .filter(i -> i.getSeller().equals(uuid))
                .toList();

        int start = page * 27;
        int end = Math.min(start + 27, active.size());

        for (int i = start; i < end; i++) {
            AHItem item = active.get(i);
            ItemStack stack = item.getItem().clone();
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add("§8§m                    ");
                lore.add("§7Type: §a§l" + AHFormatter.toSmallCaps("Active"));
                lore.add(AHFormatter.toSmallCaps(AHFormatter.stripBold(ConfigManager.get().getMessage("lore.price")
                        .replace("%price%", AHFormatter.formatPrice(item.getPrice())))));
                lore.add(AHFormatter.toSmallCaps(AHFormatter.stripBold(ConfigManager.get().getMessage("lore.cancel"))));
                meta.setLore(lore);
                stack.setItemMeta(meta);
            }
            inventory.setItem(i - start, stack);
        }

        // Unclaimed Items (Middle 18 slots, no pagination for now)
        List<ItemStack> unclaimed = AHManager.get().getUnclaimed(uuid);
        for (int i = 0; i < Math.min(unclaimed.size(), 18); i++) {
            ItemStack stack = unclaimed.get(i).clone();
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add("§8§m                    ");
                lore.add("§7Type: §c§l" + AHFormatter.toSmallCaps("Unlisted / Reclaim"));
                lore.add(
                        AHFormatter.toSmallCaps(AHFormatter.stripBold(ConfigManager.get().getMessage("lore.reclaim"))));
                meta.setLore(lore);
                stack.setItemMeta(meta);
            }
            inventory.setItem(27 + i, stack);
        }

        // Bottom Bar
        for (int i = 45; i < 54; i++)
            inventory.setItem(i, create(Material.BLACK_STAINED_GLASS_PANE, " "));

        // Status Item (Page Info)
        int totalPages = (int) Math.ceil(active.size() / 27.0);
        inventory.setItem(45, create(Material.PAPER, "§e§lPage: §f" + (page + 1) + " / " + Math.max(1, totalPages),
                "§7Active listings: §b" + active.size()));

        // Back button
        inventory.setItem(49, create(Material.RED_STAINED_GLASS_PANE, "§c§l" + AHFormatter.toSmallCaps("Back to AH")));

        // Pagination Arrows
        if (page > 0) {
            inventory.setItem(48, create(Material.ARROW, "§e" + AHFormatter.toSmallCaps("Previous Page")));
        }
        if (end < active.size()) {
            inventory.setItem(50, create(Material.ARROW, "§e" + AHFormatter.toSmallCaps("Next Page")));
        }
    }

    public void next() {
        page++;
        render();
    }

    public void prev() {
        if (page > 0)
            page--;
        render();
    }

    public int getPage() {
        return page;
    }
}
