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
        super(54, net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(
                "<gradient:#f6d365:#fda085><bold>" + AHFormatter.toSmallCaps("My Listings")));
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
        net.kyori.adventure.text.minimessage.MiniMessage mm = net.kyori.adventure.text.minimessage.MiniMessage
                .miniMessage();
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
                List<net.kyori.adventure.text.Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore())
                        : new ArrayList<>();
                lore.add(mm.deserialize("<dark_gray><strikethrough>                    "));
                lore.add(mm.deserialize("<gray>Type: <green><bold>" + AHFormatter.toSmallCaps("Active")));
                lore.add(mm.deserialize(ConfigManager.get().getMessage("lore.price")
                        .replace("%price%", AHFormatter.formatPrice(item.getPrice()))));
                lore.add(mm.deserialize(ConfigManager.get().getMessage("lore.cancel")));
                meta.lore(lore.stream()
                        .map(c -> c.decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false)).toList());
                stack.setItemMeta(meta);
            }
            inventory.setItem(i - start, stack);
        }

        // Unclaimed Items (Middle 18 slots)
        List<ItemStack> unclaimed = AHManager.get().getUnclaimed(uuid);
        for (int i = 0; i < Math.min(unclaimed.size(), 18); i++) {
            ItemStack stack = unclaimed.get(i).clone();
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                List<net.kyori.adventure.text.Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore())
                        : new ArrayList<>();
                lore.add(mm.deserialize("<dark_gray><strikethrough>                    "));
                lore.add(mm.deserialize("<gray>Type: <red><bold>" + AHFormatter.toSmallCaps("Unlisted / Reclaim")));
                lore.add(mm.deserialize(ConfigManager.get().getMessage("lore.reclaim")));
                meta.lore(lore.stream()
                        .map(c -> c.decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false)).toList());
                stack.setItemMeta(meta);
            }
            inventory.setItem(27 + i, stack);
        }

        // Bottom Bar
        ItemStack glass = create(Material.BLACK_STAINED_GLASS_PANE, mm.deserialize(" "));
        for (int i = 45; i < 54; i++)
            inventory.setItem(i, glass);

        // Status Item (Page Info)
        int totalPages = (int) Math.ceil(active.size() / 27.0);
        inventory.setItem(45, create(Material.PAPER,
                mm.deserialize("<yellow><bold>Page: <white>" + (page + 1) + " / " + Math.max(1, totalPages)),
                mm.deserialize("<gray>Active listings: <aqua>" + active.size())));

        // Back button
        inventory.setItem(49, create(Material.RED_STAINED_GLASS_PANE,
                mm.deserialize("<red><bold>" + AHFormatter.toSmallCaps("Back to AH"))));

        // Pagination Arrows
        if (page > 0) {
            inventory.setItem(48,
                    create(Material.ARROW, mm.deserialize("<yellow>" + AHFormatter.toSmallCaps("Previous Page"))));
        }
        if (end < active.size()) {
            inventory.setItem(50,
                    create(Material.ARROW, mm.deserialize("<yellow>" + AHFormatter.toSmallCaps("Next Page"))));
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
