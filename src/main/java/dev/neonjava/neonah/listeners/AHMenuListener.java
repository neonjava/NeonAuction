package dev.neonjava.neonah.listeners;

import dev.neonjava.neonah.managers.AHManager;
import dev.neonjava.neonah.menus.*;
import dev.neonjava.neonah.utils.AHDialogUtils;
import dev.neonjava.neonah.utils.BedrockUtils;
import dev.neonjava.neonah.models.AHItem;
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
        } else if (holder instanceof AHHistoryMenu) {
            if (slot == 49)
                new AHMainMenu(player, false).open(player);
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
            case 47 -> {
                menu.refresh();
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

        // Close inventory FIRST, then show dialog/menu (NeonOrder pattern)
        player.closeInventory();

        boolean isBedrock = BedrockUtils.isBedrock(player);
        if (isBedrock) {
            new AHConfirmMenu(
                    menu.isAdmin() ? "admin" : (item.getSeller().equals(player.getUniqueId()) ? "remove" : "buy"), item)
                    .open(player);
        } else {
            if (menu.isAdmin()) {
                AHDialogUtils.showAdminRemoveDialog(player, item);
            } else if (item.getSeller().equals(player.getUniqueId())) {
                AHDialogUtils.showSelfRemoveDialog(player, item);
            } else {
                AHDialogUtils.showBuyDialog(player, item);
            }
        }
    }

    private void handleListings(Player player, AHMyListingsMenu menu, int slot) {
        if (slot == 49) {
            new AHMainMenu(player, false).open(player);
            return;
        }

        if (slot < 27) {
            List<AHItem> active = AHManager.get().getItems().stream()
                    .filter(i -> i.getSeller().equals(player.getUniqueId())).toList();
            if (slot < active.size()) {
                player.closeInventory();
                AHDialogUtils.showSelfRemoveDialog(player, active.get(slot));
            }
        } else if (slot >= 27 && slot < 45) {
            List<ItemStack> unclaimed = AHManager.get().getUnclaimed(player.getUniqueId());
            if (!unclaimed.isEmpty()) {
                if (player.getInventory().firstEmpty() == -1) {
                    player.sendMessage("§c§l[AH] §cInventory full! Cannot reclaim.");
                    return;
                }
                AHManager.get().claimAll(player.getUniqueId());
                unclaimed.forEach(is -> player.getInventory().addItem(is));
                player.sendMessage("§a§l[AH] §aAll unlisted items reclaimed!");
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
            } else if (menu.getType().equals("admin")) {
                AHManager.get().removeItem(menu.getItem());
                AHManager.get().markUnclaimed(menu.getItem().getSeller(), menu.getItem().getItem());
                player.sendMessage("§a§l[AH] §aListing force-removed.");
            }
        } else if (slot == 15 || slot == 13) { // Cancel (Red) or Item (Optional)
            // Go back to main menu
            new AHMainMenu(player, false).open(player);
        }
    }

    private List<AHItem> getSortedItems(AHMainMenu menu) {
        List<AHItem> items = new ArrayList<>(AHManager.get().getItems());
        switch (menu.getSortMode()) {
            case RECENT -> items.sort(Comparator.comparingLong(AHItem::getListedAt).reversed());
            case LOW -> items.sort(Comparator.comparingDouble(AHItem::getPrice));
            case HIGH -> items.sort(Comparator.comparingDouble(AHItem::getPrice).reversed());
        }
        return items;
    }
}
