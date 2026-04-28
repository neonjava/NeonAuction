package dev.neonjava.neonah.utils;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public class LuckPermsUtils {
    private static LuckPerms lp;

    static {
        try {
            lp = LuckPermsProvider.get();
        } catch (Exception ignored) {}
    }

    public static String getPrefix(UUID uuid) {
        if (lp == null) return "";
        User user = lp.getUserManager().getUser(uuid);
        if (user == null) {
            // User not loaded, return empty (loading from DB is async and complex here)
            return ""; 
        }
        String prefix = user.getCachedData().getMetaData().getPrefix();
        return prefix != null ? prefix : "";
    }

    public static String getSuffix(UUID uuid) {
        if (lp == null) return "";
        User user = lp.getUserManager().getUser(uuid);
        if (user == null) return "";
        String suffix = user.getCachedData().getMetaData().getSuffix();
        return suffix != null ? suffix : "";
    }

    public static String getName(UUID uuid) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        return op.getName() != null ? op.getName() : uuid.toString();
    }
}
