package dev.neonjava.neonah.listeners;

import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import dev.neonjava.neonah.utils.AHDialogUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@SuppressWarnings("UnstableApiUsage")
public class AHDialogListener implements Listener {

    @EventHandler
    public void onDialogClick(PlayerCustomClickEvent e) {
        String idString = e.getIdentifier().asString();

        // Pattern match: Only handle cancel keys via Key listener
        // (Confirm is handled by the BiConsumer lambda in AHDialogUtils)
        if (idString.equals("neonah:buy/cancel") ||
                idString.equals("neonah:remove/cancel") ||
                idString.equals("neonah:admin/cancel")) {

            if (!(e.getCommonConnection() instanceof PlayerGameConnection con))
                return;
            final Player p = con.getPlayer();

            // Just close the inventory on cancel
            p.getScheduler().run(dev.neonjava.neonah.NeonAH.get(), t -> p.closeInventory(), null);
        }
    }
}
