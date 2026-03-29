package dev.neonjava.neonah.utils;

import dev.neonjava.neonah.NeonAH;
import dev.neonjava.neonah.models.AHItem;
import dev.neonjava.neonah.managers.AHManager;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import java.util.List;

/**
 * AHDialogUtils - 100% KEY-BASED PATTERN.
 * 
 * After extensive testing, we've found that BiConsumer Lambdas in Paper Dialogs
 * are unreliable for Bedrock players via Geyser on this server.
 * 
 * We have switched to a 100% Key-based system where every action is handled
 * by the AHDialogListener. This is the ultimate fix for Bedrock compatibility.
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
                                                        .body(List.of(
                                                                        DialogBody.plainMessage(mm.deserialize(
                                                                                        "<white>You will pay <green>"
                                                                                                        + AHFormatter.formatPrice(
                                                                                                                        item.getPrice())
                                                                                                        + " <white>for:")),
                                                                        DialogBody.item(item.getItem()).build()))
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
                                                        .body(List.of(DialogBody.item(item.getItem()).build()))
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
                                                        .body(List.of(DialogBody.item(item.getItem()).build()))
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
                AHManager.get().recordSale(item.getSeller(), p.getUniqueId(), item.getPrice(),
                                item.getItem().getType().toString());
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
