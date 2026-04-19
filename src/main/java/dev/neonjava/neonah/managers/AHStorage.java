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
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AHStorage {
    private dev.neon.coreplugin.CorePlugin core;

    public void init() {
        core = (dev.neon.coreplugin.CorePlugin) org.bukkit.Bukkit.getPluginManager().getPlugin("NeonCore");
        if (core == null) {
            org.bukkit.Bukkit.getLogger().severe("[NeonAH] CorePlugin not found! Cross-server sync disabled.");
        } else {
            createTables();
        }
    }

    private Connection getConnection() throws SQLException {
        if (core != null)
            return core.getDatabaseManager().getConnection();
        throw new SQLException("Core connection not available");
    }

    private void createTables() {
        try (Connection c = getConnection()) {
            ensureHealthyTable(c, "ah_active_v2",
                    "CREATE TABLE IF NOT EXISTS ah_active_v2 (" +
                            "id VARCHAR(36) PRIMARY KEY, seller VARCHAR(36), price DOUBLE, item_data LONGTEXT, listed_at BIGINT)");
            ensureHealthyTable(c, "ah_unclaimed_items",
                    "CREATE TABLE IF NOT EXISTS ah_unclaimed_items (" +
                            "id VARCHAR(36) PRIMARY KEY, seller VARCHAR(36), item_data LONGTEXT)");
            ensureHealthyTable(c, "ah_history_v2",
                    "CREATE TABLE IF NOT EXISTS ah_history_v2 (" +
                            "id VARCHAR(36) PRIMARY KEY, seller VARCHAR(36), buyer VARCHAR(36), price DOUBLE, item_data LONGTEXT, sold_at BIGINT)");

            ensureColumn(c, "ah_active_v2", "id", "VARCHAR(36) NOT NULL");
            ensureColumn(c, "ah_active_v2", "seller", "VARCHAR(36) NOT NULL");
            ensureColumn(c, "ah_active_v2", "price", "DOUBLE NOT NULL");
            ensureColumn(c, "ah_active_v2", "item_data", "LONGTEXT");
            ensureColumn(c, "ah_active_v2", "created_at", "BIGINT NULL");
            ensureColumn(c, "ah_active_v2", "listed_at", "BIGINT NOT NULL");

            ensureColumn(c, "ah_unclaimed_items", "id", "VARCHAR(36) NOT NULL");
            ensureColumn(c, "ah_unclaimed_items", "seller", "VARCHAR(36) NOT NULL");
            ensureColumn(c, "ah_unclaimed_items", "item_data", "LONGTEXT");

            ensureColumn(c, "ah_history_v2", "id", "VARCHAR(36) NOT NULL");
            ensureColumn(c, "ah_history_v2", "seller", "VARCHAR(36) NOT NULL");
            ensureColumn(c, "ah_history_v2", "buyer", "VARCHAR(36) NOT NULL");
            ensureColumn(c, "ah_history_v2", "price", "DOUBLE NOT NULL");
            ensureColumn(c, "ah_history_v2", "item_data", "LONGTEXT");
            ensureColumn(c, "ah_history_v2", "uuid", "VARCHAR(36) NULL");
            ensureColumn(c, "ah_history_v2", "type", "VARCHAR(16) NULL");
            ensureColumn(c, "ah_history_v2", "created_at", "BIGINT NULL");
            ensureColumn(c, "ah_history_v2", "sold_at", "BIGINT NOT NULL");

            ensurePrimaryKey(c, "ah_active_v2", "id");
            ensurePrimaryKey(c, "ah_unclaimed_items", "id");
            ensurePrimaryKey(c, "ah_history_v2", "id");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void ensureHealthyTable(Connection c, String table, String createSql) throws SQLException {
        try (Statement s = c.createStatement()) {
            s.execute(createSql);
            try (ResultSet ignored = s.executeQuery("SELECT 1 FROM " + table + " LIMIT 1")) {
                // table exists and is queryable
            }
        } catch (SQLException ex) {
            String msg = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
            if (msg.contains("doesn't exist in engine")
                    || msg.contains("does not exist in engine")
                    || msg.contains("tablespace already exists")) {
                try (Statement s = c.createStatement()) {
                    s.execute("DROP TABLE IF EXISTS " + table);
                    s.execute(createSql + " ENGINE=MyISAM");
                }
            } else {
                throw ex;
            }
        }
    }

    private void ensureColumn(Connection c, String table, String column, String ddlType) throws SQLException {
        if (columnExists(c, table, column)) {
            try (Statement s = c.createStatement()) {
                s.execute("ALTER TABLE " + table + " MODIFY COLUMN " + column + " " + ddlType);
            }
        } else {
            try (Statement s = c.createStatement()) {
                s.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + ddlType);
            }
        }
    }

    private boolean columnExists(Connection c, String table, String column) throws SQLException {
        DatabaseMetaData meta = c.getMetaData();
        try (ResultSet rs = meta.getColumns(c.getCatalog(), null, table, column)) {
            return rs.next();
        }
    }

    private void ensurePrimaryKey(Connection c, String table, String column) throws SQLException {
        DatabaseMetaData meta = c.getMetaData();
        boolean hasPk = false;
        try (ResultSet rs = meta.getPrimaryKeys(c.getCatalog(), null, table)) {
            while (rs.next()) {
                hasPk = true;
                break;
            }
        }
        if (!hasPk) {
            try (Statement s = c.createStatement()) {
                s.execute("ALTER TABLE " + table + " ADD PRIMARY KEY (" + column + ")");
            }
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
                    if (listedAt <= 0 && columnExists(c, "ah_active_v2", "created_at")) {
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
        List<ItemStack> items = new ArrayList<>();
        try (Connection c = getConnection();
                PreparedStatement ps = c
                        .prepareStatement("SELECT item_data FROM ah_unclaimed_items WHERE seller = ?")) {
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

    public void saveItem(AHItem item) {
        try (Connection c = getConnection();
                PreparedStatement ps = c.prepareStatement(
                        "REPLACE INTO ah_active_v2 (id, seller, price, item_data, created_at, listed_at) VALUES (?,?,?,?,?,?)")) {
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
                        .prepareStatement("INSERT INTO ah_unclaimed_items (id, seller, item_data) VALUES (?,?,?)")) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, seller.toString());
            ps.setString(3, serialize(item));
            ps.execute();
        } catch (SQLException e) {
            e.printStackTrace();
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
        String sql = "INSERT INTO ah_history_v2 (id, uuid, seller, buyer, price, item_data, type, created_at, sold_at) VALUES (?,?,?,?,?,?,?,?,?)";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            long now = System.currentTimeMillis();
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, seller.toString());
            ps.setString(3, seller.toString());
            ps.setString(4, buyer.toString());
            ps.setDouble(5, price);
            ps.setString(6, serialize(item));
            ps.setString(7, "SALE");
            ps.setLong(8, now);
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
}
