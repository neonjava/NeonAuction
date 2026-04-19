package dev.neonjava.neonah.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.neonjava.neonah.NeonAH;
import dev.neonjava.neonah.managers.AHManager;
import dev.neonjava.neonah.managers.ConfigManager;
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
                                        p.sendMessage(ConfigManager.get().getMiniMessage("invalid-price"));
                                        return 0;
                                    }

                                    ItemStack item = p.getInventory().getItemInMainHand();
                                    if (item.getType() == Material.AIR) {
                                        p.sendMessage(ConfigManager.get().getMiniMessage("hold-item"));
                                        return 0;
                                    }

                                    if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                                        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                                        String displayName = meta.getDisplayName().toUpperCase();

                                        // Whitelist check
                                        boolean whitelisted = false;
                                        for (String special : dev.neonjava.neonah.managers.ConfigManager.get()
                                                .getConfig().getStringList("allowed-custom-names")) {
                                            if (displayName.contains(special.toUpperCase())) {
                                                whitelisted = true;
                                                break;
                                            }
                                        }

                                        if (!whitelisted) {
                                            // Anvil Detection Logic
                                            boolean isAnvilRenamed = false;

                                            // 1. Check for RepairCost (Anvils always add this)
                                            if (meta instanceof org.bukkit.inventory.meta.Repairable r
                                                    && r.hasRepairCost() && r.getRepairCost() > 0) {
                                                isAnvilRenamed = true;
                                            }

                                            // 2. Check for Italicization (Default for anvil renames)
                                            if (meta.displayName() != null && meta.displayName().hasDecoration(
                                                    net.kyori.adventure.text.format.TextDecoration.ITALIC)) {
                                                isAnvilRenamed = true;
                                            }

                                            if (isAnvilRenamed) {
                                                p.sendMessage(ConfigManager.get().getMiniMessage("renamed-blocked"));
                                                return 0;
                                            }
                                        }
                                    }

                                    AHManager.get().addItem(new AHItem(p.getUniqueId(), item.clone(), price));
                                    p.getInventory().setItemInMainHand(null);

                                    // Debug signal
                                    p.getServer().getLogger().info("[AH] Broadcasting listing from " + p.getName());

                                    p.sendMessage(
                                            ConfigManager.parse(ConfigManager.get().getMessage("item-listed")
                                                    .replace("%price%", String.valueOf(price))
                                                    .replace("%compact%", AHFormatter.formatPrice(price))));
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
                            ctx.getSource().getSender().sendMessage(
                                    net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(
                                            "<gradient:#84fab0:#8fd3f4><bold>[AH]</bold></gradient> <green>Configuration and Messages reloaded!"));
                            return 1;
                        }))
                .then(Commands.literal("history")
                        .then(Commands.argument("player", StringArgumentType.string())
                                .executes(ctx -> {
                                    String target = StringArgumentType.getString(ctx, "player");
                                    OfflinePlayer op = Bukkit.getOfflinePlayer(target);
                                    if (!(ctx.getSource().getExecutor() instanceof Player sender))
                                        return 0;

                                    net.kyori.adventure.text.minimessage.MiniMessage mm = net.kyori.adventure.text.minimessage.MiniMessage
                                            .miniMessage();
                                    sender.sendMessage(mm.deserialize(
                                            "<gradient:#8fd3f4:#84fab0><bold>[AH-Admin]</bold></gradient> <white>History for <aqua>"
                                                    + target + "<white>:"));
                                    AHManager.get().getHistory(op.getUniqueId()).forEach(hi -> {
                                        boolean sold = hi.getSeller().equals(op.getUniqueId());
                                        String type = sold ? "<red>[SOLD]" : "<green>[BOUGHT]";
                                        sender.sendMessage(
                                                mm.deserialize(type + " <white>" + hi.getItem().getType() + " <yellow>"
                                                        + AHFormatter.formatPrice(hi.getPrice())));
                                    });
                                    return 1;
                                })))
                .build();
    }
}
