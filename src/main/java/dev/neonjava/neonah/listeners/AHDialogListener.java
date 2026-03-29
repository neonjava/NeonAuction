package dev.neonjava.neonah.listeners;

import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@SuppressWarnings("UnstableApiUsage")
public class AHDialogListener implements Listener {

    @EventHandler
    public void onDialogClick(PlayerCustomClickEvent e) {
        String idString = e.getIdentifier().asString();

        // GLOBAL DEBUG: Log EVERY click identifier received from any dialog
        if (e.getCommonConnection() instanceof PlayerGameConnection con) {
            Player p = con.getPlayer();
            Bukkit.getLogger().info("[AH-Debug] Click received from " + p.getName() + " | Identifier: " + idString);
        }

        // Handle specific cancel keys (NeonOrder Mixed Pattern)
        if (idString.equals("neonah:buy/cancel") ||
                idString.equals("neonah:remove/cancel") ||
                idString.equals("neonah:admin/cancel")) {

            if (!(e.getCommonConnection() instanceof PlayerGameConnection con))
                return;
            final Player p = con.getPlayer();

            Bukkit.getLogger().info("[AH-Debug] Processing CANCEL key for " + p.getName());
            p.getScheduler().run(dev.neonjava.neonah.NeonAH.get(), t -> p.closeInventory(), null);
        }
    }
}
