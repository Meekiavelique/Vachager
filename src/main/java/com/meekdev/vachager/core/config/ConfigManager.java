package com.meekdev.vachager.core.config;

import com.meekdev.vachager.VachagerSMP;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private final VachagerSMP plugin;
    private final Map<String, FileConfiguration> configs;
    private final Map<String, File> configFiles;

    public ConfigManager(VachagerSMP plugin) {
        this.plugin = plugin;
        this.configs = new HashMap<>();
        this.configFiles = new HashMap<>();
        createDefaultConfigs();
    }

    private void createDefaultConfigs() {

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }

        createConfig("config.yml");
        createConfig("messages.yml");
        createConfig("player_data.yml");
    }

    public void createConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);

        if (!file.exists()) {
            try {
                file.createNewFile();
                if (plugin.getResource(fileName) != null) {
                    plugin.saveResource(fileName, false);
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create " + fileName + ": " + e.getMessage());
            }
        }

        configFiles.put(fileName, file);
        configs.put(fileName, YamlConfiguration.loadConfiguration(file));
    }

    public void loadAll() {
        configs.clear();
        createDefaultConfigs();
    }

    public void saveAll() {
        for (Map.Entry<String, FileConfiguration> entry : configs.entrySet()) {
            try {
                entry.getValue().save(configFiles.get(entry.getKey()));
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save config: " + entry.getKey());
            }
        }
    }

    public FileConfiguration getConfig(String name) {
        return configs.get(name);
    }

    public void saveConfig(String name) {
        try {
            configs.get(name).save(configFiles.get(name));
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save config: " + name);
        }
    }

    public void reloadConfig(String name) {
        configs.put(name, YamlConfiguration.loadConfiguration(configFiles.get(name)));
    }

    public void reloadAllConfigs() {
        for (String fileName : configFiles.keySet()) {
            reloadConfig(fileName);
        }
    }


    public static class MessagesConfig {
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
                plugin.getLogger().warning("Missing config value for " + path + ". Using default: " + defaultValue);
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
}