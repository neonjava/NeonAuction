package dev.neonjava.neonah.menus;

import dev.neonjava.neonah.managers.AHManager;
import dev.neonjava.neonah.models.AHItem;
import dev.neonjava.neonah.utils.AHFormatter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.enchantments.Enchantment;

import java.util.*;

public class AHMainMenu extends AHBaseMenu {
    private final Player player;
    private final boolean admin;
    private int page = 0;
    private SortMode sortMode = SortMode.RECENT;

    // Price threshold for "high value" highlight (configurable)
    private static final double HIGH_PRICE_THRESHOLD = 1_000.0;

    public enum SortMode {
        RECENT("§fMost Recent", Material.CLOCK),
        LOW("§aLowest Price", Material.HOPPER),
        HIGH("§6Highest Price", Material.GOLD_NUGGET);

        public final String label;
        public final Material icon;

        SortMode(String label, Material icon) {
            this.label = label;
            this.icon = icon;
        }
    }

    public AHMainMenu(Player player, boolean admin) {
        super(54, admin ? "§4Admin Auction House" : "§6§lAuction House");
        this.player = player;
        this.admin = admin;
        // Clear any stale pending actions when opening fresh
        AHManager.get().clearPending(player.getUniqueId());
    }

    @Override
    public void open(Player p) {
        render();
        p.openInventory(getInventory());
    }

    public void render() {
        inventory.clear();
        List<AHItem> items = new ArrayList<>(AHManager.get().getItems());

        switch (sortMode) {
            case RECENT -> items.sort(Comparator.comparingLong(AHItem::getListedAt).reversed());
            case LOW -> items.sort(Comparator.comparingDouble(AHItem::getPrice));
            case HIGH -> items.sort(Comparator.comparingDouble(AHItem::getPrice).reversed());
        }

        int start = page * 45;
        int end = Math.min(start + 45, items.size());

        for (int i = start; i < end; i++) {
            AHItem item = items.get(i);
            ItemStack stack = item.getItem().clone();
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add("§8§m                    ");

                boolean isHighValue = item.getPrice() >= HIGH_PRICE_THRESHOLD;
                String priceColor = isHighValue ? "§6§l" : "§e";

                lore.add("§7Seller: §f" + Bukkit.getOfflinePlayer(item.getSeller()).getName());
                lore.add("§7Price: " + priceColor + AHFormatter.formatPrice(item.getPrice()));
                if (isHighValue)
                    lore.add("§6✦ High Value Listing");
                lore.add("§7Expires in: §c" + AHFormatter.formatTimeLeft(item.getListedAt()));
                lore.add("");

                if (admin)
                    lore.add("§c§l⚠ Admin: Click to Force Remove");
                else if (item.getSeller().equals(player.getUniqueId()))
                    lore.add("§c▶ Click to cancel your listing");
                else
                    lore.add("§a▶ Click to purchase");

                meta.setLore(lore);

                // Add enchant glow to high-value items
                if (isHighValue && sortMode == SortMode.HIGH) {
                    meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }
                stack.setItemMeta(meta);
            }
            inventory.setItem(i - start, stack);
        }

        // Bottom border
        for (int i = 45; i < 54; i++)
            inventory.setItem(i, create(Material.BLACK_STAINED_GLASS_PANE, " "));

        // Slot 45 – Sort button (icon changes with mode)
        ItemStack sortIcon = create(sortMode.icon, "§6Sort: " + sortMode.label,
                "§7Click to cycle: Recent → Low → High");
        inventory.setItem(45, sortIcon);

        // Slot 47 – Refresh
        inventory.setItem(47, create(Material.NETHER_STAR, "§b§lRefresh", "§7Click to reload listings."));

        // Slot 48 – Prev page
        if (page > 0)
            inventory.setItem(48, create(Material.ARROW, "§ePrevious Page"));

        // Slot 49 – Close
        inventory.setItem(49, create(Material.BARRIER, "§c§lClose Menu"));

        // Slot 50 – Next page
        if (end < items.size())
            inventory.setItem(50, create(Material.ARROW, "§eNext Page"));

        // Slot 52 – My Listings
        inventory.setItem(52, create(Material.CHEST, "§6My Listings & Returns", "§7View or reclaim your items."));

        // Slot 53 – History
        inventory.setItem(53, create(Material.ENDER_CHEST, "§5Auction History", "§7View your recent sales & buys."));
    }

    public void cycleSort() {
        int next = (sortMode.ordinal() + 1) % SortMode.values().length;
        sortMode = SortMode.values()[next];
        page = 0;
        render();
    }

    public void refresh() {
        render();
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

    public boolean isAdmin() {
        return admin;
    }

    public SortMode getSortMode() {
        return sortMode;
    }
}
