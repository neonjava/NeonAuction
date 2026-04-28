package dev.neonjava.neonah.managers;

import dev.neonjava.neonah.NeonAH;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private static ConfigManager instance;
    private final NeonAH plugin;

    private FileConfiguration config;
    private final Map<String, String> messageCache = new HashMap<>();

    private ConfigManager(NeonAH plugin) {
        this.plugin = plugin;
    }

    public static ConfigManager get() {
        if (instance == null) {
            instance = new ConfigManager(NeonAH.get());
        }
        return instance;
    }

    public void init() {
        plugin.saveDefaultConfig();
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        messageCache.clear();
    }

    public String getMessage(String path) {
        return messageCache.computeIfAbsent(path, k -> {
            // Check in messages section first
            String msg = config.getString("messages." + k);
            if (msg == null) {
                // Fallback to direct path or legacy
                msg = config.getString(k);
            }
            if (msg == null)
                return "<red>Missing message: " + path;
            return msg;
        });
    }

    public Component getMiniMessage(String path) {
        return parse(getMessage(path));
    }

    public static Component parse(String msg) {
        if (msg == null)
            return Component.empty();

        // Standardize legacy character
        msg = msg.replace('§', '&');

        // Check if it's a MiniMessage (contains tags like <gradient>, <bold>, etc.)
        // We'll try to deserialize with MiniMessage first as it's the modern standard.
        try {
            // Convert legacy & codes to MiniMessage if they exist for compatibility
            // This is a common pattern for "best of both worlds"
            String mmString = convertLegacyToMiniMessage(msg);
            return MiniMessage.miniMessage().deserialize(mmString);
        } catch (Exception ex) {
            // Fallback to legacy if MiniMessage fails
            return LegacyComponentSerializer.legacyAmpersand().deserialize(msg);
        }
    }

    private static String convertLegacyToMiniMessage(String msg) {
        return msg.replace("&0", "<black>").replace("&1", "<dark_blue>").replace("&2", "<dark_green>")
                .replace("&3", "<dark_aqua>").replace("&4", "<dark_red>").replace("&5", "<dark_purple>")
                .replace("&6", "<gold>").replace("&7", "<gray>").replace("&8", "<dark_gray>")
                .replace("&9", "<blue>").replace("&a", "<green>").replace("&b", "<aqua>")
                .replace("&c", "<red>").replace("&d", "<light_purple>").replace("&e", "<yellow>")
                .replace("&f", "<white>").replace("&k", "<obfuscated>").replace("&l", "<bold>")
                .replace("&m", "<strikethrough>").replace("&n", "<underline>").replace("&o", "<italic>")
                .replace("&r", "<reset>");
    }

    public FileConfiguration getConfig() {
        return config;
    }
}
