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
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AHDialogUtils - Router for Confirmation Interfaces.
 * Now strictly uses Chest GUIs for better compatibility and design.
 */
public class AHDialogUtils {
        private static final Set<UUID> locks = ConcurrentHashMap.newKeySet();

        public static void showBuyDialog(Player p, AHItem item) {
                if (AHManager.get().isQuickbuy(p.getUniqueId())) {
                        processPurchase(p, item);
                        p.closeInventory();
                } else {
                        AHManager.get().setPending(p.getUniqueId(), "buy", item);
                        new AHConfirmMenu("buy", item).open(p);
                }
        }

        public static void showSelfRemoveDialog(Player p, AHItem item, int page, dev.neonjava.neonah.menus.AHMainMenu.SortMode sortMode) {
                AHManager.get().setPendingWithState(p.getUniqueId(), "remove", item, page, sortMode);
                new AHConfirmMenu("remove", item).open(p);
        }

        public static void showAdminRemoveDialog(Player p, AHItem item, int page, dev.neonjava.neonah.menus.AHMainMenu.SortMode sortMode) {
                if (p.hasPermission("neonah.admin.bypass")) {
                    processAdminRemoval(p, item);
                    return;
                }
                AHManager.get().setPendingWithState(p.getUniqueId(), "admin", item, page, sortMode);
                new AHConfirmMenu("admin", item).open(p);
        }

        public static void processPurchase(Player p, AHItem item) {
                if (p.getInventory().firstEmpty() == -1) {
                        p.sendMessage(ConfigManager.get().getMiniMessage("at-inventory-full"));
                        return;
                }

                UUID itemId = item.getId();
                if (!locks.add(itemId)) {
                        p.sendMessage(ConfigManager.get().getMiniMessage("item-already-bought"));
                        return;
                }

                try {
                        AHItem liveItem = AHManager.get().getStorage().loadActiveItem(itemId);
                        if (liveItem == null) {
                                p.sendMessage(ConfigManager.get().getMiniMessage("item-already-bought"));
                                return;
                        }

                        double balance = NeonAH.getEcon().getBalance(p);
                        if (balance < liveItem.getPrice()) {
                                p.sendMessage(ConfigManager.get().getMiniMessage("insufficient-funds"));
                                return;
                        }

                        // Transactional actions
                        NeonAH.getEcon().withdrawPlayer(p, liveItem.getPrice());
                        NeonAH.getEcon().depositPlayer(Bukkit.getOfflinePlayer(liveItem.getSeller()),
                                        liveItem.getPrice());
                        AHManager.get().recordSale(liveItem.getSeller(), p.getUniqueId(), liveItem.getPrice(),
                                        liveItem.getItem());

                        // Final item removal and distribution
                        AHManager.get().removeItemById(liveItem.getId());
                        Map<Integer, ItemStack> leftovers = p.getInventory().addItem(liveItem.getItem());
                        if (!leftovers.isEmpty()) {
                                for (ItemStack leftover : leftovers.values()) {
                                        AHManager.get().markUnclaimed(p.getUniqueId(), leftover);
                                }
                                p.sendMessage(ConfigManager.get().getMiniMessage("inventory-full-returns"));
                        }

                        p.sendMessage(ConfigManager.get().getMiniMessage("purchase-success"));
                        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

                        // Broadcast locally
                        String itemName = liveItem.getItem().getType().name().replace("_", " ").toLowerCase();
                        if (liveItem.getItem().hasItemMeta() && liveItem.getItem().getItemMeta().hasDisplayName()) {
                            itemName = net.kyori.adventure.text.serializer.plain.PlainComponentSerializer.plain()
                                    .serialize(liveItem.getItem().getItemMeta().displayName());
                        }

                        String broadcastMsg = ConfigManager.get().getMessage("buy-broadcast")
                                        .replace("%prefix%", dev.neonjava.neonah.utils.LuckPermsUtils.getPrefix(p.getUniqueId()))
                                        .replace("%suffix%", dev.neonjava.neonah.utils.LuckPermsUtils.getSuffix(p.getUniqueId()))
                                        .replace("%username%", p.getName())
                                        .replace("%buyer%", p.getName())
                                        .replace("%item%", itemName)
                                        .replace("%seller_prefix%", dev.neonjava.neonah.utils.LuckPermsUtils.getPrefix(liveItem.getSeller()))
                                        .replace("%seller_suffix%", dev.neonjava.neonah.utils.LuckPermsUtils.getSuffix(liveItem.getSeller()))
                                        .replace("%seller%", (Bukkit.getOfflinePlayer(liveItem.getSeller()).getName() != null ? Bukkit.getOfflinePlayer(liveItem.getSeller()).getName() : "Unknown"))
                                        .replace("%price%", AHFormatter.formatPrice(liveItem.getPrice()));
                        AHManager.get().broadcast(broadcastMsg);
                } finally {
                        locks.remove(itemId);
                }
        }

        public static void processRemoval(Player p, AHItem item) {
                if (p.getInventory().firstEmpty() == -1) {
                        p.sendMessage(ConfigManager.get().getMiniMessage("at-inventory-full"));
                        return;
                }

                UUID itemId = item.getId();
                if (!locks.add(itemId)) return;

                try {
                        if (!AHManager.get().getStorage().exists(itemId))
                                return;
                        AHManager.get().removeItem(item);
                        p.getInventory().addItem(item.getItem());
                        p.sendMessage(ConfigManager.get().getMiniMessage("listing-removed"));
                } finally {
                        locks.remove(itemId);
                }
        }

        public static void processAdminRemoval(Player p, AHItem item) {
                UUID itemId = item.getId();
                if (!locks.add(itemId)) return;

                try {
                        if (!AHManager.get().getStorage().exists(itemId))
                                return;
                        AHManager.get().removeItem(item);
                        AHManager.get().markUnclaimedByAdmin(item.getSeller(), item.getItem());
                        p.sendMessage(ConfigManager.get().getMiniMessage("listing-force-removed"));
                } finally {
                        locks.remove(itemId);
                }
        }
}
