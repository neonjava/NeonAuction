package dev.neonjava.neonah.managers;

import dev.neonjava.neonah.models.AHHistoryItem;
import dev.neonjava.neonah.models.AHItem;
import org.bukkit.inventory.ItemStack;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class AHManager {
    private static AHManager instance;
    private final AHStorage storage = new AHStorage();
    private final List<AHItem> activeItems = new CopyOnWriteArrayList<>();
    private final Map<UUID, PendingAction> pending = new ConcurrentHashMap<>();

    public static class PendingAction {
        public final String type;
        public final AHItem item;
        public volatile long shownAt = Long.MAX_VALUE; // Sentinel: not yet shown

        public PendingAction(String type, AHItem item) {
            this.type = type;
            this.item = item;
        }
    }

    private AHManager() {
    }

    public static AHManager get() {
        if (instance == null)
            instance = new AHManager();
        return instance;
    }

    public void init() {
        storage.init();
        activeItems.addAll(storage.loadActive());
    }

    public void shutdown() {
        storage.shutdown();
    }

    public List<AHItem> getItems() {
        return activeItems;
    }

    public List<ItemStack> getUnclaimed(UUID uuid) {
        return storage.loadUnclaimed(uuid);
    }

    public void addItem(AHItem item) {
        activeItems.add(item);
        storage.saveItem(item);
    }

    public void removeItem(AHItem item) {
        activeItems.remove(item);
        storage.deleteItem(item.getId());
    }

    public void markUnclaimed(UUID uuid, ItemStack item) {
        storage.addUnclaimed(uuid, item);
    }

    public void claimAll(UUID uuid) {
        storage.clearUnclaimed(uuid);
    }

    public void recordSale(UUID s, UUID b, double p, ItemStack item) {
        storage.recordHistory(s, b, p, item);
    }

    public List<AHHistoryItem> getHistory(UUID p) {
        return storage.getHistory(p);
    }

    public void setPending(UUID player, String type, AHItem item) {
        pending.put(player, new PendingAction(type, item));
    }

    public PendingAction getPending(UUID player) {
        return pending.get(player);
    }

    public void clearPending(UUID player) {
        pending.remove(player);
    }

    /**
     * Periodically called to check for expired auction listings.
     * Items that exceed the configured duration are moved to the seller's unclaimed
     * storage.
     */
    public void checkExpiry() {
        long now = System.currentTimeMillis();
        // Read expiry duration from config (default 24h)
        long expirySeconds = dev.neonjava.neonah.managers.ConfigManager.get()
                .getConfig().getLong("settings.expiry-duration", 86400L);
        long limit = expirySeconds * 1000L;

        for (AHItem item : activeItems) {
            if (now - item.getListedAt() > limit) {
                removeItem(item);
                markUnclaimed(item.getSeller(), item.getItem());
            }
        }
    }

    // --- Permission-based limits ---
    public int getLimit(org.bukkit.entity.Player p) {
        org.bukkit.configuration.ConfigurationSection limits = dev.neonjava.neonah.managers.ConfigManager.get()
                .getConfig().getConfigurationSection("limits");
        if (limits == null)
            return 3;

        int max = limits.getInt("default", 3);
        for (String key : limits.getKeys(false)) {
            if (key.equals("default"))
                continue;
            if (p.hasPermission(key)) {
                max = Math.max(max, limits.getInt(key));
            }
        }
        return max;
    }

    public boolean isAtLimit(org.bukkit.entity.Player p) {
        long active = activeItems.stream()
                .filter(i -> i.getSeller().equals(p.getUniqueId()))
                .count();
        return active >= getLimit(p);
    }
}
