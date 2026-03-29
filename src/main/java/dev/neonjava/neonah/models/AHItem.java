package dev.neonjava.neonah.models;

import org.bukkit.inventory.ItemStack;
import java.util.UUID;

public class AHItem {
    private final UUID id;
    private final UUID seller;
    private final ItemStack item;
    private final double price;
    private final long listedAt;

    public AHItem(UUID seller, ItemStack item, double price) {
        this(UUID.randomUUID(), seller, item, price, System.currentTimeMillis());
    }

    public AHItem(UUID id, UUID seller, ItemStack item, double price, long listedAt) {
        this.id = id;
        this.seller = seller;
        this.item = item;
        this.price = price;
        this.listedAt = listedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSeller() {
        return seller;
    }

    public ItemStack getItem() {
        return item;
    }

    public double getPrice() {
        return price;
    }

    public long getListedAt() {
        return listedAt;
    }
}
