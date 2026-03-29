package dev.neonjava.neonah.commands;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.neonjava.neonah.NeonAH;
import dev.neonjava.neonah.managers.AHManager;
import dev.neonjava.neonah.models.AHItem;
import dev.neonjava.neonah.menus.AHMainMenu;
import dev.neonjava.neonah.menus.AHHistoryMenu;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@SuppressWarnings("UnstableApiUsage")
public class AHCommandBuilder {
    private final NeonAH plugin;

    public AHCommandBuilder(NeonAH plugin) {
        this.plugin = plugin;
    }

    public LiteralCommandNode<CommandSourceStack> buildMain(String name) {
        return Commands.literal(name)
                .executes(ctx -> {
                    if (ctx.getSource().getExecutor() instanceof Player p)
                        new AHMainMenu(p, false).open(p);
                    return 1;
                })
                .then(Commands.literal("sell")
                        .then(Commands.argument("price", DoubleArgumentType.doubleArg(1.0))
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getExecutor() instanceof Player p))
                                        return 0;
                                    double price = DoubleArgumentType.getDouble(ctx, "price");
                                    ItemStack item = p.getInventory().getItemInMainHand();
                                    if (item.getType() == Material.AIR) {
                                        p.sendMessage("§cYou must hold an item to sell it!");
                                        return 0;
                                    }
                                    AHManager.get().addItem(new AHItem(p.getUniqueId(), item.clone(), price));
                                    p.getInventory().setItemInMainHand(null);
                                    p.sendMessage("§a§l[AH] §aItem listed for " + price + "₹");
                                    return 1;
                                })))
                .build();
    }

    public LiteralCommandNode<CommandSourceStack> buildAdmin(String name) {
        return Commands.literal(name)
                .requires(stack -> stack.getSender().hasPermission("neonah.admin"))
                .executes(ctx -> {
                    if (ctx.getSource().getExecutor() instanceof Player p)
                        new AHMainMenu(p, true).open(p);
                    return 1;
                })
                .then(Commands.literal("history")
                        .then(Commands.argument("player", StringArgumentType.string())
                                .executes(ctx -> {
                                    String target = StringArgumentType.getString(ctx, "player");
                                    OfflinePlayer op = Bukkit.getOfflinePlayer(target);
                                    if (!(ctx.getSource().getExecutor() instanceof Player sender))
                                        return 0;

                                    sender.sendMessage("§b§l[AH-Admin] §7History for " + target + ":");
                                    AHManager.get().getHistoryStrings(op.getUniqueId()).forEach(sender::sendMessage);
                                    return 1;
                                })))
                .build();
    }
}
