package com.meekdev.vachager.core.commands;

import com.meekdev.vachager.VachagerSMP;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public class SpawnCommand implements CommandExecutor, TabCompleter {
    private final VachagerSMP plugin;

    public SpawnCommand(VachagerSMP plugin) {
        this.plugin = plugin;
        plugin.getCommand("setspawn").setExecutor(this);
        plugin.getCommand("setspawn").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMiniMessage().deserialize("<gradient:#FF0000:#FF6600>Only players can use this command!</gradient>"));
            return true;
        }

        Location loc = player.getLocation();
        if (loc.getBlock().getType() != Material.LODESTONE) {
            player.sendMessage(plugin.getMiniMessage().deserialize("<gradient:#FF0000:#FF6600>You must be standing on a Lodestone!</gradient>"));
            return true;
        }

        plugin.getRespawnManager().setRespawnPoint(player, loc);
        player.sendMessage(plugin.getMiniMessage().deserialize("<gradient:#00FF00:#00FFAA>Spawn point set successfully!</gradient>"));

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }
}