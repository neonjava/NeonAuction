package dev.neonjava.neonah.menus;

import dev.neonjava.neonah.managers.AHManager;
import dev.neonjava.neonah.managers.ConfigManager;
import dev.neonjava.neonah.models.AHHistoryItem;
import dev.neonjava.neonah.utils.AHFormatter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import java.util.Date;
import java.util.List;

/**
 * AHHistoryMenu - Enhanced Auction History.
 * Shows actual item icons and detailed transaction logs.
 */
public class AHHistoryMenu extends AHBaseMenu {
    private final Player player;
    private int page = 0;
    private String search = null;

    public AHHistoryMenu(Player player) {
        this(player, null);
    }

    public AHHistoryMenu(Player player, String search) {
        super(54, ConfigManager.parse(
                "<gradient:#9d50bb:#6e48aa><bold>" + AHFormatter.toSmallCaps("Auction History")));
        this.player = player;
        this.search = search;
    }

    @Override
    public void open(Player p) {
        render();
        p.openInventory(getInventory());
    }

    public void render() {
        inventory.clear();
        List<AHHistoryItem> history = AHManager.get().getHistory(player.getUniqueId());

        if (search != null && !search.isEmpty()) {
            String q = search.toLowerCase();
            history = history.stream()
                .filter(hi -> hi.getItem().getType().name().toLowerCase().contains(q) || 
                              Bukkit.getOfflinePlayer(hi.getSeller()).getName() != null && Bukkit.getOfflinePlayer(hi.getSeller()).getName().toLowerCase().contains(q) ||
                              Bukkit.getOfflinePlayer(hi.getBuyer()).getName() != null && Bukkit.getOfflinePlayer(hi.getBuyer()).getName().toLowerCase().contains(q))
                .toList();
        }

        int start = page * 45;
        int end = Math.min(start + 45, history.size());

        for (int i = start; i < end; i++) {
            AHHistoryItem hi = history.get(i);
            boolean sold = hi.getSeller().equals(player.getUniqueId());

            Material iconMat = hi.getItem().getType();

            net.kyori.adventure.text.Component status = ConfigManager
                    .parse(sold ? "<red><bold>[SOLD]" : "<green><bold>[BOUGHT]");
            String userTag = sold ? "<gray>Buyer: <white>" : "<gray>Seller: <white>";
            String userName = Bukkit.getOfflinePlayer(sold ? hi.getBuyer() : hi.getSeller()).getName();

            net.kyori.adventure.text.Component previewHint = AHConfirmMenu.isShulker(hi.getItem())
                    ? ConfigManager.parse("<yellow><bold>▶ Click to View Contents")
                    : net.kyori.adventure.text.Component.empty();

            inventory.setItem(i - start, create(iconMat,
                    status.append(ConfigManager.parse(" <white>" + hi.getItem().getType())),
                    ConfigManager.parse("<dark_gray><strikethrough>                    "),
                    ConfigManager.parse(userTag + userName),
                    ConfigManager.parse("<gray>Price: <yellow>" + AHFormatter.formatPrice(hi.getPrice())),
                    ConfigManager.parse("<gray>Date: <dark_gray>"
                            + new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date(hi.getTimestamp()))),
                    net.kyori.adventure.text.Component.empty(),
                    previewHint));
        }

        // Bottom Bar
        for (int i = 45; i < 54; i++)
            inventory.setItem(i, create(Material.GRAY_STAINED_GLASS_PANE, " "));

        // Search
        inventory.setItem(47, create(Material.COMPASS,
                "<gradient:#a1c4fd:#c2e9fb><bold>ѕᴇᴀʀᴄʜ</bold></gradient>",
                search == null ? "<gray>Click to search history" : ConfigManager.get().getMessage("searching-for").replace("%search%", search),
                "<gray>Filter by item name or player"));

        if (page > 0)
            inventory.setItem(48, create(Material.ARROW,
                    ConfigManager.parse("<yellow>" + AHFormatter.toSmallCaps("Previous Page"))));

        inventory.setItem(49, create(Material.RED_STAINED_GLASS_PANE,
                ConfigManager.parse("<red><bold>" + AHFormatter.toSmallCaps("Back to AH"))));

        if (end < history.size())
            inventory.setItem(50, create(Material.ARROW,
                    ConfigManager.parse("<yellow>" + AHFormatter.toSmallCaps("Next Page"))));
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
