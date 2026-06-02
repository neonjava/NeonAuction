package dev.neonjava.neonah.listeners;

import dev.neonjava.neonah.managers.AHManager;
import dev.neonjava.neonah.managers.ConfigManager;
import dev.neonjava.neonah.menus.*;
import dev.neonjava.neonah.utils.AHDialogUtils;
import dev.neonjava.neonah.models.AHItem;
import dev.neonjava.neonah.models.AHHistoryItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AHMenuListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof AHBaseMenu))
            return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;
        if (clicked.getType() == Material.BLACK_STAINED_GLASS_PANE)
            return;

        int slot = event.getRawSlot();

        if (holder instanceof AHMainMenu menu) {
            handleMain(player, menu, slot);
        } else if (holder instanceof AHHistoryMenu menu) {
            handleHistory(player, menu, slot);
        } else if (holder instanceof AHHistoryPreviewMenu) {
            if (slot == 49)
                new AHHistoryMenu(player).open(player);
        } else if (holder instanceof AHMyListingsMenu menu) {
            handleListings(player, menu, slot);
        } else if (holder instanceof AHConfirmMenu menu) {
            handleConfirm(player, menu, slot);
        }
    }

    private void handleMain(Player player, AHMainMenu menu, int slot) {
        switch (slot) {
            case 45 -> {
                menu.cycleSort();
                return;
            }
            case 46 -> {
                menu.refresh();
                return;
            }
            case 47 -> {
                player.closeInventory();
                dev.neonjava.neonah.utils.SignGUI.get().open(player, (p, lines) -> {
                    String search = lines[0].trim();
                    if (search.isEmpty()) {
                        AHManager.get().clearSearchQuery(p.getUniqueId());
                    } else {
                        AHManager.get().setSearchQuery(p.getUniqueId(), search);
                    }
                    new AHMainMenu(p, false).open(p);
                });
                return;
            }
            case 48 -> {
                menu.prev();
                return;
            }
            case 49 -> {
                player.closeInventory();
                return;
            }
            case 50 -> {
                menu.next();
                return;
            }
            case 52 -> {
                new AHMyListingsMenu(player).open(player);
                return;
            }
            case 53 -> {
                new AHHistoryMenu(player).open(player);
                return;
            }
        }

        if (slot >= 45)
            return;

        List<AHItem> items = getSortedItems(menu);
        int index = menu.getPage() * 45 + slot;
        if (index >= items.size())
            return;

        AHItem item = items.get(index);

        if (menu.isAdmin()) {
            if (player.hasPermission("neonah.admin.bypass")) {
                AHDialogUtils.processAdminRemoval(player, item);
                player.getScheduler().run(dev.neonjava.neonah.NeonAH.get(), t -> {
                    new AHMainMenu(player, true, menu.getPage(), menu.getSortMode()).open(player);
                }, null);
            } else {
                player.closeInventory();
                AHDialogUtils.showAdminRemoveDialog(player, item, menu.getPage(), menu.getSortMode());
            }
        } else if (item.getSeller().equals(player.getUniqueId())) {
            player.closeInventory();
            AHDialogUtils.showSelfRemoveDialog(player, item, menu.getPage(), menu.getSortMode());
        } else {
            player.closeInventory();
            AHDialogUtils.showBuyDialog(player, item);
        }
    }

    private void handleHistory(Player player, AHHistoryMenu menu, int slot) {
        if (slot < 45) {
            List<AHHistoryItem> history = AHManager.get().getHistory(player.getUniqueId());
            int index = (menu.getPage() * 45) + slot;
            if (index < history.size()) {
                AHHistoryItem hi = history.get(index);
                if (AHConfirmMenu.isShulker(hi.getItem())) {
                    new AHHistoryPreviewMenu(hi.getItem()).open(player);
                }
            }
            return;
        }

        switch (slot) {
            case 47 -> {
                player.closeInventory();
                dev.neonjava.neonah.utils.SignGUI.get().open(player, (p, lines) -> {
                    String q = lines[0].trim();
                    if (q.isEmpty()) return;
                    new AHHistoryMenu(p, q).open(p);
                });
            }
            case 48 -> menu.prev();
            case 49 -> new AHMainMenu(player, false).open(player);
            case 50 -> menu.next();
        }
    }

    private void handleListings(Player player, AHMyListingsMenu menu, int slot) {
        if (slot == 45)
            return; // Status info
        if (slot == 48) {
            menu.prev();
            return;
        }
        if (slot == 49) {
            new AHMainMenu(player, false).open(player);
            return;
        }
        if (slot == 50) {
            menu.next();
            return;
        }

        if (slot < 27) {
            List<AHItem> active = AHManager.get().getItems().stream()
                    .filter(i -> i.getSeller().equals(player.getUniqueId())).toList();
            int index = menu.getPage() * 27 + slot;
            if (index < active.size()) {
                if (player.hasPermission("neonah.admin.bypass")) {
                    AHDialogUtils.processRemoval(player, active.get(index));
                    menu.render();
                } else {
                    player.closeInventory();
                    AHDialogUtils.showSelfRemoveDialog(player, active.get(index), menu.getPage(), dev.neonjava.neonah.menus.AHMainMenu.SortMode.RECENT);
                }
            }
        } else if (slot >= 27 && slot < 36) {
            List<dev.neonjava.neonah.managers.AHStorage.UnclaimedEntry> unclaimed = AHManager.get()
                    .getUnclaimedEntries(player.getUniqueId());
            int idx = slot - 27;
            if (idx >= unclaimed.size()) {
                return;
            }
            if (player.getInventory().firstEmpty() == -1) {
                player.sendMessage(ConfigManager.get().getMiniMessage("at-inventory-full"));
                return;
            }
            dev.neonjava.neonah.managers.AHStorage.UnclaimedEntry entry = unclaimed.get(idx);
            ItemStack claimed = AHManager.get().claimUnclaimed(player.getUniqueId(), entry.id());
            if (claimed != null) {
                player.getInventory().addItem(claimed);
                player.sendMessage(ConfigManager.get().getMiniMessage("collected-unclaimed"));
                menu.render();
            }
        }
    }

    private void handleConfirm(Player player, AHConfirmMenu menu, int slot) {
        if (slot == 11) { // Confirm (Lime)
            player.closeInventory();
            if (menu.getType().equals("buy")) {
                AHDialogUtils.processPurchase(player, menu.getItem());
            } else if (menu.getType().equals("remove")) {
                AHDialogUtils.processRemoval(player, menu.getItem());
                new AHMyListingsMenu(player).open(player);
            } else if (menu.getType().equals("admin")) {
                AHManager.PendingAction pa = AHManager.get().getPending(player.getUniqueId());
                AHDialogUtils.processAdminRemoval(player, menu.getItem());
                player.getScheduler().run(dev.neonjava.neonah.NeonAH.get(), t -> {
                    if (pa != null) {
                        new AHMainMenu(player, true, pa.page, pa.sortMode).open(player);
                    } else {
                        new AHMainMenu(player, true).open(player);
                    }
                }, null);
            }
        } else if (slot == 15) { // Cancel (Red)
            // Go back to main menu
            new AHMainMenu(player, false).open(player);
        }
    }

    private List<AHItem> getSortedItems(AHMainMenu menu) {
        return AHMainMenu.getVisibleItems(menu.getViewer(), menu.getSortMode());
    }
}
