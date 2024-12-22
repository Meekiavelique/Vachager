package com.meekdev.vachager;

import com.meekdev.vachager.chat.ChatManager;
import com.meekdev.vachager.commands.CommandsManager;
import com.meekdev.vachager.events.EventsManager;
import com.meekdev.vachager.performance.ChunkManager;
import org.bukkit.plugin.java.JavaPlugin;

public class VachagerPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();

        ChunkManager chunkManager = new ChunkManager(this);
        ChatManager chatManager = new ChatManager(this);
        EventsManager eventsManager = new EventsManager(this);
        CommandsManager commandsManager = new CommandsManager(this);

        eventsManager.registerEvents();
        commandsManager.registerCommands();

        getLogger().info("Vachager Plugin enabled successfully!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Vachager Plugin disabled successfully!");
    }
}