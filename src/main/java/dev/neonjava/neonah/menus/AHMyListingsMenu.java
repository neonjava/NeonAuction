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
        super(54, ConfigManager.parse(
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

        // ─────────────────────────────────────────────────────────────
        // Returns & Navigation (Bottom rows)
        // ─────────────────────────────────────────────────────────────

        // Returns (Slots 27-35) — claim specific item by clicking it
        List<dev.neonjava.neonah.managers.AHStorage.UnclaimedEntry> returns = AHManager.get().getUnclaimedEntries(uuid);
        for (int j = 0; j < Math.min(9, returns.size()); j++) {
            ItemStack stack = returns.get(j).item().clone();
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                List<net.kyori.adventure.text.Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore())
                        : new ArrayList<>();
                lore.add(mm.deserialize("<dark_gray><strikethrough>                    "));
                lore.add(mm.deserialize("<gradient:#84fab0:#8fd3f4>▶ Click to collect this item</gradient>"));
                lore.add(mm.deserialize("<gray>Unclaimed retention: <yellow>"
                        + ConfigManager.get().getConfig().getLong("settings.unclaimed-retention-days", 10L) + "d"));
                meta.lore(lore.stream()
                        .map(c -> c.decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false)).toList());
                stack.setItemMeta(meta);
            }
            inventory.setItem(27 + j, stack);
        }

        // Navigation (Bottom Row)
        ItemStack glass = create(Material.BLACK_STAINED_GLASS_PANE, ConfigManager.parse(" "));
        for (int j = 45; j < 54; j++)
            inventory.setItem(j, glass);

        if (page > 0)
            inventory.setItem(48,
                    create(Material.ARROW, ConfigManager.parse("<yellow>" + AHFormatter.toSmallCaps("Previous Page"))));

        inventory.setItem(49, create(Material.RED_STAINED_GLASS_PANE,
                ConfigManager.parse("<red><bold>" + AHFormatter.toSmallCaps("Back to Menu"))));

        if (end < active.size())
            inventory.setItem(50,
                    create(Material.ARROW, ConfigManager.parse("<yellow>" + AHFormatter.toSmallCaps("Next Page"))));
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
