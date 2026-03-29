package dev.neonjava.neonah.menus;

import dev.neonjava.neonah.utils.AHFormatter;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import java.util.stream.IntStream;

/**
 * AHHistoryPreviewMenu - Read-only preview for history items.
 */
public class AHHistoryPreviewMenu extends AHBaseMenu {

    public AHHistoryPreviewMenu(ItemStack item) {
        super(54, "§5§l" + AHFormatter.toSmallCaps("History Preview"));
        render(item);
    }

    private void render(ItemStack item) {
        inventory.clear();
        ItemStack bg = create(Material.BLACK_STAINED_GLASS_PANE, " ");
        IntStream.range(0, 54).forEach(i -> inventory.setItem(i, bg));

        // Center the item being previewed
        inventory.setItem(4, item);

        // If shulker, show contents
        if (AHConfirmMenu.isShulker(item) && item.getItemMeta() instanceof BlockStateMeta bsm) {
            if (bsm.getBlockState() instanceof ShulkerBox box) {
                ItemStack[] contents = box.getInventory().getContents();
                for (int i = 0; i < 27; i++) {
                    if (contents[i] != null && contents[i].getType() != Material.AIR) {
                        inventory.setItem(18 + i, contents[i]);
                    } else {
                        inventory.setItem(18 + i, create(Material.LIGHT_GRAY_STAINED_GLASS_PANE, " "));
                    }
                }
            }
        }

        // Back button
        inventory.setItem(49,
                create(Material.RED_STAINED_GLASS_PANE, "§c§l" + AHFormatter.toSmallCaps("Back to History")));
    }

    @Override
    public void open(Player p) {
        p.openInventory(getInventory());
    }
}
