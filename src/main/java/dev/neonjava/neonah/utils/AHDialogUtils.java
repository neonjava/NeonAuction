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
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import java.util.List;

/**
 * AHDialogUtils - Re-implemented using the EXACT Mixed Pattern found in
 * NeonOrder's DeliveryConfirmDialog.java.
 * 
 * THE PATTERN (confirmed working for Bedrock in NeonOrder):
 * 1. CONFIRM button = Lambda action (DialogAction.customClick((v, player) -> {
 * ... }, ClickCallback.Options.builder().build()))
 * 2. CANCEL button = Key action
 * (DialogAction.customClick(Key.key("neonah:.../cancel"), null))
 * 3. Both buttons have Tooltips (Geyser may use these as metadata).
 * 4. "Confirm" is the FIRST argument in confirmation(), "Cancel" is the SECOND.
 * 
 * I will use a AHDialogListener to handle the Cancel key.
 */
@SuppressWarnings("UnstableApiUsage")
public class AHDialogUtils {
        private static final MiniMessage mm = MiniMessage.miniMessage();

        public static final Key CANCEL_BUY = Key.key("neonah:buy/cancel");
        public static final Key CANCEL_REMOVE = Key.key("neonah:remove/cancel");
        public static final Key CANCEL_ADMIN = Key.key("neonah:admin/cancel");

        public static void showBuyDialog(Player p, AHItem item) {
                final Player buyer = p;
                final Dialog dialog = Dialog.create(builder -> {
                        builder.empty()
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
                                                        // Button 1: CONFIRM (Pattern matched: Lambda + Tooltip)
                                                        ActionButton.builder(mm.deserialize("<green>✔ Confirm"))
                                                                        .tooltip(mm.deserialize(
                                                                                        "<green>Click to confirm purchase"))
                                                                        .action(DialogAction.customClick(
                                                                                        (view, audience) -> {
                                                                                                processPurchase(buyer,
                                                                                                                item);
                                                                                        },
                                                                                        ClickCallback.Options.builder()
                                                                                                        .build()))
                                                                        .build(),
                                                        // Button 2: CANCEL (Pattern matched: Key + Tooltip)
                                                        ActionButton.builder(mm.deserialize("<red>✖ Cancel"))
                                                                        .tooltip(mm.deserialize(
                                                                                        "<red>Cancel and go back"))
                                                                        .action(DialogAction.customClick(CANCEL_BUY,
                                                                                        null))
                                                                        .build()));
                });
                open(p, dialog);
        }

        public static void showSelfRemoveDialog(Player p, AHItem item) {
                final Player seller = p;
                final Dialog dialog = Dialog.create(builder -> {
                        builder.empty()
                                        .base(DialogBase.builder(mm.deserialize("<white>Cancel Your Listing?"))
                                                        .body(List.of(
                                                                        DialogBody.plainMessage(mm.deserialize(
                                                                                        "<white>You are removing this listing:")),
                                                                        DialogBody.item(item.getItem()).build()))
                                                        .build())
                                        .type(DialogType.confirmation(
                                                        ActionButton.builder(mm.deserialize("<red>✔ Remove Listing"))
                                                                        .tooltip(mm.deserialize("<red>Remove from AH"))
                                                                        .action(DialogAction.customClick(
                                                                                        (view, audience) -> {
                                                                                                processRemoval(seller,
                                                                                                                item);
                                                                                        },
                                                                                        ClickCallback.Options.builder()
                                                                                                        .build()))
                                                                        .build(),
                                                        ActionButton.builder(mm.deserialize("<gray>✖ Keep Listing"))
                                                                        .tooltip(mm.deserialize("<gray>Keep it listed"))
                                                                        .action(DialogAction.customClick(CANCEL_REMOVE,
                                                                                        null))
                                                                        .build()));
                });
                open(p, dialog);
        }

        public static void showAdminRemoveDialog(Player p, AHItem item) {
                final Player admin = p;
                final Dialog dialog = Dialog.create(builder -> {
                        builder.empty()
                                        .base(DialogBase.builder(mm.deserialize("<red>Admin: Force Remove?"))
                                                        .body(List.of(DialogBody.item(item.getItem()).build()))
                                                        .build())
                                        .type(DialogType.confirmation(
                                                        ActionButton.builder(mm.deserialize("<red>✔ Force Remove"))
                                                                        .tooltip(mm.deserialize("<red>Admins only"))
                                                                        .action(DialogAction.customClick(
                                                                                        (view, audience) -> {
                                                                                                AHManager.get().removeItem(
                                                                                                                item);
                                                                                                AHManager.get().markUnclaimed(
                                                                                                                item.getSeller(),
                                                                                                                item.getItem());
                                                                                                admin.sendMessage(
                                                                                                                "§a§l[AH] §aListing force-removed.");
                                                                                        },
                                                                                        ClickCallback.Options.builder()
                                                                                                        .build()))
                                                                        .build(),
                                                        ActionButton.builder(mm.deserialize("<gray>✖ Cancel"))
                                                                        .tooltip(mm.deserialize("<gray>Take no action"))
                                                                        .action(DialogAction.customClick(CANCEL_ADMIN,
                                                                                        null))
                                                                        .build()));
                });
                open(p, dialog);
        }

        private static void open(Player p, Dialog d) {
                // NeonOrder uses p.getScheduler().run() (instant)
                p.getScheduler().run(NeonAH.get(), t -> p.showDialog(d), null);
        }

        public static void processPurchase(Player p, AHItem item) {
                if (!AHManager.get().getItems().contains(item)) {
                        p.sendMessage("§c§l[AH] §cItem no longer available!");
                        return;
                }
                if (p.getInventory().firstEmpty() == -1) {
                        p.sendMessage("§c§l[AH] §cYour inventory is full!");
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
                p.sendMessage("§a§l[AH] §aSuccessfully purchased!");
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        }

        public static void processRemoval(Player p, AHItem item) {
                if (!AHManager.get().getItems().contains(item))
                        return;
                if (p.getInventory().firstEmpty() == -1) {
                        p.sendMessage("§cPROJECT_ROOT/Data/projects/ninesmp/order/neonah/src/main/java/dev/neonjava/neonah/utils/AHDialogUtils.javaInventory full! Cannot reclaim.");
                        return;
                }
                AHManager.get().removeItem(item);
                p.getInventory().addItem(item.getItem());
                p.sendMessage("§a§l[AH] §aItem returned!");
        }
}
