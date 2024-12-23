package com.meekdev.vachager.core.config;

import com.meekdev.vachager.VachagerSMP;
import org.bukkit.configuration.file.FileConfiguration;

public class MessagesConfig {
    private final VachagerSMP plugin;
    private FileConfiguration config;

    public MessagesConfig(VachagerSMP plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    public void loadMessages() {
        config = plugin.getConfigManager().getConfig("messages.yml");

        setDefaultMessage("prefix", "<gradient:#FFB302:#FF7302>[VachagerSMP]</gradient>");
        setDefaultMessage("spawn.set", "<prefix> Spawn point set for <duration> minutes!");
        setDefaultMessage("spawn.invalid", "<prefix> You must be standing on a Lodestone!");
        setDefaultMessage("spawn.no-diamonds", "<prefix> You need diamonds to set a spawn point!");
        setDefaultMessage("end.disabled", "<prefix> The End is currently disabled!");
        setDefaultMessage("end.enabled", "<prefix> The End has been enabled!");
        setDefaultMessage("end.toggled", "<prefix> End access has been <state>!");
        setDefaultMessage("error.no-permission", "<prefix> You don't have permission to do this!");

        plugin.getConfigManager().saveConfig("messages.yml");
    }

    private void setDefaultMessage(String path, String defaultValue) {
        if (!config.isSet(path)) {
            config.set(path, defaultValue);
        }
    }

    public String getMessage(String path) {
        String message = config.getString(path);
        if (message == null) {
            return "Missing message: " + path;
        }
        return message.replace("<prefix>", config.getString("prefix", ""));
    }
}

// NOT USED