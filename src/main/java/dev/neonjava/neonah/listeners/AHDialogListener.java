package dev.neonjava.neonah.listeners;

import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import dev.neonjava.neonah.managers.AHManager;
import dev.neonjava.neonah.utils.AHDialogUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@SuppressWarnings("UnstableApiUsage")
public class AHDialogListener implements Listener {

    /**
     * Grace window for KEYS only. This filters out Geyser's residue clicks
     * if the player is using the Key fallback path.
     */
    private static final long KEY_GRACE_MS = 200L;

    @EventHandler
    public void onDialogClick(PlayerCustomClickEvent e) {
        String idString = e.getIdentifier().asString();
        if (!idString.startsWith("neonah:"))
            return;

        if (!(e.getCommonConnection() instanceof PlayerGameConnection con))
            return;
        final Player p = con.getPlayer();

        Bukkit.getLogger().info("[AH-Debug] KEY received: " + idString + " from " + p.getName());

        // Handle specific actions via Key (Fallback Path)
        if (idString.endsWith("/confirm")) {
            AHManager.PendingAction pa = AHManager.get().getPending(p.getUniqueId());
            if (pa == null)
                return;

            // Simple check: if Item is already gone from AH, don't execute
            if (!AHManager.get().getItems().contains(pa.item)) {
                AHManager.get().clearPending(p.getUniqueId());
                return;
            }

            Bukkit.getLogger().info("[AH-Debug] Executing via KEY fallback: " + pa.type + " for " + p.getName());

            // Execute based on type
            if (idString.contains("buy"))
                AHDialogUtils.processPurchase(p, pa.item);
            else if (idString.contains("remove"))
                AHDialogUtils.processRemoval(p, pa.item);
            else if (idString.contains("admin")) {
                AHManager.get().removeItem(pa.item);
                AHManager.get().markUnclaimed(pa.item.getSeller(), pa.item.getItem());
            }

            AHManager.get().clearPending(p.getUniqueId());

        } else if (idString.endsWith("/cancel")) {
            AHManager.get().clearPending(p.getUniqueId());
            p.getScheduler().run(dev.neonjava.neonah.NeonAH.get(), task -> p.closeInventory(), null);
        }
    }
}
