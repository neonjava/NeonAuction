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
     * Grace window: ignore any confirm/cancel event that arrives within this many
     * milliseconds of the dialog being sent. This filters out Geyser's "echo"
     * confirm
     * which fires because the tap that selected the item in the chest GUI
     * propagates
     * through to the modal form as an immediate button press.
     */
    private static final long GRACE_MS = 800L;

    @EventHandler
    public void onDialogClick(PlayerCustomClickEvent e) {
        String idString = e.getIdentifier().asString();
        if (!idString.startsWith("neonah:"))
            return;

        if (!(e.getCommonConnection() instanceof PlayerGameConnection con))
            return;
        final Player p = con.getPlayer();

        Bukkit.getLogger().info("[AH-Debug] Key received: " + idString + " | player=" + p.getName());

        if (idString.equals("neonah:confirm")) {
            AHManager.PendingAction pa = AHManager.get().getPending(p.getUniqueId());

            if (pa == null) {
                Bukkit.getLogger().warning("[AH-Debug] neonah:confirm — NO pending action for " + p.getName());
                return;
            }

            // Grace window — reject auto-fires from Geyser tap propagation
            long timeSinceShown = System.currentTimeMillis() - pa.shownAt;
            if (timeSinceShown < GRACE_MS) {
                Bukkit.getLogger().info("[AH-Debug] IGNORED auto-confirm for " + p.getName()
                        + " — only " + timeSinceShown + "ms since dialog shown (grace=" + GRACE_MS + "ms)");
                return;
            }

            // Clear BEFORE processing to prevent duplicate execution
            AHManager.get().clearPending(p.getUniqueId());
            Bukkit.getLogger().info("[AH-Debug] Executing pending action: " + pa.type
                    + " for " + p.getName() + " (" + timeSinceShown + "ms after show)");
            process(p, pa);

        } else if (idString.equals("neonah:cancel")) {
            AHManager.PendingAction pa = AHManager.get().getPending(p.getUniqueId());

            if (pa != null) {
                long timeSinceShown = System.currentTimeMillis() - pa.shownAt;
                if (timeSinceShown < GRACE_MS) {
                    Bukkit.getLogger().info("[AH-Debug] IGNORED auto-cancel for " + p.getName()
                            + " — only " + timeSinceShown + "ms since dialog shown");
                    return;
                }
            }

            AHManager.get().clearPending(p.getUniqueId());
            Bukkit.getLogger().info("[AH-Debug] neonah:cancel — closing for " + p.getName());
            p.closeInventory();
        }
    }

    private void process(Player p, AHManager.PendingAction pa) {
        switch (pa.type) {
            case "BUY" -> AHDialogUtils.processPurchase(p, pa.item);
            case "REMOVE" -> AHDialogUtils.processRemoval(p, pa.item);
            case "ADMIN" -> {
                AHManager.get().removeItem(pa.item);
                AHManager.get().markUnclaimed(pa.item.getSeller(), pa.item.getItem());
                p.sendMessage("§a§l[AH] §aListing removed successfully.");
            }
        }
    }
}
