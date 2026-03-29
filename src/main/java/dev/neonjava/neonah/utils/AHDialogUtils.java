package dev.neonjava.neonah.utils;

import dev.neonjava.neonah.NeonAH;
import dev.neonjava.neonah.models.AHItem;
import dev.neonjava.neonah.managers.AHManager;
import dev.neonjava.neonah.menus.AHConfirmMenu;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * AHDialogUtils - Router for Confirmation Interfaces.
 * Handles both Modern Dialogs (Java 1.21.7+) and Chest GUIs (Bedrock/Legacy).
 */
public class AHDialogUtils {
        public static final Key KEY_CONFIRM_BUY = Key.key("neonah:buy/confirm");
        public static final Key KEY_CANCEL_BUY = Key.key("neonah:buy/cancel");
        public static final Key KEY_CONFIRM_REMOVE = Key.key("neonah:remove/confirm");
        public static final Key KEY_CANCEL_REMOVE = Key.key("neonah:remove/cancel");
        public static final Key KEY_CONFIRM_ADMIN = Key.key("neonah:admin/confirm");
        public static final Key KEY_CANCEL_ADMIN = Key.key("neonah:admin/cancel");

        public static void showBuyDialog(Player p, AHItem item) {
                AHManager.get().setPending(p.getUniqueId(), "buy", item);
                if (SafeDialog.isSupported() && !BedrockUtils.isBedrock(p)) {
                        AHDialogImpl.showBuy(p, item);
                } else {
                        new AHConfirmMenu("buy", item).open(p);
                }
        }

        public static void showSelfRemoveDialog(Player p, AHItem item) {
                AHManager.get().setPending(p.getUniqueId(), "remove", item);
                if (SafeDialog.isSupported() && !BedrockUtils.isBedrock(p)) {
                        AHDialogImpl.showSelfRemove(p, item);
                } else {
                        new AHConfirmMenu("remove", item).open(p);
                }
        }

        public static void showAdminRemoveDialog(Player p, AHItem item) {
                AHManager.get().setPending(p.getUniqueId(), "admin", item);
                if (SafeDialog.isSupported() && !BedrockUtils.isBedrock(p)) {
                        AHDialogImpl.showAdminRemove(p, item);
                } else {
                        new AHConfirmMenu("admin", item).open(p);
                }
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
                p.sendMessage("§a§l[AH] §aListing removed.");
        }
}
