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
 * NeonAH Dialog Utilities — Hybrid Java+Bedrock approach
 *
 * BEDROCK (Geyser) FINDINGS:
 * - BiConsumer lambdas (customClick with options) are NOT called for Bedrock
 * players
 * - Key-based buttons (customClick with Key) ARE received via
 * PlayerCustomClickEvent
 * - Geyser SWAPS button order: firstArg in confirmation() = visual CANCEL,
 * secondArg = visual CONFIRM
 *
 * SOLUTION:
 * - Store pending action in AHManager before opening dialog
 * - Use Keys for buttons (swapped order for Bedrock)
 * - AHDialogListener handles neonah:confirm → execute pending, neonah:cancel →
 * close only
 */
@SuppressWarnings("UnstableApiUsage")
public class AHDialogUtils {
        private static final MiniMessage mm = MiniMessage.miniMessage();

        // Simple keys — no slashes, no dynamic content — Geyser sends these reliably
        public static final Key KEY_CONFIRM = Key.key("neonah:confirm");
        public static final Key KEY_CANCEL = Key.key("neonah:cancel");

        public static void showBuyDialog(Player p, AHItem item) {
                // Store pending BEFORE opening dialog
                AHManager.get().setPending(p.getUniqueId(), "BUY", item);
                Bukkit.getLogger().info("[AH-Debug] showBuyDialog — pending set for " + p.getName()
                                + " | item=" + item.getItem().getType() + " | price=" + item.getPrice());

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
                                /**
                                 * IMPORTANT — Bedrock button order is SWAPPED vs Java:
                                 * - firstArg → Bedrock shows as CANCEL button (sends KEY_CANCEL)
                                 * - secondArg → Bedrock shows as CONFIRM button (sends KEY_CONFIRM)
                                 */
                                .type(DialogType.confirmation(
                                                ActionButton.builder(mm.deserialize("<red>✖ Cancel"))
                                                                .action(DialogAction.customClick(KEY_CANCEL, null))
                                                                .build(),
                                                ActionButton.builder(mm.deserialize("<green>✔ Confirm"))
                                                                .action(DialogAction.customClick(KEY_CONFIRM, null))
                                                                .build())));
                open(p, dialog);
        }

        public static void showSelfRemoveDialog(Player p, AHItem item) {
                AHManager.get().setPending(p.getUniqueId(), "REMOVE", item);
                Bukkit.getLogger().info("[AH-Debug] showSelfRemoveDialog — pending set for " + p.getName());

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
                                                ActionButton.builder(mm.deserialize("<gray>✖ Keep Listing"))
                                                                .action(DialogAction.customClick(KEY_CANCEL, null))
                                                                .build(),
                                                ActionButton.builder(mm.deserialize("<red>✔ Remove Listing"))
                                                                .action(DialogAction.customClick(KEY_CONFIRM, null))
                                                                .build())));
                open(p, dialog);
        }

        public static void showAdminRemoveDialog(Player admin, AHItem item) {
                AHManager.get().setPending(admin.getUniqueId(), "ADMIN", item);
                Bukkit.getLogger().info("[AH-Debug] showAdminRemoveDialog — pending set for " + admin.getName());

                Dialog dialog = Dialog.create(builder -> builder.empty()
                                .base(DialogBase.builder(mm.deserialize("<red>Admin: Force Remove Listing?"))
                                                .body(List.of(
                                                                DialogBody.plainMessage(mm.deserialize(
                                                                                "<white>You are force-unlisting this item:")),
                                                                DialogBody.item(item.getItem()).build()))
                                                .build())
                                .type(DialogType.confirmation(
                                                ActionButton.builder(mm.deserialize("<gray>✖ Cancel"))
                                                                .action(DialogAction.customClick(KEY_CANCEL, null))
                                                                .build(),
                                                ActionButton.builder(mm.deserialize("<red>✔ Confirm Removal"))
                                                                .action(DialogAction.customClick(KEY_CONFIRM, null))
                                                                .build())));
                open(admin, dialog);
        }

        /**
         * Opens dialog with a 5-tick delay after inventory close.
         *
         * WHY: Without a delay, Geyser auto-fires neonah:confirm immediately because
         * the Bedrock client hasn't finished processing the inventory-close packet
         * before the modal form packet arrives. The delay gives the client time to
         * reset.
         *
         * Inventory is closed in AHMenuListener BEFORE this is called.
         */
        private static void open(Player p, Dialog d) {
                Bukkit.getLogger().info("[AH-Debug] open() — scheduling showDialog in 5 ticks for " + p.getName());
                p.getScheduler().runDelayed(NeonAH.get(), t -> {
                        Bukkit.getLogger().info("[AH-Debug] showDialog() sent to " + p.getName());
                        p.showDialog(d);
                        // Stamp the exact time dialog packet was sent so listener can apply grace
                        // window
                        AHManager.PendingAction pa = AHManager.get().getPending(p.getUniqueId());
                        if (pa != null)
                                pa.shownAt = System.currentTimeMillis();
                }, null, 5L);
        }

        // ─── Purchase / Removal logic ─────────────────────────────────────────────

        public static void processPurchase(Player p, AHItem item) {
                Bukkit.getLogger().info("[AH-Debug] processPurchase START — buyer=" + p.getName()
                                + " | item=" + item.getItem().getType() + " | price=" + item.getPrice());

                if (!AHManager.get().getItems().contains(item)) {
                        Bukkit.getLogger().warning("[AH-Debug] Item no longer in active list!");
                        p.sendMessage("§c§l[AH] §cItem no longer available!");
                        return;
                }
                if (p.getInventory().firstEmpty() == -1) {
                        Bukkit.getLogger().info("[AH-Debug] Buyer inventory full!");
                        p.sendMessage("§c§l[AH] §cYour inventory is full! Cannot buy.");
                        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        return;
                }
                double balance = NeonAH.getEcon().getBalance(p);
                if (balance < item.getPrice()) {
                        Bukkit.getLogger().info("[AH-Debug] Insufficient funds! balance=" + balance);
                        p.sendMessage("§c§l[AH] §cInsufficient funds!");
                        return;
                }

                Bukkit.getLogger().info("[AH-Debug] All checks passed — executing transaction");
                NeonAH.getEcon().withdrawPlayer(p, item.getPrice());
                NeonAH.getEcon().depositPlayer(Bukkit.getOfflinePlayer(item.getSeller()), item.getPrice());
                AHManager.get().recordSale(item.getSeller(), p.getUniqueId(), item.getPrice(),
                                item.getItem().getType().toString());
                AHManager.get().removeItem(item);
                p.getInventory().addItem(item.getItem());
                p.sendMessage("§a§l[AH] §aSuccessfully purchased for §e" + AHFormatter.formatPrice(item.getPrice()));
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                Bukkit.getLogger().info("[AH-Debug] processPurchase COMPLETE for " + p.getName());
        }

        public static void processRemoval(Player p, AHItem item) {
                Bukkit.getLogger().info("[AH-Debug] processRemoval START — seller=" + p.getName());
                if (!AHManager.get().getItems().contains(item))
                        return;
                if (p.getInventory().firstEmpty() == -1) {
                        p.sendMessage("§c§l[AH] §cInventory full! Cannot reclaim item.");
                        return;
                }
                AHManager.get().removeItem(item);
                p.getInventory().addItem(item.getItem());
                p.sendMessage("§a§l[AH] §aListing removed and item returned!");
                Bukkit.getLogger().info("[AH-Debug] processRemoval COMPLETE for " + p.getName());
        }
}
