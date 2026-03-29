package dev.neonjava.neonah.utils;

import org.bukkit.entity.Player;
import java.lang.reflect.Method;
import java.util.UUID;

public class AHBedrock {
    public static boolean isBedrock(Player player) {
        try {
            Class<?> geyserApiClass = Class.forName("org.geysermc.geyser.api.GeyserApi");
            Method apiMethod = geyserApiClass.getMethod("api");
            Object apiInstance = apiMethod.invoke(null);
            Method isBedrockMethod = apiInstance.getClass().getMethod("isBedrockPlayer", UUID.class);
            return (boolean) isBedrockMethod.invoke(apiInstance, player.getUniqueId());
        } catch (Exception ignored) {
            return false;
        }
    }
}
