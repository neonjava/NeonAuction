package dev.neonjava.neonah.menus;

import dev.neonjava.neonah.models.AHItem;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.stream.IntStream;

/**
 * AHConfirmMenu - A Bedrock-friendly Chest GUI for confirming
 * purchases/removals.
 */
@Getter
public class AHConfirmMenu extends AHBaseMenu {
    private final String type;
    private final AHItem item;

    public AHConfirmMenu(String type, AHItem item) {
        super(27, type.equals("buy") ? "§0Confirm Purchase" : "§0Confirm Removal");
        this.type = type;
        this.item = item;
        render();
    }

    private void render() {
        // Fill empty background
        ItemStack bg = create(Material.BLACK_STAINED_GLASS_PANE, " ");
        IntStream.range(0, 27).forEach(i -> inventory.setItem(i, bg));

        // Slot 11: Confirm (Lime)
        inventory.setItem(11, create(Material.LIME_STAINED_GLASS_PANE,
                type.equals("buy") ? "§a§lConfirm Purchase" : "§c§lConfirm Removal",
                "§7Click to process."));

        // Slot 13: Auction Item
        inventory.setItem(13, item.getItem());

        // Slot 15: Cancel (Red)
        inventory.setItem(15, create(Material.RED_STAINED_GLASS_PANE, "§c§lCancel",
                "§7Click to go back."));
    }

    @Override
    public void open(Player player) {
        player.openInventory(getInventory());
    }
}
