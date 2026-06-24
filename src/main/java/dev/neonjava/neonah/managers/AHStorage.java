package dev.neonjava.neonah.managers;

import dev.neonjava.neonah.NeonAH;
import dev.neonjava.neonah.models.AHHistoryItem;
import dev.neonjava.neonah.models.AHItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AHStorage {
    public record UnclaimedEntry(UUID id, ItemStack item, long createdAt) {
    }

    private Connection connection;

    public void init() {
        try {
            File dataFolder = NeonAH.get().getDataFolder();
            if (!dataFolder.exists())
                dataFolder.mkdirs();

            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + new File(dataFolder, "auction.db").getAbsolutePath());
            createTables();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Connection getConnection() throws SQLException {
        if (connection != null && !connection.isClosed())
            return connection;
        
        File dataFolder = NeonAH.get().getDataFolder();
        connection = DriverManager.getConnection("jdbc:sqlite:" + new File(dataFolder, "auction.db").getAbsolutePath());
        return connection;
    }

    private void createTables() {
        try (Connection c = getConnection()) {
            try (Statement s = c.createStatement()) {
                s.execute("CREATE TABLE IF NOT EXISTS ah_active_v2 (" +
                        "id VARCHAR(36) PRIMARY KEY, seller VARCHAR(36), price DOUBLE, item_data LONGTEXT, listed_at BIGINT, created_at BIGINT)");
                s.execute("CREATE TABLE IF NOT EXISTS ah_unclaimed_items (" +
                        "id VARCHAR(36) PRIMARY KEY, seller VARCHAR(36), item_data LONGTEXT, created_at BIGINT)");
                s.execute("CREATE TABLE IF NOT EXISTS ah_history_v2 (" +
                        "id VARCHAR(36) PRIMARY KEY, seller VARCHAR(36), buyer VARCHAR(36), price DOUBLE, item_data LONGTEXT, sold_at BIGINT, uuid VARCHAR(36), type VARCHAR(16), created_at BIGINT)");
                s.execute("CREATE TABLE IF NOT EXISTS ah_player_settings (" +
                        "uuid VARCHAR(36) PRIMARY KEY, messages BOOLEAN, quickbuy BOOLEAN)");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<AHItem> loadActive() {
        List<AHItem> items = new ArrayList<>();
        try (Connection c = getConnection();
                PreparedStatement ps = c.prepareStatement("SELECT * FROM ah_active_v2")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ItemStack is = deserialize(rs.getString("item_data"));
                if (is != null) {
                    long listedAt = rs.getLong("listed_at");
                    if (listedAt <= 0) {
                        listedAt = rs.getLong("created_at");
                    }
                    items.add(new AHItem(
                            UUID.fromString(rs.getString("id")),
                            UUID.fromString(rs.getString("seller")),
                            is,
                            rs.getDouble("price"),
                            listedAt > 0 ? listedAt : System.currentTimeMillis()));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    public List<ItemStack> loadUnclaimed(UUID seller) {
        purgeExpiredUnclaimed();
        List<ItemStack> items = new ArrayList<>();
        try (Connection c = getConnection();
                PreparedStatement ps = c
                        .prepareStatement("SELECT item_data FROM ah_unclaimed_items WHERE seller = ? ORDER BY created_at DESC")) {
            ps.setString(1, seller.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ItemStack is = deserialize(rs.getString("item_data"));
                if (is != null)
                    items.add(is);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    public List<UnclaimedEntry> loadUnclaimedEntries(UUID seller) {
        purgeExpiredUnclaimed();
        List<UnclaimedEntry> items = new ArrayList<>();
        try (Connection c = getConnection();
                PreparedStatement ps = c.prepareStatement(
                        "SELECT id, item_data, created_at FROM ah_unclaimed_items WHERE seller = ? ORDER BY created_at DESC")) {
            ps.setString(1, seller.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ItemStack is = deserialize(rs.getString("item_data"));
                if (is != null) {
                    items.add(new UnclaimedEntry(
                            UUID.fromString(rs.getString("id")),
                            is,
                            rs.getLong("created_at")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    public void saveItem(AHItem item) {
        try (Connection c = getConnection();
                PreparedStatement ps = c.prepareStatement(
                        "INSERT OR REPLACE INTO ah_active_v2 (id, seller, price, item_data, created_at, listed_at) VALUES (?,?,?,?,?,?)")) {
            long now = item.getListedAt() > 0 ? item.getListedAt() : System.currentTimeMillis();
            ps.setString(1, item.getId().toString());
            ps.setString(2, item.getSeller().toString());
            ps.setDouble(3, item.getPrice());
            ps.setString(4, serialize(item.getItem()));
            ps.setLong(5, now);
            ps.setLong(6, now);
            ps.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean exists(UUID id) {
        try (Connection c = getConnection();
                PreparedStatement ps = c.prepareStatement("SELECT 1 FROM ah_active_v2 WHERE id = ?")) {
            ps.setString(1, id.toString());
            return ps.executeQuery().next();
        } catch (SQLException e) {
            return false;
        }
    }

    public AHItem loadActiveItem(UUID id) {
        String sql = "SELECT * FROM ah_active_v2 WHERE id = ? LIMIT 1";
        try (Connection c = getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id.toString());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                return null;
            }
            ItemStack is = deserialize(rs.getString("item_data"));
            if (is == null) {
                return null;
            }
            return new AHItem(
                    UUID.fromString(rs.getString("id")),
                    UUID.fromString(rs.getString("seller")),
                    is,
                    rs.getDouble("price"),
                    rs.getLong("listed_at"));
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void deleteItem(UUID id) {
        try (Connection c = getConnection();
                PreparedStatement ps = c.prepareStatement("DELETE FROM ah_active_v2 WHERE id = ?")) {
            ps.setString(1, id.toString());
            ps.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addUnclaimed(UUID seller, ItemStack item) {
        try (Connection c = getConnection();
                PreparedStatement ps = c
                        .prepareStatement("INSERT INTO ah_unclaimed_items (id, seller, item_data, created_at) VALUES (?,?,?,?)")) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, seller.toString());
            ps.setString(3, serialize(item));
            ps.setLong(4, System.currentTimeMillis());
            ps.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public ItemStack claimUnclaimed(UUID seller, UUID unclaimedId) {
        String select = "SELECT item_data FROM ah_unclaimed_items WHERE id = ? AND seller = ? LIMIT 1";
        String delete = "DELETE FROM ah_unclaimed_items WHERE id = ? AND seller = ?";
        try (Connection c = getConnection();
                PreparedStatement sel = c.prepareStatement(select);
                PreparedStatement del = c.prepareStatement(delete)) {
            sel.setString(1, unclaimedId.toString());
            sel.setString(2, seller.toString());
            ResultSet rs = sel.executeQuery();
            if (!rs.next()) {
                return null;
            }
            ItemStack item = deserialize(rs.getString("item_data"));
            del.setString(1, unclaimedId.toString());
            del.setString(2, seller.toString());
            del.execute();
            return item;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void clearUnclaimed(UUID seller) {
        try (Connection c = getConnection();
                PreparedStatement ps = c.prepareStatement("DELETE FROM ah_unclaimed_items WHERE seller = ?")) {
            ps.setString(1, seller.toString());
            ps.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void recordHistory(UUID seller, UUID buyer, double price, ItemStack item) {
        String sql = "INSERT INTO ah_history_v2 (id, seller, buyer, price, item_data, sold_at, uuid, type, created_at) VALUES (?,?,?,?,?,?,?,?,?)";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            long now = System.currentTimeMillis();
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, seller.toString());
            ps.setString(3, buyer.toString());
            ps.setDouble(4, price);
            ps.setString(5, serialize(item));
            ps.setLong(6, now);
            ps.setString(7, seller.toString());
            ps.setString(8, "SALE");
            ps.setLong(9, now);
            ps.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<AHHistoryItem> getHistory(UUID player) {
        List<AHHistoryItem> list = new ArrayList<>();
        try (Connection c = getConnection();
                PreparedStatement ps = c.prepareStatement(
                        "SELECT * FROM ah_history_v2 WHERE seller = ? OR buyer = ? ORDER BY sold_at DESC LIMIT 100")) {
            ps.setString(1, player.toString());
            ps.setString(2, player.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ItemStack is = deserialize(rs.getString("item_data"));
                if (is == null)
                    is = new ItemStack(Material.PAPER);
                list.add(new AHHistoryItem(
                        UUID.fromString(rs.getString("seller")),
                        UUID.fromString(rs.getString("buyer")),
                        rs.getDouble("price"),
                        is,
                        rs.getLong("sold_at")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void shutdown() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void purgeExpiredUnclaimed() {
        long retentionDays = ConfigManager.get().getConfig().getLong("settings.unclaimed-retention-days", 10L);
        long cutoff = System.currentTimeMillis() - (retentionDays * 24L * 60L * 60L * 1000L);
        try (Connection c = getConnection();
                PreparedStatement ps = c.prepareStatement("DELETE FROM ah_unclaimed_items WHERE created_at < ?")) {
            ps.setLong(1, cutoff);
            ps.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String serialize(ItemStack item) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream();
                BukkitObjectOutputStream boos = new BukkitObjectOutputStream(os)) {
            boos.writeObject(item);
            return Base64Coder.encodeLines(os.toByteArray());
        } catch (Exception e) {
            return "";
        }
    }

    private ItemStack deserialize(String data) {
        try (ByteArrayInputStream is = new ByteArrayInputStream(Base64Coder.decodeLines(data));
                BukkitObjectInputStream bois = new BukkitObjectInputStream(is)) {
            return (ItemStack) bois.readObject();
        } catch (Exception e) {
            return null;
        }
    }

    public static class AHPlayerSettings {
        public boolean messages = true;
        public boolean quickbuy = false;

        public AHPlayerSettings(boolean messages, boolean quickbuy) {
            this.messages = messages;
            this.quickbuy = quickbuy;
        }

        public AHPlayerSettings() {}
    }

    public AHPlayerSettings loadSettings(UUID uuid) {
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT messages, quickbuy FROM ah_player_settings WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new AHPlayerSettings(rs.getBoolean("messages"), rs.getBoolean("quickbuy"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new AHPlayerSettings(true, false);
    }

    public void saveSettings(UUID uuid, AHPlayerSettings settings) {
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement("INSERT OR REPLACE INTO ah_player_settings (uuid, messages, quickbuy) VALUES (?, ?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setBoolean(2, settings.messages);
            ps.setBoolean(3, settings.quickbuy);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
