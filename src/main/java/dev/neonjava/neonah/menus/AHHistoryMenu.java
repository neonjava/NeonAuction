package dev.neonjava.neonah.menus;

import dev.neonjava.neonah.managers.AHManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import java.util.List;

public class AHHistoryMenu extends AHBaseMenu {
    private final Player player;

    public AHHistoryMenu(Player player) {
        super(54, "Auction History");
        this.player = player;
    }

    @Override
    public void open(Player p) {
        render();
        p.openInventory(getInventory());
    }

    public void render() {
        inventory.clear();
        List<String> list = AHManager.get().getHistoryStrings(player.getUniqueId());

        for (int i = 0; i < Math.min(list.size(), 45); i++) {
            inventory.setItem(i, create(Material.PAPER, "§eListing #" + (i + 1), list.get(i)));
        }

        // Bottom Bar
        for (int i = 45; i < 54; i++)
            inventory.setItem(i, create(Material.BLACK_STAINED_GLASS_PANE, " "));
        inventory.setItem(49, create(Material.RED_STAINED_GLASS_PANE, "§c§lBack to AH"));
    }
}
