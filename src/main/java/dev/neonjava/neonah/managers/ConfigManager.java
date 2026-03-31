package dev.neonjava.neonah.managers;

import dev.neonjava.neonah.NeonAH;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private static ConfigManager instance;
    private final NeonAH plugin;

    private FileConfiguration config;
    private FileConfiguration messages;
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

        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.messages = YamlConfiguration.loadConfiguration(messagesFile);

        // Load default messages if missing
        InputStream defMessagesStream = plugin.getResource("messages.yml");
        if (defMessagesStream != null) {
            this.messages.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defMessagesStream)));
        }

        messageCache.clear();
    }

    public String getMessage(String path) {
        return messageCache.computeIfAbsent(path, k -> {
            String msg = messages.getString(k);
            if (msg == null)
                return "§cMissing message: " + k;
            return msg;
        });
    }

    public net.kyori.adventure.text.Component getMiniMessage(String path) {
        return parse(getMessage(path));
    }

    public static net.kyori.adventure.text.Component parse(String msg) {
        if (msg == null)
            return net.kyori.adventure.text.Component.empty();
        // Automatic legacy Support: Convert & codes to MiniMessage tags if present
        if (msg.contains("&")) {
            msg = msg.replace("&0", "<black>").replace("&1", "<dark_blue>").replace("&2", "<dark_green>")
                    .replace("&3", "<dark_aqua>").replace("&4", "<dark_red>").replace("&5", "<dark_purple>")
                    .replace("&6", "<gold>").replace("&7", "<gray>").replace("&8", "<dark_gray>")
                    .replace("&9", "<blue>").replace("&a", "<green>").replace("&b", "<aqua>")
                    .replace("&c", "<red>").replace("&d", "<light_purple>").replace("&e", "<yellow>")
                    .replace("&f", "<white>").replace("&k", "<obfuscated>").replace("&l", "<bold>")
                    .replace("&m", "<strikethrough>").replace("&n", "<underline>").replace("&o", "<italic>")
                    .replace("&r", "<reset>");
        }
        return net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(msg);
    }

    public FileConfiguration getConfig() {
        return config;
    }
}
