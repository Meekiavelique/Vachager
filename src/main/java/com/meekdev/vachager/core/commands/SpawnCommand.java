package com.meekdev.vachager.core.commands;

import com.meekdev.vachager.VachagerSMP;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class SpawnCommand implements CommandExecutor, TabCompleter {
    private final VachagerSMP plugin;

    public SpawnCommand(VachagerSMP plugin) {
        this.plugin = plugin;
        plugin.getCommand("spawn").setExecutor(this);
        plugin.getCommand("spawn").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMiniMessage().deserialize("<gradient:#FF0000:#FF6600>Only players can use this command!</gradient>"));
            return true;
        }

        Location spawnLocation = player.getWorld().getSpawnLocation();
        player.teleport(spawnLocation);
        player.sendMessage(plugin.getMiniMessage().deserialize("<color:#ffce47>Vous êtes maintenant au spawn !</color>"));

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }
}