package com.meekdev.vachager.features.worlds;

import com.meekdev.vachager.VachagerSMP;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.World.Environment;

import java.util.HashMap;
import java.util.Map;

public class WorldManager {
    private final VachagerSMP plugin;
    private final Map<String, Boolean> worldStates;

    public WorldManager(VachagerSMP plugin) {
        this.plugin = plugin;
        this.worldStates = new HashMap<>();
        loadWorldStates();
    }

    public boolean isEndEnabled() {
        return worldStates.getOrDefault("end", true);
    }

    public void setEndEnabled(boolean enabled) {
        worldStates.put("end", enabled);
        saveWorldStates();

        if (!enabled) {
            World end = plugin.getServer().getWorld("world_the_end");
            if (end != null) {
                end.getPlayers().forEach(player ->
                        player.teleport(plugin.getServer().getWorlds().get(0).getSpawnLocation())
                );
            }
        }
    }

    private void loadWorldStates() {
        var config = plugin.getConfigManager().getConfig("config.yml");
        worldStates.put("end", config.getBoolean("worlds.end.enabled", true));
    }

    private void saveWorldStates() {
        var config = plugin.getConfigManager().getConfig("config.yml");
        config.set("worlds.end.enabled", worldStates.get("end"));
        plugin.getConfigManager().saveConfig("config.yml");
    }

    public void shutdown() {
        saveWorldStates();
    }
}