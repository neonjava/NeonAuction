package dev.neonjava.neonah.utils;

import dev.neonjava.neonah.NeonAH;
import dev.neonjava.neonah.models.AHItem;
import dev.neonjava.neonah.managers.AHManager;
import dev.neonjava.neonah.menus.AHConfirmMenu;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import java.util.ArrayList;
import java.util.List;

/**
 * AHDialogUtils - Paper Dialog API utility.
 */
@SuppressWarnings("UnstableApiUsage")
public class AHDialogUtils {
        private static final MiniMessage mm = MiniMessage.miniMessage();

        public static final Key KEY_CONFIRM_BUY = Key.key("neonah:buy/confirm");
        public static final Key KEY_CANCEL_BUY = Key.key("neonah:buy/cancel");
        public static final Key KEY_CONFIRM_REMOVE = Key.key("neonah:remove/confirm");
        public static final Key KEY_CANCEL_REMOVE = Key.key("neonah:remove/cancel");
        public static final Key KEY_CONFIRM_ADMIN = Key.key("neonah:admin/confirm");
        public static final Key KEY_CANCEL_ADMIN = Key.key("neonah:admin/cancel");

        public static void showBuyDialog(Player p, AHItem item) {
                AHManager.get().setPending(p.getUniqueId(), "buy", item);
                final Dialog dialog = Dialog.create(builder -> {
                        builder.empty()
                                        .base(DialogBase.builder(mm.deserialize("<white>Confirm Purchase"))
                                                        .body(buildBody(item))
                                                        .build())
                                        .type(DialogType.confirmation(
                                                        ActionButton.builder(mm.deserialize("<green>✔ Confirm"))
                                                                        .tooltip(mm.deserialize("<green>Click to buy!"))
                                                                        .action(DialogAction.customClick(
                                                                                        KEY_CONFIRM_BUY, null))
                                                                        .build(),
                                                        ActionButton.builder(mm.deserialize("<red>✖ Cancel"))
                                                                        .tooltip(mm.deserialize(
                                                                                        "<red>Cancel and go back"))
                                                                        .action(DialogAction.customClick(KEY_CANCEL_BUY,
                                                                                        null))
                                                                        .build()));
                });
                open(p, dialog);
        }

        public static void showSelfRemoveDialog(Player p, AHItem item) {
                AHManager.get().setPending(p.getUniqueId(), "remove", item);
                final Dialog dialog = Dialog.create(builder -> {
                        builder.empty()
                                        .base(DialogBase.builder(mm.deserialize("<white>Remove Listing?"))
                                                        .body(buildBody(item))
                                                        .build())
                                        .type(DialogType.confirmation(
                                                        ActionButton.builder(mm.deserialize("<red>✔ Remove"))
                                                                        .action(DialogAction.customClick(
                                                                                        KEY_CONFIRM_REMOVE, null))
                                                                        .build(),
                                                        ActionButton.builder(mm.deserialize("<gray>✖ Keep Listing"))
                                                                        .action(DialogAction.customClick(
                                                                                        KEY_CANCEL_REMOVE, null))
                                                                        .build()));
                });
                open(p, dialog);
        }

        public static void showAdminRemoveDialog(Player p, AHItem item) {
                AHManager.get().setPending(p.getUniqueId(), "admin", item);
                final Dialog dialog = Dialog.create(builder -> {
                        builder.empty()
                                        .base(DialogBase.builder(mm.deserialize("<red>Admin: Force Remove?"))
                                                        .body(buildBody(item))
                                                        .build())
                                        .type(DialogType.confirmation(
                                                        ActionButton.builder(mm.deserialize("<red>✔ Force Remove"))
                                                                        .action(DialogAction.customClick(
                                                                                        KEY_CONFIRM_ADMIN, null))
                                                                        .build(),
                                                        ActionButton.builder(mm.deserialize("<gray>✖ Cancel"))
                                                                        .action(DialogAction.customClick(
                                                                                        KEY_CANCEL_ADMIN, null))
                                                                        .build()));
                });
                open(p, dialog);
        }

        private static List<DialogBody> buildBody(AHItem item) {
                List<DialogBody> bodies = new ArrayList<>();
                bodies.add(DialogBody.plainMessage(mm.deserialize("<white>Action for:")));
                bodies.add(DialogBody.item(item.getItem()).build());

                // Add Price if it's a buy dialog (we can infer from context usually, but here
                // generic)
                bodies.add(DialogBody.plainMessage(
                                mm.deserialize("<white>Price: <yellow>" + AHFormatter.formatPrice(item.getPrice()))));

                // Shulker Preview in Dialog Body!
                if (AHConfirmMenu.isShulker(item.getItem())) {
                        if (item.getItem().getItemMeta() instanceof BlockStateMeta bsm) {
                                if (bsm.getBlockState() instanceof ShulkerBox box) {
                                        bodies.add(DialogBody.plainMessage(
                                                        mm.deserialize("<gray><italic>Previewing contents below:")));
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

        public static void processPurchase(Player p, AHItem item) {
                if (!AHManager.get().getItems().contains(item))
                        return;
                if (p.getInventory().firstEmpty() == -1) {
                        p.sendMessage("§c§l[AH] §cInventory full!");
                        return;
                }
                double balance = NeonAH.getEcon().getBalance(p);
                if (balance < item.getPrice()) {
                        p.sendMessage("§c§l[AH] §cInsufficient funds!");
                        return;
                }

                NeonAH.getEcon().withdrawPlayer(p, item.getPrice());
                NeonAH.getEcon().depositPlayer(Bukkit.getOfflinePlayer(item.getSeller()), item.getPrice());
                AHManager.get().recordSale(item.getSeller(), p.getUniqueId(), item.getPrice(), item.getItem());
                AHManager.get().removeItem(item);
                p.getInventory().addItem(item.getItem());
                p.sendMessage("§a§l[AH] §aSuccessfully purchased!");
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        }

        public static void processRemoval(Player p, AHItem item) {
                if (!AHManager.get().getItems().contains(item))
                        return;
                if (p.getInventory().firstEmpty() == -1) {
                        p.sendMessage("§cInventory full!");
                        return;
                }
                AHManager.get().removeItem(item);
                p.getInventory().addItem(item.getItem());
                p.sendMessage("§a§l[AH] §aItem returned!");
        }

        private static void open(Player p, Dialog d) {
                p.getScheduler().run(dev.neonjava.neonah.NeonAH.get(), t -> p.showDialog(d), null);
        }
}
