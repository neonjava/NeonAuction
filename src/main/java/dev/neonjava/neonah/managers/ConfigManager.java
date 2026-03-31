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
            return msg.replace("&", "§");
        });
    }

    public FileConfiguration getConfig() {
        return config;
    }
}
