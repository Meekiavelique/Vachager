package com.meekdev.vachager.commands;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class Home implements CommandExecutor {

    private final JavaPlugin plugin;

    public Home(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        FileConfiguration config = plugin.getConfig();
        UUID playerUUID = player.getUniqueId();

        if (args.length == 0) {
            teleportHome(player, config, playerUUID);
        } else if (args.length == 1) {
            switch (args[0].toLowerCase()) {
                case "set":
                    setHome(player, config, playerUUID);
                    break;
                case "remove":
                    removeHome(player, config, playerUUID);
                    break;
                default:
                    player.sendMessage("Usage: /home, /home set, or /home remove");
            }
        } else {
            player.sendMessage("Usage: /home, /home set, or /home remove");
        }

        return true;
    }

    private void teleportHome(Player player, FileConfiguration config, UUID playerUUID) {
        if (config.contains("homes." + playerUUID)) {
            Location home = new Location(
                    player.getWorld(),
                    config.getDouble("homes." + playerUUID + ".x"),
                    config.getDouble("homes." + playerUUID + ".y"),
                    config.getDouble("homes." + playerUUID + ".z"),
                    (float) config.getDouble("homes." + playerUUID + ".yaw"),
                    (float) config.getDouble("homes." + playerUUID + ".pitch")
            );
            player.teleport(home);
            player.sendMessage("Teleported to your home.");
        } else {
            player.sendMessage("You have not set a home yet.");
        }
    }

    private void setHome(Player player, FileConfiguration config, UUID playerUUID) {
        if (config.contains("homes." + playerUUID)) {
            player.sendMessage("You already have a home set. Please remove it before setting a new one.");
        } else {
            Location loc = player.getLocation();
            config.set("homes." + playerUUID + ".x", loc.getX());
            config.set("homes." + playerUUID + ".y", loc.getY());
            config.set("homes." + playerUUID + ".z", loc.getZ());
            config.set("homes." + playerUUID + ".yaw", loc.getYaw());
            config.set("homes." + playerUUID + ".pitch", loc.getPitch());
            plugin.saveConfig();
            player.sendMessage("Home set.");
        }
    }

    private void removeHome(Player player, FileConfiguration config, UUID playerUUID) {
        if (config.contains("homes." + playerUUID)) {
            config.set("homes." + playerUUID, null);
            plugin.saveConfig();
            player.sendMessage("Home removed.");
        } else {
            player.sendMessage("You do not have a home set.");
        }
    }
}