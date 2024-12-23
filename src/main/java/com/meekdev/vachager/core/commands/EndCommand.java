package com.meekdev.vachager.core.commands;

import com.meekdev.vachager.VachagerSMP;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class EndCommand implements CommandExecutor, TabCompleter {
    private final VachagerSMP plugin;

    public EndCommand(VachagerSMP plugin) {
        this.plugin = plugin;
        plugin.getCommand("toggleend").setExecutor(this);
        plugin.getCommand("toggleend").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("vachager.end.toggle")) {
            sender.sendMessage(plugin.getMiniMessage().deserialize("<gradient:#FF0000:#FF6600>You don't have permission!</gradient>"));
            return true;
        }

        boolean newState = !plugin.getWorldManager().isEndEnabled();
        plugin.getWorldManager().setEndEnabled(newState);

        String stateMsg = newState ? "<gradient:#00FF00:#00FFAA>enabled</gradient>" : "<gradient:#FF0000:#FF6600>disabled</gradient>";
        sender.sendMessage(plugin.getMiniMessage().deserialize("<gradient:#FFB302:#FF7302>The End has been " + stateMsg));

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }
}