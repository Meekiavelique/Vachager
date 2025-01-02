package com.meekdev.vachager.core.commands;

import com.meekdev.vachager.VachagerSMP;
import com.meekdev.vachager.utils.MessageUtils;
import com.meekdev.vachager.features.worlds.EndEventManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class EndCommand implements CommandExecutor, TabCompleter {
    private final VachagerSMP plugin;
    private final EndEventManager endManager;
    private static final String COLOR_ERROR = "#ff4d2e";
    private static final String COLOR_SUCCESS = "#a7ff78";

    public EndCommand(VachagerSMP plugin) {
        this.plugin = plugin;
        this.endManager = plugin.getEndEventManager();


        plugin.getCommand("toggleend").setExecutor(this);
        plugin.getCommand("toggleend").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return true;
        }

        if (!player.hasPermission("vachager.end.toggle")) {
            MessageUtils.sendMessage(player, "Vous n'avez pas la permission !", COLOR_ERROR);
            return true;
        }

        boolean success = endManager.toggleEndManually(player);

        if (!success) {
            MessageUtils.sendMessage(player, "Impossible de toggle l'End pendant un event !", COLOR_ERROR);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }
}