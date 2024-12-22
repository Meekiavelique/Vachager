package com.meekdev.vachager.commands;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class CommandsManager {

    private final JavaPlugin plugin;

    public CommandsManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerCommands() {
        Objects.requireNonNull(plugin.getCommand("home")).setExecutor(new Home(plugin));
        Objects.requireNonNull(plugin.getCommand("spawn")).setExecutor(new Spawn());
    }
}