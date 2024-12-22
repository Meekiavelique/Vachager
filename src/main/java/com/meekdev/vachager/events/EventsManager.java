package com.meekdev.vachager.events;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

public class EventsManager {

    private final Plugin plugin;

    public EventsManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void registerEvents() {
        PluginManager pluginManager = plugin.getServer().getPluginManager();
        pluginManager.registerEvents(new ChargedCreeperOnRain(plugin), plugin);
    }
}