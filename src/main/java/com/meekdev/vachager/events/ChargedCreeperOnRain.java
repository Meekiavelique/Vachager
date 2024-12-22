package com.meekdev.vachager.events;

import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Creeper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.plugin.Plugin;

public class ChargedCreeperOnRain implements Listener {

    public ChargedCreeperOnRain(Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        if (event.toWeatherState()) {
            Bukkit.getWorlds().forEach(world ->
                    world.getEntitiesByClass(Creeper.class).forEach(creeper -> {
                        if (canChargeCreeper(creeper, world)) {
                            creeper.setPowered(true);
                        }
                    })
            );
        }
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getEntity() instanceof Creeper creeper) {
            World world = creeper.getWorld();
            if (canChargeCreeper(creeper, world)) {
                creeper.setPowered(true);
            }
        }
    }

    @EventHandler
    public void onEntityMove(EntityMoveEvent event) {
        if (event.getEntity() instanceof Creeper creeper) {
            World world = creeper.getWorld();
            if (canChargeCreeper(creeper, world)) {
                creeper.setPowered(true);
            }
        }
    }

    public boolean canChargeCreeper(Creeper creeper, World world) {
        return !creeper.isPowered() && (world.hasStorm() || world.isThundering()) && world.getHighestBlockYAt(creeper.getLocation()) <= creeper.getLocation().getY();
    }
}