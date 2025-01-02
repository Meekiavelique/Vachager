package com.meekdev.vachager;

import com.meekdev.vachager.core.chat.ChatManager;
import com.meekdev.vachager.core.commands.CommandManager;
import com.meekdev.vachager.core.config.ConfigManager;
import com.meekdev.vachager.core.listeners.ListenerManager;
import com.meekdev.vachager.features.respawn.LodestoneManager;
import com.meekdev.vachager.features.respawn.RespawnManager;
import com.meekdev.vachager.features.worlds.EndEventListener;
import com.meekdev.vachager.features.worlds.EndEventManager;
import com.meekdev.vachager.features.worlds.WorldManager;
import com.meekdev.vachager.utils.LegacyRandomManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class VachagerSMP extends JavaPlugin {
    private ConfigManager configManager;
    private EndEventManager endEventManager;
    private RespawnManager respawnManager;
    private WorldManager worldManager;
    private MiniMessage miniMessage;
    private ChatManager chatManager;
    private ListenerManager listenerManager;
    private CommandManager commandManager;
    private LodestoneManager lodestoneManager;

    @Override
    public void onEnable() {
        try {
            // Initialize MiniMessage first as it's required by many components
            this.miniMessage = MiniMessage.miniMessage();

            if (this.miniMessage == null) {
                throw new IllegalStateException("Failed to initialize MiniMessage");
            }

            // Create data folder if it doesn't exist
            if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
                throw new IllegalStateException("Failed to create plugin directory");
            }

            // Load configuration first as other components depend on it
            this.configManager = new ConfigManager(this);
            this.configManager.loadAll();

            // Verify essential config values exist
            this.lodestoneManager = new LodestoneManager(this);
            this.endEventManager = new EndEventManager(this);
            getServer().getPluginManager().registerEvents(
                    new EndEventListener(this, endEventManager),
                    this
            );

            // Initialize core managers in dependency order
            initializeManagers();

            // Register all events and commands
            registerHandlers();

            // Start any scheduled tasks
            startTasks();

            getLogger().info("Vachager has been enabled successfully!");
        } catch (Exception e) {
            getLogger().severe("Failed to enable Vachager: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void initializeManagers() {
        // World manager first as it's needed for respawn
        this.worldManager = new WorldManager(this);

        // Respawn manager depends on world manager
        this.respawnManager = new RespawnManager(this);
        if (this.respawnManager == null) {
            throw new IllegalStateException("Failed to initialize RespawnManager");
        }

        // Initialize optional integrations
        try {
            this.chatManager = new ChatManager(this);
        } catch (Exception e) {
            getLogger().warning("Chat features disabled: " + e.getMessage());
        }
    }

    private void registerHandlers() {
        // Initialize listener manager first
        if (this.listenerManager == null) {
            this.listenerManager = new ListenerManager(this);
        }

        // Register all listeners
        try {
            this.listenerManager.registerListeners();
        } catch (Exception e) {
            getLogger().severe("Failed to register listeners: " + e.getMessage());
            throw e;
        }

        // Initialize and register commands
        this.commandManager = new CommandManager(this);
        try {
            this.commandManager.registerCommands();
        } catch (Exception e) {
            getLogger().severe("Failed to register commands: " + e.getMessage());
            throw e;
        }
    }

    private void startTasks() {
        // Register any cleanup tasks
        getServer().getScheduler().runTaskTimer(this, () -> {
            try {
                if (respawnManager != null) {
                    // Cleanup expired respawn points
                }
            } catch (Exception e) {
                getLogger().warning("Error in cleanup task: " + e.getMessage());
            }
        }, 20L * 60L, 20L * 60L); // Run every minute
    }

    @Override
    public void onDisable() {
        try {
            // Save all configuration data
            if (configManager != null) {
                configManager.saveAll();
            }

            // Cleanup managers in reverse order
            if (listenerManager != null) {
                listenerManager.unregisterListeners();
            }

            // Cancel all tasks
            getServer().getScheduler().cancelTasks(this);

            getLogger().info("Vachager has been disabled successfully!");
            LegacyRandomManager.cleanupAll();
        } catch (Exception e) {
            getLogger().severe("Error during plugin disable: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Getters for the managers
    public ConfigManager getConfigManager() { return configManager; }
    public RespawnManager getRespawnManager() { return respawnManager; }
    public WorldManager getWorldManager() { return worldManager; }
    public MiniMessage getMiniMessage() { return miniMessage; }
    public ChatManager getChatManager() { return chatManager; }

    public @NotNull NamespacedKey getKey(String chair) {
        return new NamespacedKey(this, chair);
    }

    public LodestoneManager getLodestoneManager() {
        return lodestoneManager;
    }

    public EndEventManager getEndEventManager() {
        return endEventManager;
    }
}
