package dev.neonjava.neonah.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * BedrockUtils - Helper to detect if a player is on Bedrock (Geyser/Floodgate).
 * Updated with both Geyser and Floodgate detection for maximum reliability.
 */
public class BedrockUtils {
    private static Method geyserIsBedrock;
    private static Object geyserApi;
    private static Method floodgateIsBedrock;
    private static Object floodgateApi;

    static {
        // Geyser API Detection
        try {
            Class<?> geyserClass = Class.forName("org.geysermc.geyser.api.GeyserApi");
            geyserApi = geyserClass.getMethod("api").invoke(null);
            geyserIsBedrock = geyserClass.getMethod("isBedrockPlayer", UUID.class);
            Bukkit.getLogger().info("[AH] Geyser API detected for Bedrock support.");
        } catch (Exception ignored) {
        }

        // Floodgate API Detection (Fallback)
        try {
            Class<?> floodgateClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            floodgateApi = floodgateClass.getMethod("getInstance").invoke(null);
            floodgateIsBedrock = floodgateClass.getMethod("isFloodgatePlayer", UUID.class);
            Bukkit.getLogger().info("[AH] Floodgate API detected for Bedrock support.");
        } catch (Exception ignored) {
        }
    }

    public static boolean isBedrock(Player player) {
        UUID uuid = player.getUniqueId();

        // Try Geyser API first
        if (geyserIsBedrock != null) {
            try {
                if ((boolean) geyserIsBedrock.invoke(geyserApi, uuid))
                    return true;
            } catch (Exception ignored) {
            }
        }

        // Try Floodgate API second
        if (floodgateIsBedrock != null) {
            try {
                if ((boolean) floodgateIsBedrock.invoke(floodgateApi, uuid))
                    return true;
            } catch (Exception ignored) {
            }
        }

        // Check for common Bedrock prefix (last resort)
        return player.getName().startsWith(".") || player.getName().startsWith("*");
    }
}
