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
    private Connection connection;

    public void init() {
        try {
            File dataFolder = NeonAH.get().getDataFolder();
            if (!dataFolder.exists())
                dataFolder.mkdirs();

            Class.forName("org.sqlite.JDBC");
            connection = DriverManager
                    .getConnection("jdbc:sqlite:" + new File(dataFolder, "auction.db").getAbsolutePath());

            try (Statement s = connection.createStatement()) {
                s.execute("CREATE TABLE IF NOT EXISTS active_items (" +
                        "id TEXT PRIMARY KEY, seller TEXT, price DOUBLE, item TEXT, listed_at LONG)");

                s.execute("CREATE TABLE IF NOT EXISTS unclaimed_items (" +
                        "id TEXT PRIMARY KEY, seller TEXT, item TEXT)");

                s.execute("CREATE TABLE IF NOT EXISTS history (" +
                        "id TEXT PRIMARY KEY, seller TEXT, buyer TEXT, price DOUBLE, item_blob TEXT, sold_at LONG)");

                // Migration: Check if item_blob exists, if not, rename item_name or add it
                try {
                    s.execute("ALTER TABLE history ADD COLUMN item_blob TEXT");
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<AHItem> loadActive() {
        List<AHItem> items = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM active_items")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ItemStack is = deserialize(rs.getString("item"));
                if (is != null) {
                    items.add(new AHItem(
                            UUID.fromString(rs.getString("id")),
                            UUID.fromString(rs.getString("seller")),
                            is,
                            rs.getDouble("price"),
                            rs.getLong("listed_at")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    public List<ItemStack> loadUnclaimed(UUID seller) {
        List<ItemStack> items = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT item FROM unclaimed_items WHERE seller = ?")) {
            ps.setString(1, seller.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ItemStack is = deserialize(rs.getString("item"));
                if (is != null)
                    items.add(is);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    public void saveItem(AHItem item) {
        try (PreparedStatement ps = connection
                .prepareStatement("INSERT OR REPLACE INTO active_items VALUES (?,?,?,?,?)")) {
            ps.setString(1, item.getId().toString());
            ps.setString(2, item.getSeller().toString());
            ps.setDouble(3, item.getPrice());
            ps.setString(4, serialize(item.getItem()));
            ps.setLong(5, item.getListedAt());
            ps.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteItem(UUID id) {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM active_items WHERE id = ?")) {
            ps.setString(1, id.toString());
            ps.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addUnclaimed(UUID seller, ItemStack item) {
        try (PreparedStatement ps = connection
                .prepareStatement("INSERT INTO unclaimed_items (id, seller, item) VALUES (?,?,?)")) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, seller.toString());
            ps.setString(3, serialize(item));
            ps.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void clearUnclaimed(UUID seller) {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM unclaimed_items WHERE seller = ?")) {
            ps.setString(1, seller.toString());
            ps.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void recordHistory(UUID seller, UUID buyer, double price, ItemStack item) {
        String sql = "INSERT INTO history (id, seller, buyer, price, item_blob, sold_at) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, seller.toString());
            ps.setString(3, buyer.toString());
            ps.setDouble(4, price);
            ps.setString(5, serialize(item));
            ps.setLong(6, System.currentTimeMillis());
            ps.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<AHHistoryItem> getHistory(UUID player) {
        List<AHHistoryItem> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM history WHERE seller = ? OR buyer = ? ORDER BY sold_at DESC LIMIT 100")) {
            ps.setString(1, player.toString());
            ps.setString(2, player.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ItemStack is = deserialize(rs.getString("item_blob"));
                if (is == null)
                    is = new ItemStack(Material.PAPER); // Fail-safe for old entries

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
            if (connection != null && !connection.isClosed())
                connection.close();
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
}
