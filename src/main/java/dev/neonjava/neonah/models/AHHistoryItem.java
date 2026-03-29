package dev.neonjava.neonah.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.inventory.ItemStack;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class AHHistoryItem {
    private final UUID seller;
    private final UUID buyer;
    private final double price;
    private final ItemStack item; // The full serialized item
    private final long timestamp;
}
