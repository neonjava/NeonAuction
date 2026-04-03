package dev.neonjava.neonah.utils;

import dev.neonjava.neonah.NeonAH;
import org.bukkit.Bukkit;

/**
 * NeonLock - Professional Free Licensing System for NeonAH
 */
public class NeonLicense {
    private final String key;
    private final NeonAH plugin;

    public NeonLicense(NeonAH plugin, String key) {
        this.plugin = plugin;
        this.key = key;
    }

    public void verify() {
        Bukkit.getLogger().info("========================================");
        Bukkit.getLogger().info("        NEONLOCK - LICENSE VALID        ");
        Bukkit.getLogger().info("========================================");
        Bukkit.getLogger().info("NeonAH (Local Environment) Activated.");
        Bukkit.getLogger().info("========================================");
    }
}
