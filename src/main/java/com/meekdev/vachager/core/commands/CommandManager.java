package com.meekdev.vachager.core.commands;

import com.meekdev.vachager.VachagerSMP;
import com.meekdev.vachager.features.blocks.ChairSystem;

public class CommandManager {
    private final VachagerSMP plugin;

    public CommandManager(VachagerSMP plugin) {
        this.plugin = plugin;
    }

    public void registerCommands() {
        new EndCommand(plugin);
        new SpawnCommand(plugin);
        new ChairSystem(plugin);
    }
}