package dev.neonjava.neonah.menus;

import dev.neonjava.neonah.models.AHItem;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.util.stream.IntStream;

/**
 * AHConfirmMenu - Enhanced with Shulker Box Previews.
 */
@Getter
public class AHConfirmMenu extends AHBaseMenu {
    private final String type;
    private final AHItem item;
    private final boolean isShulker;

    public AHConfirmMenu(String type, AHItem item) {
        // Use 54 slots (6 rows) if it's a shulker preview, 27 slots (3 rows) otherwise.
        super(isShulker(item.getItem()) ? 54 : 27,
                net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(
                        (type.equals("buy") ? "<gradient:#84fab0:#8fd3f4>Confirm Purchase"
                                : "<gradient:#ff0844:#ffb199>Confirm Removal")
                                + (isShulker(item.getItem()) ? " (Preview)" : "")));
        this.type = type;
        this.item = item;
        this.isShulker = isShulker(item.getItem());
        render();
    }

    private void render() {
        inventory.clear();
        net.kyori.adventure.text.minimessage.MiniMessage mm = net.kyori.adventure.text.minimessage.MiniMessage
                .miniMessage();

        // Layout adapts based on size
        int confirmSlot = 11;
        int itemSlot = 13;
        int cancelSlot = 15;

        // Action Buttons
        inventory.setItem(confirmSlot, create(Material.LIME_STAINED_GLASS_PANE,
                mm.deserialize(type.equals("buy") ? "<green><bold>✔ Confirm Purchase" : "<red><bold>✔ Confirm Removal"),
                mm.deserialize("<gray>Click to process.")));

        inventory.setItem(itemSlot, item.getItem());

        inventory.setItem(cancelSlot, create(Material.RED_STAINED_GLASS_PANE, mm.deserialize("<red><bold>✖ Cancel"),
                mm.deserialize("<gray>Click to go back.")));

        // Shulker Contents (Rows 3, 4, 5)
        if (isShulker && item.getItem().getItemMeta() instanceof BlockStateMeta bsm) {
            if (bsm.getBlockState() instanceof ShulkerBox box) {
                ItemStack[] contents = box.getInventory().getContents();
                // Start at slot 18 (Row 3)
                for (int i = 0; i < 27; i++) {
                    if (contents[i] != null && contents[i].getType() != Material.AIR) {
                        inventory.setItem(18 + i, contents[i]);
                    } else {
                        inventory.setItem(18 + i, create(Material.LIGHT_GRAY_STAINED_GLASS_PANE, mm.deserialize(" ")));
                    }
                }

                // Add Lore hint for preview
                inventory.setItem(4,
                        create(Material.BOOK, mm.deserialize("<light_purple><bold>Shulker Box Contents"),
                                mm.deserialize("<gray>Below is a live preview of the box.")));
            }
        }
    }

    public static boolean isShulker(ItemStack item) {
        if (item == null)
            return false;
        String name = item.getType().name();
        return name.contains("SHULKER_BOX");
    }

    @Override
    public void open(Player player) {
        player.openInventory(getInventory());
    }
}
