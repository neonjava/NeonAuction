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
    private dev.neon.coreplugin.CorePlugin core;

    private void setupRedis() {
        core = (dev.neon.coreplugin.CorePlugin) org.bukkit.Bukkit.getPluginManager().getPlugin("NeonCore");
        if (core != null) {
            core.getSyncManager().registerHandler(dev.neon.coreplugin.sync.SyncManager.CHANNEL_AH,
                    (channel, message) -> {
                        // message format: action:id
                        String[] parts = message.split(":");
                        if (parts.length < 2)
                            return;

                        String action = parts[0];
                        if (action.equals("refresh")) {
                            refreshFromDB();
                        }
                    });
        }
    }

    private void refreshFromDB() {
        activeItems.clear();
        activeItems.addAll(storage.loadActive());
    }

    private void notifySync() {
        if (core != null) {
            core.getRedisManager().publish(dev.neon.coreplugin.sync.SyncManager.CHANNEL_AH, "refresh:all");
        }
    }

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
        setupRedis();
        activeItems.addAll(storage.loadActive());
    }

    public void shutdown() {
        storage.shutdown();
    }

    public List<AHItem> getItems() {
        return activeItems;
    }

    public AHStorage getStorage() {
        return storage;
    }

    public List<ItemStack> getUnclaimed(UUID uuid) {
        return storage.loadUnclaimed(uuid);
    }

    public List<AHStorage.UnclaimedEntry> getUnclaimedEntries(UUID uuid) {
        return storage.loadUnclaimedEntries(uuid);
    }

    public ItemStack claimUnclaimed(UUID uuid, UUID unclaimedId) {
        return storage.claimUnclaimed(uuid, unclaimedId);
    }

    public void addItem(AHItem item) {
        activeItems.add(item);
        storage.saveItem(item);
        notifySync();

        // Send global broadcast via Redis RAW chat channel
        if (core != null) {
            String broadcastTemplate = ConfigManager.get().getMessage("global-broadcast");
            
            String itemName = item.getItem().getType().name().replace("_", " ").toLowerCase();
            if (item.getItem().hasItemMeta() && item.getItem().getItemMeta().hasDisplayName()) {
                 // Use legacy conversion to keep colors but avoid tag confusion
                 itemName = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand()
                         .serialize(item.getItem().getItemMeta().displayName());
            }

            String price = dev.neonjava.neonah.utils.AHFormatter.formatPrice(item.getPrice());
            
            String broadcast = broadcastTemplate
                    .replace("%prefix%", dev.neonjava.neonah.utils.LuckPermsUtils.getPrefix(item.getSeller()))
                    .replace("%suffix%", dev.neonjava.neonah.utils.LuckPermsUtils.getSuffix(item.getSeller()))
                    .replace("%username%", dev.neonjava.neonah.utils.LuckPermsUtils.getName(item.getSeller()))
                    .replace("%name%", dev.neonjava.neonah.utils.LuckPermsUtils.getName(item.getSeller()))
                    .replace("%item%", itemName)
                    .replace("%price%", price);

            broadcast(broadcast);
        }
    }

    public void broadcast(String message) {
        if (core != null) {
            core.getRedisManager().publish(dev.neon.coreplugin.sync.SyncManager.CHANNEL_CHAT, "RAW:" + message);
        }
    }

    public void removeItem(AHItem item) {
        if (item == null) {
            return;
        }
        removeItemById(item.getId());
    }

    public void removeItemById(UUID itemId) {
        if (itemId == null) {
            return;
        }
        activeItems.removeIf(i -> i.getId().equals(itemId));
        storage.deleteItem(itemId);
        notifySync();
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
        storage.purgeExpiredUnclaimed();
        long now = System.currentTimeMillis();
        long expirySeconds = dev.neonjava.neonah.managers.ConfigManager.get()
                .getConfig().getLong("settings.expiry-duration", 86400L);
        long limit = expirySeconds * 1000L;

        dev.neon.coreplugin.tools.AmethystToolManager toolManager = (core != null) ? core.getAmethystToolManager() : null;

        activeItems.removeIf(item -> {
            // 1. Listing duration expiry (listed_at based)
            if (now - item.getListedAt() > limit) {
                storage.deleteItem(item.getId());
                markUnclaimed(item.getSeller(), item.getItem());
                return true;
            }

            // 2. Amethyst tool expiry (PDC timestamp based)
            if (toolManager != null && toolManager.getToolType(item.getItem()) != null && toolManager.isExpired(item.getItem())) {
                storage.deleteItem(item.getId());
                // Breaks/disappears when expired anywhere
                return true;
            }
            return false;
        });

        if (toolManager != null) {
            storage.purgeExpiredToolsFromReturns(toolManager);
        }
        notifySync();
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
