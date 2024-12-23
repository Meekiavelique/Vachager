package com.meekdev.vachager;

import com.meekdev.vachager.chat.ChatManager;
import com.meekdev.vachager.core.config.ConfigManager;
import com.meekdev.vachager.core.config.MessagesConfig;
import com.meekdev.vachager.features.blocks.ChairSystem;
import com.meekdev.vachager.features.blocks.PistonListener;
import com.meekdev.vachager.features.experience.XPBottleListener;
import com.meekdev.vachager.features.respawn.RespawnManager;
import com.meekdev.vachager.core.commands.home.HomeCommand;
import com.meekdev.vachager.features.mobs.BatDropListener;
import com.meekdev.vachager.features.worlds.WorldManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class VachagerSMP extends JavaPlugin {
    private ConfigManager configManager;
    private MessagesConfig messagesConfig;
    private RespawnManager respawnManager;
    private WorldManager worldManager;
    private MiniMessage miniMessage;
    private ChatManager chatManager;
    private ChairSystem chairSystem;

    @Override
    public void onEnable() {
        miniMessage = MiniMessage.miniMessage();

        initializeConfigs();
        initializeManagers();
        registerCommands();
        registerListeners();

        getLogger().info("VachagerSMP has been enabled!");
    }

    private void initializeConfigs() {
        configManager = new ConfigManager(this);
        configManager.loadAll();
        messagesConfig = new MessagesConfig(this);
    }

    private void initializeManagers() {
        worldManager = new WorldManager(this);
    }

    private void registerCommands() {
        new HomeCommand(this);
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();

        chairSystem = new ChairSystem(this);
        chatManager = new ChatManager(this);

        pm.registerEvents(new PistonListener(), this);
        pm.registerEvents(new XPBottleListener(), this);
        pm.registerEvents(new BatDropListener(), this);
    }

    @Override
    public void onDisable() {
        if (chairSystem != null) {
            chairSystem.disableChairs();
        }
        if (worldManager != null) {
            worldManager.shutdown();
        }
        configManager.saveAll();
        getLogger().info("VachagerSMP has been disabled!");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public WorldManager getWorldManager() {
        return worldManager;
    }

    public MessagesConfig getMessagesConfig() {
        return messagesConfig;
    }

    public MiniMessage getMiniMessage() {
        return miniMessage;
    }

    public ChatManager getChatManager() {
        return chatManager;
    }

    public RespawnManager getRespawnManager() {
        return respawnManager;
    }
}