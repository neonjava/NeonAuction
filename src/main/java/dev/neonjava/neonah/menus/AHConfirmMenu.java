package dev.neonjava.neonah.menus;

import dev.neonjava.neonah.managers.ConfigManager;
import dev.neonjava.neonah.models.AHItem;
import dev.neonjava.neonah.utils.AHFormatter;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.ArrayList;
import java.util.List;

/**
 * AHConfirmMenu - Enhanced with Shulker Box Previews.
 */
public class AHConfirmMenu extends AHBaseMenu {
    @Getter
    private final String type;
    @Getter
    private final AHItem item;
    private final boolean isShulker;

    public AHConfirmMenu(String type, AHItem item) {
        super(isShulker(item.getItem()) ? 54 : 27, 
            type.equals("buy") ? ConfigManager.get().getMiniMessage("gui.confirm-buy.title") : 
            net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<red><bold>ᴄᴏɴꜰɪʀᴍ ʀᴇᴍᴏᴠᴀʟ"));
        this.type = type;
        this.item = item;
        this.isShulker = isShulker(item.getItem());
        render();
    }

    private void render() {
        inventory.clear();
        fillRange(0, inventory.getSize(), Material.BLACK_STAINED_GLASS_PANE);

        // Layout adapts based on size
        int confirmSlot = 11;
        int itemSlot = 13;
        int cancelSlot = 15;

        // Action Buttons
        inventory.setItem(confirmSlot, create(Material.LIME_STAINED_GLASS_PANE,
                type.equals("buy") ? "<green><bold>✔ ᴄᴏɴꜰɪʀᴍ ᴘᴜʀᴄʜᴀѕᴇ" : "<red><bold>✔ ᴄᴏɴꜰɪʀᴍ ʀᴇᴍᴏᴠᴀʟ",
                "<gray>Price: <yellow>" + AHFormatter.formatPrice(item.getPrice()),
                "<gray>Click to process this action."));

        ItemStack displayItem = item.getItem().clone();
        ItemMeta meta = displayItem.getItemMeta();
        if (meta != null) {
            List<net.kyori.adventure.text.Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            org.bukkit.persistence.PersistentDataContainer pdc = meta.getPersistentDataContainer();
            NamespacedKey toolKey = new NamespacedKey("neontools", "amethyst_expiry");
            if (pdc.has(toolKey, org.bukkit.persistence.PersistentDataType.LONG)) {
                long toolExpiry = pdc.getOrDefault(toolKey, org.bukkit.persistence.PersistentDataType.LONG, 0L);
                long remaining = toolExpiry - System.currentTimeMillis();
                lore.add(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<light_purple>ᴛᴏᴏʟ ʟɪꜰᴇ: <white>" + AHFormatter.formatDuration(remaining)));
            }
            meta.lore(lore);
            displayItem.setItemMeta(meta);
        }
        inventory.setItem(itemSlot, displayItem);

        inventory.setItem(cancelSlot,
                create(Material.RED_STAINED_GLASS_PANE, "<red><bold>✖ ᴄᴀɴᴄᴇʟ",
                        "<gray>Click to return to auction."));

        // Shulker Contents (Rows 3, 4, 5)
        if (isShulker && item.getItem().getItemMeta() instanceof BlockStateMeta bsm) {
            if (bsm.getBlockState() instanceof ShulkerBox box) {
                ItemStack[] contents = box.getInventory().getContents();
                // Start at slot 18 (Row 3)
                for (int i = 0; i < 27; i++) {
                    if (contents[i] != null && contents[i].getType() != Material.AIR) {
                        inventory.setItem(18 + i, contents[i]);
                    } else {
                        inventory.setItem(18 + i, create(Material.GRAY_STAINED_GLASS_PANE, " "));
                    }
                }

                // Add Lore hint for preview
                inventory.setItem(4, create(Material.PURPLE_STAINED_GLASS_PANE, 
                        "<light_purple><bold>ѕʜᴜʟᴋᴇʀ ᴘʀᴇᴠɪᴇᴡ",
                        "<gray>Viewing contents of this box."));
            }
        }
    }

    public static boolean isShulker(ItemStack item) {
        if (item == null) return false;
        return item.getType().name().contains("SHULKER_BOX");
    }

    @Override
    public void open(Player player) {
        player.openInventory(getInventory());
    }
}
