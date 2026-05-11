package dev.neonjava.neonah;

import dev.neonjava.neonah.commands.AHCommandBuilder;
import dev.neonjava.neonah.managers.AHManager;
import dev.neonjava.neonah.listeners.AHMenuListener;
import dev.neonjava.neonah.listeners.AHDialogListener;
import dev.neonjava.neonah.utils.SignGUI;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.command.brigadier.Commands;

public class NeonAH extends JavaPlugin {
    private static NeonAH instance;
    private static Economy economy;
    private final net.kyori.adventure.text.minimessage.MiniMessage mm = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage();

    public net.kyori.adventure.text.minimessage.MiniMessage getMm() {
        return mm;
    }

    @Override
    public void onLoad() {
        // Initialize PacketEvents (matching NeonOrder for Bedrock stability)
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();

        PacketEvents.getAPI().getEventManager().registerListener(
                new SignGUI(this), PacketListenerPriority.NORMAL);
    }

    @Override
    public void onEnable() {
        instance = this;

        // Initialize PacketEvents API
        PacketEvents.getAPI().init();

        if (!setupEconomy()) {
            Bukkit.getLogger().warning("NeonAH: Vault economy provider not found yet. Will retry in 5 seconds...");
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (!setupEconomy()) {
                    Bukkit.getLogger().severe("NeonAH: Still no Vault economy provider found! Auction features will be limited.");
                } else {
                    Bukkit.getLogger().info("NeonAH: Vault economy provider connected!");
                }
            }, 100L); // 5 seconds
        }

        dev.neonjava.neonah.managers.ConfigManager.get().init();

        AHManager.get().init();

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();
            AHCommandBuilder builder = new AHCommandBuilder(this);
            commands.register(builder.buildMain("ah"), "Auction command", java.util.List.of("auction"));
            commands.register(builder.buildAdmin("ahadmin"), "Admin AH command", java.util.List.of());
        });

        // AHMenuListener — chest GUI clicks
        getServer().getPluginManager().registerEvents(new AHMenuListener(), this);
        // AHDialogListener — handles specific Cancel keys (mixed pattern)
        getServer().getPluginManager().registerEvents(new AHDialogListener(), this);

        // Periodically check for expired items (every 5 mins = 6000 ticks)
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> AHManager.get().checkExpiry(), 1200L, 6000L);

        Bukkit.getLogger().info("NeonAH v" + getDescription().getVersion() + " (with PacketEvents) Loaded!");
    }

    @Override
    public void onDisable() {
        // Shut down PacketEvents
        PacketEvents.getAPI().terminate();
        AHManager.get().shutdown();
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null)
            return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null)
            return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    public static NeonAH get() {
        return instance;
    }

    public static Economy getEcon() {
        return economy;
    }
}
