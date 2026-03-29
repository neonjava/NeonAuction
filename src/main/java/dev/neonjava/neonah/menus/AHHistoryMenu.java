package dev.neonjava.neonah.menus;

import dev.neonjava.neonah.managers.AHManager;
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

    public AHHistoryMenu(Player player) {
        super(54, "§5§l" + AHFormatter.toSmallCaps("Auction History"));
        this.player = player;
    }

    @Override
    public void open(Player p) {
        render();
        p.openInventory(getInventory());
    }

    public void render() {
        inventory.clear();
        List<AHHistoryItem> history = AHManager.get().getHistory(player.getUniqueId());

        int start = page * 45;
        int end = Math.min(start + 45, history.size());

        for (int i = start; i < end; i++) {
            AHHistoryItem hi = history.get(i);
            boolean sold = hi.getSeller().equals(player.getUniqueId());

            Material iconMat = hi.getItem().getType();

            String status = sold ? "§c§l[SOLD]" : "§a§l[BOUGHT]";
            String userTag = sold ? "§7Buyer: §f" : "§7Seller: §f";
            String userName = Bukkit.getOfflinePlayer(sold ? hi.getBuyer() : hi.getSeller()).getName();

            String previewHint = AHConfirmMenu.isShulker(hi.getItem()) ? "§e§l▶ Click to View Contents" : "";

            inventory.setItem(i - start, create(iconMat, status + " §f" + hi.getItem().getType(),
                    "§8§m                    ",
                    userTag + userName,
                    "§7Price: §e" + AHFormatter.formatPrice(hi.getPrice()),
                    "§7Date: §8" + new Date(hi.getTimestamp()).toString(),
                    "",
                    previewHint));
        }

        // Bottom Bar
        for (int i = 45; i < 54; i++)
            inventory.setItem(i, create(Material.BLACK_STAINED_GLASS_PANE, " "));

        if (page > 0)
            inventory.setItem(48, create(Material.ARROW, "§e" + AHFormatter.toSmallCaps("Previous Page")));

        inventory.setItem(49, create(Material.RED_STAINED_GLASS_PANE, "§c§l" + AHFormatter.toSmallCaps("Back to AH")));

        if (end < history.size())
            inventory.setItem(50, create(Material.ARROW, "§e" + AHFormatter.toSmallCaps("Next Page")));
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
