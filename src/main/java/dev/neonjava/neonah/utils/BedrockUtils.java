package dev.neonjava.neonah.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * BedrockUtils - Helper to detect if a player is on Bedrock (Geyser).
 * Replicated from NeonOrder.
 */
public class BedrockUtils {
    private static Method isBedrockPlayerMethod;
    private static Object geyserApi;

    static {
        try {
            Class<?> geyserApiClass = Class.forName("org.geysermc.geyser.api.GeyserApi");
            Method apiMethod = geyserApiClass.getMethod("api");
            geyserApi = apiMethod.invoke(null);
            isBedrockPlayerMethod = geyserApiClass.getMethod("isBedrockPlayer", UUID.class);
        } catch (Exception ignored) {
            // Geyser not installed
        }
    }

    public static boolean isBedrock(Player player) {
        if (isBedrockPlayerMethod == null)
            return false;
        try {
            return (boolean) isBedrockPlayerMethod.invoke(geyserApi, player.getUniqueId());
        } catch (Exception e) {
            return false;
        }
    }
}
