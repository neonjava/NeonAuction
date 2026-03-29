package dev.neonjava.neonah.obj;

import com.github.retrooper.packetevents.util.Vector3i;
import org.bukkit.block.BlockType;
import java.util.function.Consumer;

/**
 * SignInfo - Stores metadata for an active Bedrock Sign GUI session.
 * Replicated from NeonOrder.
 */
public record SignInfo(Consumer<String> action, BlockType signBlock, int line, Vector3i pos) {
}
