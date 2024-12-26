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
        // Si le dossier du plugin n'existe pas, le créer
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }

        createConfig("config.yml");
        createConfig("messages.yml");
        createConfig("player_data.yml");
    }

    private void createConfig(String fileName) {
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
}