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
    private final Map<UUID, String> searchQueries = new ConcurrentHashMap<>();
    private final Map<UUID, AHStorage.AHPlayerSettings> playerSettings = new ConcurrentHashMap<>();

    public AHStorage.AHPlayerSettings getPlayerSettings(UUID uuid) {
        return playerSettings.computeIfAbsent(uuid, storage::loadSettings);
    }

    public boolean isMessages(UUID uuid) {
        return getPlayerSettings(uuid).messages;
    }

    public void toggleMessages(UUID uuid) {
        AHStorage.AHPlayerSettings settings = getPlayerSettings(uuid);
        settings.messages = !settings.messages;
        storage.saveSettings(uuid, settings);
    }

    public boolean isQuickbuy(UUID uuid) {
        return getPlayerSettings(uuid).quickbuy;
    }

    public void toggleQuickbuy(UUID uuid) {
        AHStorage.AHPlayerSettings settings = getPlayerSettings(uuid);
        settings.quickbuy = !settings.quickbuy;
        storage.saveSettings(uuid, settings);
    }
    public static class PendingAction {
        public final String type;
        public final AHItem item;
        public final int page;
        public final dev.neonjava.neonah.menus.AHMainMenu.SortMode sortMode;
        public volatile long shownAt = Long.MAX_VALUE;

        public PendingAction(String type, AHItem item, int page, dev.neonjava.neonah.menus.AHMainMenu.SortMode sortMode) {
            this.type = type;
            this.item = item;
            this.page = page;
            this.sortMode = sortMode;
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

        String broadcastTemplate = ConfigManager.get().getMessage("global-broadcast");
        if (broadcastTemplate != null && !broadcastTemplate.isEmpty()) {
            String itemName = item.getItem().getType().name().replace("_", " ").toLowerCase();
            if (item.getItem().hasItemMeta() && item.getItem().getItemMeta().hasDisplayName()) {
                 itemName = net.kyori.adventure.text.serializer.plain.PlainComponentSerializer.plain()
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
        net.kyori.adventure.text.Component component = dev.neonjava.neonah.NeonAH.get().getMm().deserialize(message);
        for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (isMessages(p.getUniqueId())) {
                p.sendActionBar(component);
            }
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
    }

    public void markUnclaimed(UUID uuid, ItemStack item) {
        storage.addUnclaimed(uuid, item);
    }

    public void markUnclaimedByAdmin(UUID uuid, ItemStack item) {
        ItemStack cloned = item.clone();
        org.bukkit.inventory.meta.ItemMeta meta = cloned.getItemMeta();
        if (meta != null) {
            List<net.kyori.adventure.text.Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            lore.add(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<red>ᴛʜɪѕ ɪᴛᴇᴍ ᴡᴀѕ ʀᴇᴍᴏᴠᴇᴅ ʙʏ ᴀɴ ᴀᴅᴍɪɴ"));
            meta.lore(lore.stream().map(c -> c.decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false)).toList());
            cloned.setItemMeta(meta);
        }
        storage.addUnclaimed(uuid, cloned);
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
        pending.put(player, new PendingAction(type, item, 0, dev.neonjava.neonah.menus.AHMainMenu.SortMode.RECENT));
    }

    public void setPendingWithState(UUID player, String type, AHItem item, int page, dev.neonjava.neonah.menus.AHMainMenu.SortMode sortMode) {
        pending.put(player, new PendingAction(type, item, page, sortMode));
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

        activeItems.removeIf(item -> {
            // Listing duration expiry (listed_at based)
            if (now - item.getListedAt() > limit) {
                storage.deleteItem(item.getId());
                markUnclaimed(item.getSeller(), item.getItem());
                return true;
            }
            return false;
        });
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

    // --- Search Query Storage ---
    public void setSearchQuery(UUID player, String query) {
        if (query == null || query.isBlank()) {
            searchQueries.remove(player);
        } else {
            searchQueries.put(player, query);
        }
    }

    public String getSearchQuery(UUID player) {
        return searchQueries.get(player);
    }

    public void clearSearchQuery(UUID player) {
        searchQueries.remove(player);
    }
}
