package dev.neonjava.neonah.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.inventory.ItemStack;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class AHItem {
    private final UUID id;
    private final UUID seller;
    private final ItemStack item;
    private final double price;
    private final long listedAt;

    public AHItem(UUID seller, ItemStack item, double price) {
        this(UUID.randomUUID(), seller, item, price, System.currentTimeMillis());
    }
}
