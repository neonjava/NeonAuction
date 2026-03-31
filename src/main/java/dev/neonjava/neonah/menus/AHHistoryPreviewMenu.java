package dev.neonjava.neonah.menus;

import dev.neonjava.neonah.utils.AHFormatter;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

/**
 * AHHistoryPreviewMenu - Read-only preview for history items.
 */
public class AHHistoryPreviewMenu extends AHBaseMenu {

    public AHHistoryPreviewMenu(ItemStack item) {
        super(54, net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(
                "<gradient:#9d50bb:#6e48aa><bold>" + AHFormatter.toSmallCaps("History Preview")));
        render(item);
    }

    private void render(ItemStack item) {
        inventory.clear();
        net.kyori.adventure.text.minimessage.MiniMessage mm = net.kyori.adventure.text.minimessage.MiniMessage
                .miniMessage();
        ItemStack bg = create(Material.BLACK_STAINED_GLASS_PANE, mm.deserialize(" "));
        for (int i = 0; i < 54; i++)
            inventory.setItem(i, bg);

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
                        inventory.setItem(18 + i, create(Material.LIGHT_GRAY_STAINED_GLASS_PANE, mm.deserialize(" ")));
                    }
                }
            }
        }

        // Back button
        inventory.setItem(49,
                create(Material.RED_STAINED_GLASS_PANE,
                        mm.deserialize("<red><bold>" + AHFormatter.toSmallCaps("Back to History"))));
    }

    @Override
    public void open(Player p) {
        p.openInventory(getInventory());
    }
}
