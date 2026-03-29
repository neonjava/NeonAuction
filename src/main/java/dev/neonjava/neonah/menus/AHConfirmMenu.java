package dev.neonjava.neonah.menus;

import dev.neonjava.neonah.models.AHItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.List;

public class AHConfirmMenu extends AHBaseMenu {
    private final AHItem item;
    private final ConfirmType type;

    public enum ConfirmType {
        BUY, REMOVE, ADMIN_REMOVE
    }

    public AHConfirmMenu(Player player, AHItem item, ConfirmType type) {
        super(27, type == ConfirmType.BUY ? "Confirm Purchase" : "Confirm Removal");
        this.item = item;
        this.type = type;
    }

    @Override
    public void open(Player p) {
        render();
        p.openInventory(getInventory());
    }

    public void render() {
        for (int i = 0; i < 27; i++)
            inventory.setItem(i, create(Material.GRAY_STAINED_GLASS_PANE, " "));

        Material confirmMat = (type == ConfirmType.BUY) ? Material.LIME_STAINED_GLASS_PANE
                : Material.ORANGE_STAINED_GLASS_PANE;
        String confirmName = (type == ConfirmType.BUY) ? "§a§lConfirm Purchase" : "§6§lRemove from Auction";
        inventory.setItem(11, create(confirmMat, confirmName, "§7Click to proceed."));

        inventory.setItem(13, item.getItem());

        String cancelName = (type == ConfirmType.BUY) ? "§c§lCancel" : "§7§lKeep Listing";
        inventory.setItem(15, create(Material.RED_STAINED_GLASS_PANE, cancelName, "§7Go back."));
    }

    public AHItem getItem() {
        return item;
    }

    public ConfirmType getType() {
        return type;
    }
}
