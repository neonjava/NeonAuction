package dev.neonjava.neonah.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.neonjava.neonah.NeonAH;
import dev.neonjava.neonah.managers.AHManager;
import dev.neonjava.neonah.models.AHItem;
import dev.neonjava.neonah.menus.AHMainMenu;
import dev.neonjava.neonah.utils.AHFormatter;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@SuppressWarnings("UnstableApiUsage")
public class AHCommandBuilder {

    public AHCommandBuilder(NeonAH plugin) {
    }

    public LiteralCommandNode<CommandSourceStack> buildMain(String name) {
        return Commands.literal(name)
                .executes(ctx -> {
                    if (ctx.getSource().getExecutor() instanceof Player p)
                        new AHMainMenu(p, false).open(p);
                    return 1;
                })
                .then(Commands.literal("sell")
                        .then(Commands.argument("price_str", StringArgumentType.string())
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getExecutor() instanceof Player p))
                                        return 0;
                                    String priceInput = StringArgumentType.getString(ctx, "price_str");
                                    double price = dev.neonjava.neonah.utils.AHFormatter.parsePrice(priceInput);

                                    if (price < 1.0) {
                                        p.sendMessage("§cInvalid price! Use a number or suffixes like 100k, 1m, 1b.");
                                        return 0;
                                    }

                                    ItemStack item = p.getInventory().getItemInMainHand();
                                    if (item.getType() == Material.AIR) {
                                        p.sendMessage("§cYou must hold an item to sell it!");
                                        return 0;
                                    }

                                    if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                                        String displayName = item.getItemMeta().getDisplayName().toUpperCase();
                                        boolean allowed = false;
                                        for (String special : dev.neonjava.neonah.managers.ConfigManager.get()
                                                .getConfig().getStringList("allowed-custom-names")) {
                                            if (displayName.contains(special.toUpperCase())) {
                                                allowed = true;
                                                break;
                                            }
                                        }
                                        if (!allowed) {
                                            p.sendMessage(dev.neonjava.neonah.managers.ConfigManager.get()
                                                    .getMessage("renamed-blocked"));
                                            return 0;
                                        }
                                    }

                                    AHManager.get().addItem(new AHItem(p.getUniqueId(), item.clone(), price));
                                    p.getInventory().setItemInMainHand(null);

                                    // Debug signal
                                    p.getServer().getLogger().info("[AH] Broadcasting listing from " + p.getName());

                                    // Broadcast new auction (no bold, uses messages.yml)
                                    String broadcastMsg = dev.neonjava.neonah.managers.ConfigManager.get()
                                            .getMessage("global-broadcast")
                                            .replace("%name%", p.getName())
                                            .replace("%price%", AHFormatter.formatPrice(price));

                                    p.getServer().sendMessage(
                                            net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(
                                                    AHFormatter.stripBold(broadcastMsg),
                                                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
                                                            .component("item", (item.getItemMeta() != null
                                                                    && item.getItemMeta().hasDisplayName()
                                                                            ? item.getItemMeta().displayName()
                                                                            : net.kyori.adventure.text.Component
                                                                                    .translatable(item.getType()
                                                                                            .translationKey())))));

                                    p.sendMessage(
                                            dev.neonjava.neonah.managers.ConfigManager.get().getMessage("item-listed")
                                                    .replace("%price%", String.valueOf(price))
                                                    .replace("%compact%", AHFormatter.formatPrice(price)));
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
                .then(Commands.literal("reload")
                        .executes(ctx -> {
                            dev.neonjava.neonah.managers.ConfigManager.get().reload();
                            ctx.getSource().getSender().sendMessage("§a§l[AH] §aConfiguration and Messages reloaded!");
                            return 1;
                        }))
                .then(Commands.literal("history")
                        .then(Commands.argument("player", StringArgumentType.string())
                                .executes(ctx -> {
                                    String target = StringArgumentType.getString(ctx, "player");
                                    OfflinePlayer op = Bukkit.getOfflinePlayer(target);
                                    if (!(ctx.getSource().getExecutor() instanceof Player sender))
                                        return 0;

                                    sender.sendMessage("§b§l[AH-Admin] "
                                            + dev.neonjava.neonah.utils.AHFormatter.toSmallCaps("History for " + target)
                                            + ":");
                                    AHManager.get().getHistory(op.getUniqueId()).forEach(hi -> {
                                        boolean sold = hi.getSeller().equals(op.getUniqueId());
                                        String type = sold ? "§c[SOLD]" : "§a[BOUGHT]";
                                        sender.sendMessage(type + " §f" + hi.getItem().getType() + " §e"
                                                + AHFormatter.formatPrice(hi.getPrice()));
                                    });
                                    return 1;
                                })))
                .build();
    }
}
