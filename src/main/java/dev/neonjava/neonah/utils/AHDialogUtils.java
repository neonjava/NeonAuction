package dev.neonjava.neonah.utils;

import dev.neonjava.neonah.NeonAH;
import dev.neonjava.neonah.models.AHItem;
import dev.neonjava.neonah.managers.AHManager;
import dev.neonjava.neonah.managers.ConfigManager;
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
                if (p.getInventory().firstEmpty() == -1) {
                        p.sendMessage(ConfigManager.get().getMessage("at-inventory-full"));
                        return;
                }

                dev.neon.coreplugin.sync.SyncManager sync = dev.neon.coreplugin.CorePlugin
                                .getPlugin(dev.neon.coreplugin.CorePlugin.class).getSyncManager();
                String lockKey = "ah_lock:" + item.getId();

                sync.acquireLock(lockKey).thenAccept(acquired -> {
                        if (!acquired) {
                                p.sendMessage(ConfigManager.get().getMessage("item-already-bought"));
                                return;
                        }

                        try {
                                // Re-verify existence in DB
                                if (!AHManager.get().getStorage().exists(item.getId())) {
                                        p.sendMessage(ConfigManager.get().getMessage("item-already-bought"));
                                        return;
                                }

                                double balance = NeonAH.getEcon().getBalance(p);
                                if (balance < item.getPrice()) {
                                        p.sendMessage(ConfigManager.get().getMessage("insufficient-funds"));
                                        return;
                                }

                                // Transactional actions
                                NeonAH.getEcon().withdrawPlayer(p, item.getPrice());
                                NeonAH.getEcon().depositPlayer(Bukkit.getOfflinePlayer(item.getSeller()),
                                                item.getPrice());
                                AHManager.get().recordSale(item.getSeller(), p.getUniqueId(), item.getPrice(),
                                                item.getItem());

                                // Final item removal and distribution
                                AHManager.get().removeItem(item);
                                p.getInventory().addItem(item.getItem());

                                p.sendMessage(ConfigManager.get().getMessage("purchase-success"));
                                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

                                // Broadcast
                                String broadcastMsg = ConfigManager.get().getMessage("buy-broadcast")
                                                .replace("%buyer%", p.getName())
                                                .replace("%seller%",
                                                                Bukkit.getOfflinePlayer(item.getSeller()).getName())
                                                .replace("%price%", AHFormatter.formatPrice(item.getPrice()));

                                Bukkit.getServer().sendMessage(net.kyori.adventure.text.minimessage.MiniMessage
                                                .miniMessage().deserialize(
                                                                AHFormatter.stripBold(broadcastMsg),
                                                                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
                                                                                .component(
                                                                                                "item",
                                                                                                (item.getItem().getItemMeta() != null
                                                                                                                && item.getItem()
                                                                                                                                .getItemMeta()
                                                                                                                                .hasDisplayName()
                                                                                                                                                ? item.getItem().getItemMeta()
                                                                                                                                                                .displayName()
                                                                                                                                                : net.kyori.adventure.text.Component
                                                                                                                                                                .translatable(item
                                                                                                                                                                                .getItem()
                                                                                                                                                                                .getType()
                                                                                                                                                                                .translationKey())))));
                        } finally {
                                sync.releaseLock(lockKey);
                        }
                });
        }

        public static void processRemoval(Player p, AHItem item) {
                if (p.getInventory().firstEmpty() == -1) {
                        p.sendMessage("§cInventory full!");
                        return;
                }

                dev.neon.coreplugin.sync.SyncManager sync = dev.neon.coreplugin.CorePlugin
                                .getPlugin(dev.neon.coreplugin.CorePlugin.class).getSyncManager();
                String lockKey = "ah_lock:" + item.getId();

                sync.acquireLock(lockKey).thenAccept(acquired -> {
                        if (!acquired)
                                return;
                        try {
                                if (!AHManager.get().getStorage().exists(item.getId()))
                                        return;
                                AHManager.get().removeItem(item);
                                p.getInventory().addItem(item.getItem());
                                p.sendMessage("§a§l[AH] §aListing removed.");
                        } finally {
                                sync.releaseLock(lockKey);
                        }
                });
        }
}
