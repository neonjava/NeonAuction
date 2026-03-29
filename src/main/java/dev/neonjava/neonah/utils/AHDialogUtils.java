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
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import java.util.List;

/**
 * AHDialogUtils - Re-implemented using the EXACT NeonOrder pattern from
 * ManageOrderDialog.java.
 *
 * This pattern uses:
 * 1. Lambda for the Confirm button (customClick BiConsumer)
 * 2. NO ACTION (empty action) for the Cancel button
 * 3. NO KEY listener for confirm (letting Paper handle it)
 *
 * NeonOrder's ManageOrderDialog.java uses this and works on Bedrock.
 */
@SuppressWarnings("UnstableApiUsage")
public class AHDialogUtils {
        private static final MiniMessage mm = MiniMessage.miniMessage();

        public static void showBuyDialog(Player p, AHItem item) {
                // We use a final reference for the player to use inside the lambda
                final Player playerToPay = p;

                Dialog dialog = Dialog.create(builder -> builder.empty()
                                .base(DialogBase.builder(mm.deserialize("<white>Confirm Purchase"))
                                                .body(List.of(
                                                                DialogBody.plainMessage(mm.deserialize(
                                                                                "<white>You are purchasing:")),
                                                                DialogBody.item(item.getItem()).build(),
                                                                DialogBody.plainMessage(mm.deserialize(
                                                                                "<white>You will pay <green>"
                                                                                                + AHFormatter.formatPrice(
                                                                                                                item.getPrice())
                                                                                                + " <white>in return"))))
                                                .build())
                                .type(DialogType.confirmation(
                                                // Button 1: Confirm (uses lambda, just like NeonOrder's Deliver/Manage
                                                // dialogs)
                                                ActionButton.builder(mm.deserialize("<green>✔ Confirm"))
                                                                .action(DialogAction.customClick((view, audience) -> {
                                                                        // Using the final variable from the outer
                                                                        // scope, just like NeonOrder
                                                                        processPurchase(playerToPay, item);
                                                                }, ClickCallback.Options.builder().build()))
                                                                .build(),
                                                // Button 2: Cancel (no action, just closes, just like NeonOrder)
                                                ActionButton.builder(mm.deserialize("<red>✖ Cancel"))
                                                                .build())));

                open(p, dialog);
        }

        public static void showSelfRemoveDialog(Player p, AHItem item) {
                final Player seller = p;
                Dialog dialog = Dialog.create(builder -> builder.empty()
                                .base(DialogBase.builder(mm.deserialize("<white>Cancel Your Listing?"))
                                                .body(List.of(
                                                                DialogBody.plainMessage(mm.deserialize(
                                                                                "<white>You are removing this listing:")),
                                                                DialogBody.item(item.getItem()).build(),
                                                                DialogBody.plainMessage(mm.deserialize(
                                                                                "<white>The item will be returned to your inventory."))))
                                                .build())
                                .type(DialogType.confirmation(
                                                ActionButton.builder(mm.deserialize("<red>✔ Remove Listing"))
                                                                .action(DialogAction.customClick((view, audience) -> {
                                                                        processRemoval(seller, item);
                                                                }, ClickCallback.Options.builder().build()))
                                                                .build(),
                                                ActionButton.builder(mm.deserialize("<gray>✖ Keep Listing"))
                                                                .build())));
                open(p, dialog);
        }

        public static void showAdminRemoveDialog(Player p, AHItem item) {
                final Player admin = p;
                Dialog dialog = Dialog.create(builder -> builder.empty()
                                .base(DialogBase.builder(mm.deserialize("<red>Admin: Force Remove Listing?"))
                                                .body(List.of(
                                                                DialogBody.plainMessage(mm.deserialize(
                                                                                "<white>You are force-unlisting this item:")),
                                                                DialogBody.item(item.getItem()).build()))
                                                .build())
                                .type(DialogType.confirmation(
                                                ActionButton.builder(mm.deserialize("<red>✔ Confirm Removal"))
                                                                .action(DialogAction.customClick((view, audience) -> {
                                                                        AHManager.get().removeItem(item);
                                                                        AHManager.get().markUnclaimed(item.getSeller(),
                                                                                        item.getItem());
                                                                        admin.sendMessage(
                                                                                        "§a§l[AH] §aListing removed successfully.");
                                                                }, ClickCallback.Options.builder().build()))
                                                                .build(),
                                                ActionButton.builder(mm.deserialize("<gray>✖ Cancel"))
                                                                .build())));
                open(p, dialog);
        }

        private static void open(Player p, Dialog d) {
                // NeonOrder uses run() WITHOUT a delay for its Confirmation dialogs
                p.getScheduler().run(NeonAH.get(), t -> p.showDialog(d), null);
        }

        public static void processPurchase(Player p, AHItem item) {
                if (!AHManager.get().getItems().contains(item)) {
                        p.sendMessage("§c§l[AH] §cItem no longer available!");
                        return;
                }
                if (p.getInventory().firstEmpty() == -1) {
                        p.sendMessage("§c§l[AH] §cYour inventory is full! Cannot buy.");
                        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
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
                p.sendMessage("§a§l[AH] §aSuccessfully purchased for §e" + AHFormatter.formatPrice(item.getPrice()));
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        }

        public static void processRemoval(Player p, AHItem item) {
                if (!AHManager.get().getItems().contains(item))
                        return;
                if (p.getInventory().firstEmpty() == -1) {
                        p.sendMessage("§c§l[AH] §cInventory full! Cannot reclaim item.");
                        return;
                }
                AHManager.get().removeItem(item);
                p.getInventory().addItem(item.getItem());
                p.sendMessage("§a§l[AH] §aListing removed and item returned!");
        }
}
