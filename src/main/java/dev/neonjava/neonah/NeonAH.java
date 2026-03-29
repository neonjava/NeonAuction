package dev.neonjava.neonah;

import dev.neonjava.neonah.commands.AHCommandBuilder;
import dev.neonjava.neonah.managers.AHManager;
import dev.neonjava.neonah.listeners.AHMenuListener;
import dev.neonjava.neonah.listeners.AHDialogListener;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.command.brigadier.Commands;

public class NeonAH extends JavaPlugin {
    private static NeonAH instance;
    private static Economy economy;

    @Override
    public void onEnable() {
        instance = this;

        if (!setupEconomy()) {
            Bukkit.getLogger().severe("NeonAH could not find Vault economy provider!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        AHManager.get().init();

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();
            AHCommandBuilder builder = new AHCommandBuilder(this);
            commands.register(builder.buildMain("ah"), "Auction command", java.util.List.of("auction"));
            commands.register(builder.buildAdmin("ahadmin"), "Admin AH command", java.util.List.of());
        });

        // AHMenuListener — chest GUI clicks
        getServer().getPluginManager().registerEvents(new AHMenuListener(), this);

        // Periodically check for expired items
        Bukkit.getAsyncScheduler().runAtFixedRate(this, t -> AHManager.get().checkExpiry(), 1, 5,
                java.util.concurrent.TimeUnit.MINUTES);

        Bukkit.getLogger().info("NeonAH v" + getDescription().getVersion() + " (Fresh Clean Start) Loaded!");
    }

    @Override
    public void onDisable() {
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
