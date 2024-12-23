package com.meekdev.vachager.core.listeners;

import com.meekdev.vachager.VachagerSMP;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.Location;
import org.bukkit.World.Environment;


public class ListenerManager implements Listener {
    private final VachagerSMP plugin;

    public ListenerManager(VachagerSMP plugin) {
        this.plugin = plugin;
    }

    public void registerListeners() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Location respawnLoc = plugin.getRespawnManager().getRespawnLocation(event.getPlayer());
        if (respawnLoc != null) {
            event.setRespawnLocation(respawnLoc);
        }
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (event.getTo().getWorld().getEnvironment() == Environment.THE_END) {
            if (!plugin.getWorldManager().isEndEnabled()) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(
                        plugin.getMiniMessage().deserialize("<gradient:#FF0000:#FF6600>The End is currently disabled!</gradient>")
                );
            }
        }
    }

    @EventHandler
    public void onPortalCreate(PortalCreateEvent event) {
        if (event.getWorld().getEnvironment() == Environment.THE_END) {
            if (!plugin.getWorldManager().isEndEnabled()) {
                event.setCancelled(true);
            }
        }
    }
}