package dev.neonjava.neonah.utils;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUpdateSign;
import dev.neonjava.neonah.NeonAH;
import dev.neonjava.neonah.obj.SignInfo;
import io.papermc.paper.math.Position;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockType;
import org.bukkit.block.Sign;
import org.bukkit.block.TileState;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

/**
 * SignGUI - Native Bedrock Sign Input Handler using PacketEvents.
 * Replicated from NeonOrder to ensure Bedrock text inputs work correctly.
 */
@SuppressWarnings("UnstableApiUsage")
public class SignGUI implements PacketListener {
    private static final HashMap<Player, SignInfo> sessionsList = new HashMap<>();
    private static final MiniMessage mm = MiniMessage.miniMessage();
    private final NeonAH plugin;

    public SignGUI(NeonAH plugin) {
        this.plugin = plugin;
    }

    public static void newSession(Player p, Consumer<String> action, List<String> lines, BlockType blockType,
            int line) {
        if (blockType == null || sessionsList.containsKey(p))
            return;

        final int x = (int) Math.floor(p.getX());
        int y = (int) Math.ceil(p.getY());
        final int z = (int) Math.floor(p.getZ());
        if (p.getPitch() < 0)
            y -= 4;
        else
            y += 5;

        Sign signState = (Sign) blockType.createBlockData().createBlockState();
        var frontSide = signState.getSide(Side.FRONT);
        for (int i = 0; i < Math.min(4, lines.size()); i++)
            frontSide.line(i, mm.deserialize(lines.get(i)));

        final Location loc = new Location(p.getWorld(), x, y, z);
        p.sendBlockChange(loc, signState.getBlockData());
        p.sendBlockUpdate(loc, signState);
        p.openVirtualSign(Position.block(loc), Side.FRONT);

        sessionsList.put(p, new SignInfo(action, blockType, line,
                new Vector3i(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())));
    }

    @Override
    public void onPacketReceive(@NonNull PacketReceiveEvent e) {
        if (e.getPacketType() != PacketType.Play.Client.UPDATE_SIGN)
            return;

        final Player player = (Player) e.getPlayer();
        final SignInfo info = sessionsList.get(player);
        if (info == null)
            return;

        final WrapperPlayClientUpdateSign wrapper = new WrapperPlayClientUpdateSign(e);
        if (!wrapper.getBlockPosition().equals(info.pos()))
            return;

        final String[] lines = wrapper.getTextLines();
        info.action().accept(lines[Math.max(0, info.line() - 1)]);

        sessionsList.remove(player);

        final Location loc = new Location(player.getWorld(), info.pos().getX(), info.pos().getY(), info.pos().getZ());
        Bukkit.getRegionScheduler().run(plugin, loc, t -> {
            player.sendBlockChange(loc, player.getWorld().getBlockData(loc));
            if (player.getWorld().getBlockState(loc) instanceof TileState tile)
                player.sendBlockUpdate(loc, tile);
        });

        e.setCancelled(true);
    }
}
