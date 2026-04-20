package dev.neonjava.neonah.menus;

import dev.neonjava.neonah.managers.AHManager;
import dev.neonjava.neonah.managers.ConfigManager;
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
        RECENT("Most Recent", Material.CLOCK),
        LOW("Lowest Price", Material.HOPPER),
        HIGH("Highest Price", Material.GOLD_NUGGET);

        public final String label;
        public final Material icon;

        SortMode(String label, Material icon) {
            this.label = label;
            this.icon = icon;
        }
    }

    public AHMainMenu(Player player, boolean admin) {
        super(54, net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(
                admin ? "<gradient:#ff0844:#ffb199><bold>" + AHFormatter.toSmallCaps("Admin Auction House")
                        : "<gradient:#f6d365:#fda085><bold>" + AHFormatter.toSmallCaps("Auction House")));
        this.player = player;
        this.admin = admin;
        AHManager.get().clearPending(player.getUniqueId());
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
                List<net.kyori.adventure.text.Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore())
                        : new ArrayList<>();
                lore.add(mm.deserialize("<dark_gray><strikethrough>                    "));

                boolean isHighValue = item.getPrice() >= HIGH_PRICE_THRESHOLD;
                String sellerName = Bukkit.getOfflinePlayer(item.getSeller()).getName();
                if (sellerName == null || sellerName.isBlank()) {
                    sellerName = item.getSeller().toString();
                }

                lore.add(ConfigManager.parse(ConfigManager.get().getMessage("lore.seller")
                        .replace("%name%", sellerName)));
                lore.add(ConfigManager.parse(ConfigManager.get().getMessage("lore.price")
                        .replace("%price%", AHFormatter.formatPrice(item.getPrice()))));

                if (isHighValue)
                    lore.add(ConfigManager.get().getMiniMessage("lore.high-value"));

                lore.add(ConfigManager.parse(ConfigManager.get().getMessage("lore.expires")
                        .replace("%time%", AHFormatter.formatTimeLeft(item.getListedAt()))));
                lore.add(net.kyori.adventure.text.Component.empty());

                if (admin)
                    lore.add(ConfigManager.parse("<red><bold>⚠ Admin: Click to Force Remove"));
                else if (item.getSeller().equals(player.getUniqueId()))
                    lore.add(ConfigManager.get().getMiniMessage("lore.cancel"));
                else
                    lore.add(ConfigManager.get().getMiniMessage("lore.buy"));

                meta.lore(lore.stream()
                        .map(c -> c.decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false)).toList());

                if (isHighValue && sortMode == SortMode.HIGH) {
                    meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }
                stack.setItemMeta(meta);
            }
            inventory.setItem(i - start, stack);
        }

        // Bottom border
        ItemStack glass = create(Material.BLACK_STAINED_GLASS_PANE, mm.deserialize(" "));
        for (int i = 45; i < 54; i++)
            inventory.setItem(i, glass);

        // Slot 45 – Sort button
        inventory.setItem(45, create(sortMode.icon,
                ConfigManager.parse("<yellow><bold>" + AHFormatter.toSmallCaps("Sort: " + sortMode.label)),
                ConfigManager.parse("<gray>Click to cycle: Recent → Low → High")));

        // Slot 46 – Refresh
        inventory.setItem(46, create(Material.NETHER_STAR,
                ConfigManager.parse("<aqua><bold>" + AHFormatter.toSmallCaps("Refresh")),
                ConfigManager.parse("<gray>Click to reload listings.")));

        // Slot 48 – Prev page
        if (page > 0)
            inventory.setItem(48,
                    create(Material.ARROW, ConfigManager.parse("<yellow>" + AHFormatter.toSmallCaps("Previous Page"))));

        // Slot 49 – Close
        inventory.setItem(49, create(Material.RED_STAINED_GLASS_PANE,
                ConfigManager.parse("<red><bold>" + AHFormatter.toSmallCaps("Close Menu"))));

        // Slot 50 – Next page
        if (end < items.size())
            inventory.setItem(50,
                    create(Material.ARROW, ConfigManager.parse("<yellow>" + AHFormatter.toSmallCaps("Next Page"))));

        // Slot 52 – My Listings
        inventory.setItem(52, create(Material.CHEST,
                ConfigManager.parse("<gold><bold>" + AHFormatter.toSmallCaps("My Listings")),
                ConfigManager.parse("<gray>View or reclaim your items.")));

        // Slot 53 – History
        inventory.setItem(53, create(Material.ENDER_CHEST,
                ConfigManager.parse("<light_purple><bold>" + AHFormatter.toSmallCaps("Auction History")),
                ConfigManager.parse("<gray>View your recent sales & buys.")));
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
