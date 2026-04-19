package dev.neonjava.neonah.utils;

import dev.neonjava.neonah.models.AHItem;
import dev.neonjava.neonah.menus.AHConfirmMenu;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import java.util.ArrayList;
import java.util.List;

/**
 * AHDialogImpl - Version-safe implementation of Dialog API.
 */
@SuppressWarnings("UnstableApiUsage")
public class AHDialogImpl {
    private static final MiniMessage mm = MiniMessage.miniMessage();

    public static void showBuy(Player p, AHItem item) {
        final Dialog dialog = Dialog.create(builder -> {
            builder.empty()
                    .base(DialogBase.builder(mm.deserialize("<white>Confirm Purchase"))
                            .body(buildBody(item))
                            .build())
                    .type(DialogType.confirmation(
                            ActionButton.builder(mm.deserialize("<green>✔ Confirm"))
                                    .tooltip(mm.deserialize("<green>Click to buy!"))
                                    .action(DialogAction.customClick(AHDialogUtils.KEY_CONFIRM_BUY, null))
                                    .build(),
                            ActionButton.builder(mm.deserialize("<red>✖ Cancel"))
                                    .tooltip(mm.deserialize("<red>Cancel and go back"))
                                    .action(DialogAction.customClick(AHDialogUtils.KEY_CANCEL_BUY, null))
                                    .build()));
        });
        show(p, dialog);
    }

    public static void showSelfRemove(Player p, AHItem item) {
        final Dialog dialog = Dialog.create(builder -> {
            builder.empty()
                    .base(DialogBase.builder(mm.deserialize("<white>Remove Listing?"))
                            .body(buildBody(item))
                            .build())
                    .type(DialogType.confirmation(
                            ActionButton.builder(mm.deserialize("<red>✔ Remove"))
                                    .action(DialogAction.customClick(AHDialogUtils.KEY_CONFIRM_REMOVE, null))
                                    .build(),
                            ActionButton.builder(mm.deserialize("<gray>✖ Keep Listing"))
                                    .action(DialogAction.customClick(AHDialogUtils.KEY_CANCEL_REMOVE, null))
                                    .build()));
        });
        show(p, dialog);
    }

    public static void showAdminRemove(Player p, AHItem item) {
        final Dialog dialog = Dialog.create(builder -> {
            builder.empty()
                    .base(DialogBase.builder(mm.deserialize("<red>Admin: Force Remove?"))
                            .body(buildBody(item))
                            .build())
                    .type(DialogType.confirmation(
                            ActionButton.builder(mm.deserialize("<red>✔ Force Remove"))
                                    .action(DialogAction.customClick(AHDialogUtils.KEY_CONFIRM_ADMIN, null))
                                    .build(),
                            ActionButton.builder(mm.deserialize("<gray>✖ Cancel"))
                                    .action(DialogAction.customClick(AHDialogUtils.KEY_CANCEL_ADMIN, null))
                                    .build()));
        });
        show(p, dialog);
    }

    private static List<DialogBody> buildBody(AHItem item) {
        List<DialogBody> bodies = new ArrayList<>();
        bodies.add(DialogBody.plainMessage(mm.deserialize("<white>Action for:")));
        bodies.add(DialogBody.item(item.getItem()).build());
        bodies.add(DialogBody
                .plainMessage(mm.deserialize("<white>Price: <yellow>" + AHFormatter.formatPrice(item.getPrice()))));

        if (AHConfirmMenu.isShulker(item.getItem())) {
            if (item.getItem().getItemMeta() instanceof BlockStateMeta bsm) {
                if (bsm.getBlockState() instanceof ShulkerBox box) {
                    bodies.add(DialogBody.plainMessage(mm.deserialize("<gray><italic>Previewing contents below:")));
                    for (ItemStack sub : box.getInventory().getContents()) {
                        if (sub != null && sub.getType() != Material.AIR) {
                            bodies.add(DialogBody.item(sub).build());
                        }
                    }
                }
            }
        }
        return bodies;
    }

    private static void show(Player p, Dialog d) {
        Bukkit.getScheduler().runTask(dev.neonjava.neonah.NeonAH.get(), () -> {
            try {
                Player.class.getMethod("showDialog", Dialog.class).invoke(p, d);
            } catch (ReflectiveOperationException ignored) {
                // Fallback handled by AHDialogUtils, which opens inventory confirmations when
                // dialog API is unavailable.
            }
        });
    }
}
