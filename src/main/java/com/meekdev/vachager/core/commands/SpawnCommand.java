package com.meekdev.vachager.core.commands;

import com.meekdev.vachager.VachagerSMP;
import com.meekdev.vachager.utils.MessageUtils;
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
    private static final String COLOR_ERROR = "#ff4d2e";
    private static final String COLOR_SUCCESS = "#a7ff78";

    public SpawnCommand(VachagerSMP plugin) {
        this.plugin = plugin;
        plugin.getCommand("spawn").setExecutor(this);
        plugin.getCommand("spawn").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtils.sendMessage((Player) sender, "Seuls les joueurs peuvent utiliser cette commande !", COLOR_ERROR);
            return true;
        }

        Location spawnLocation = player.getWorld().getSpawnLocation();
        player.teleport(spawnLocation);
        MessageUtils.sendMessage(player, "Vous êtes maintenant au spawn !", COLOR_SUCCESS);

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }
}