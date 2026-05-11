package dev.neonjava.neonah.menus;

import dev.neonjava.neonah.managers.AHManager;
import dev.neonjava.neonah.managers.ConfigManager;
import dev.neonjava.neonah.models.AHItem;
import dev.neonjava.neonah.utils.AHFormatter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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
        this(player, admin, 0, SortMode.RECENT);
    }

    public AHMainMenu(Player player, boolean admin, int page, SortMode sortMode) {
        super(54, ConfigManager.get().getMiniMessage("gui.main-menu.title"));
        this.player = player;
        this.admin = admin;
        this.page = page;
        this.sortMode = sortMode;
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

        // Handle Search filtering if needed (will be implemented in MenuListener)
        
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

                lore.add(mm.deserialize("<gray>ѕᴇʟʟᴇʀ: <white>" + sellerName));
                lore.add(mm.deserialize("<gray>ᴘʀɪᴄᴇ: <yellow>" + AHFormatter.formatPrice(item.getPrice())));

                if (isHighValue)
                    lore.add(mm.deserialize("<gradient:#f6d365:#fda085><bold>⚡ ʜɪɢʜ ᴠᴀʟᴜᴇ</bold></gradient>"));

                lore.add(mm.deserialize("<gray>ᴇxᴘɪʀᴇѕ: <red>" + AHFormatter.formatTimeLeft(item.getListedAt())));
                
                // Amethyst/NeonTools Expiry Support
                org.bukkit.persistence.PersistentDataContainer pdc = meta.getPersistentDataContainer();
                NamespacedKey toolKey = new NamespacedKey("neontools", "amethyst_expiry");
                if (pdc.has(toolKey, org.bukkit.persistence.PersistentDataType.LONG)) {
                    long toolExpiry = pdc.getOrDefault(toolKey, org.bukkit.persistence.PersistentDataType.LONG, 0L);
                    long remaining = toolExpiry - System.currentTimeMillis();
                    if (remaining > 0) {
                        lore.add(mm.deserialize("<light_purple>ᴛᴏᴏʟ ʟɪꜰᴇ: <white>" + AHFormatter.formatDuration(remaining)));
                    } else {
                        lore.add(mm.deserialize("<red><bold>⚠ ᴛᴏᴏʟ ᴇxᴘɪʀᴇᴅ</bold>"));
                    }
                }

                lore.add(net.kyori.adventure.text.Component.empty());

                if (admin)
                    lore.add(ConfigManager.get().getMiniMessage("lore.force-remove"));
                else if (item.getSeller().equals(player.getUniqueId()))
                    lore.add(ConfigManager.get().getMiniMessage("lore.cancel"));
                else {
                    if (stack.getType().name().contains("SHULKER_BOX")) {
                        lore.add(ConfigManager.get().getMiniMessage("lore.preview"));
                    } else {
                        lore.add(ConfigManager.get().getMiniMessage("lore.purchase"));
                    }
                }

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

        // Navigation & Action Bar (45-53)
        fillRange(45, 54, Material.GRAY_STAINED_GLASS_PANE);

        // Sort button
        inventory.setItem(45, create(sortMode.icon,
                "<gradient:#cfd9df:#e2ebf0><bold>ѕᴏʀᴛ ᴍᴏᴅᴇ</bold></gradient>",
                "<gray>Current: <white>" + sortMode.label,
                "<gray>Click to cycle sorting"));

        // Refresh
        inventory.setItem(46, create(Material.SUNFLOWER,
                "<gradient:#fccb90:#d57eeb><bold>ʀᴇꜰʀᴇѕʜ</bold></gradient>",
                "<gray>Update listings in real-time"));

        // Search
        inventory.setItem(47, create(Material.COMPASS,
                "<gradient:#a1c4fd:#c2e9fb><bold>ѕᴇᴀʀᴄʜ</bold></gradient>",
                "<gray>Search by item or username"));

        // Navigation
        if (page > 0)
            inventory.setItem(48, create(Material.ARROW, "<gradient:#8fd3f4:#84fab0><bold>ᴘʀᴇᴠɪᴏᴜѕ ᴘᴀɢᴇ</bold></gradient>"));
        
        inventory.setItem(49, create(Material.RED_STAINED_GLASS_PANE, "<red><bold>ᴄʟᴏѕᴇ</bold>"));

        if (end < items.size())
            inventory.setItem(50, create(Material.ARROW, "<gradient:#8fd3f4:#84fab0><bold>ɴᴇxᴛ ᴘᴀɢᴇ</bold></gradient>"));

        // User Buttons
        inventory.setItem(52, create(Material.CHEST,
                "<gradient:#f6d365:#fda085><bold>ᴍʏ ʟɪѕᴛɪɴɢѕ</bold></gradient>",
                "<gray>Manage your active auctions"));

        inventory.setItem(53, create(Material.BOOK,
                "<gradient:#cfd9df:#e2ebf0><bold>ᴀᴜᴄᴛɪᴏɴ ʜɪѕᴛᴏʀʏ</bold></gradient>",
                "<gray>View your past sales and buys"));
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
