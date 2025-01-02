package com.meekdev.vachager.features.worlds;

import com.meekdev.vachager.VachagerSMP;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

public class EndEventListener implements Listener {
    private final VachagerSMP plugin;
    private final EndEventManager manager;

    public EndEventListener(VachagerSMP plugin, EndEventManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (event.getCause() != TeleportCause.END_PORTAL) {
            return;
        }

        Player player = event.getPlayer();

        if (!manager.isEndEnabled()) {
            event.setCancelled(true);
            manager.notifyEndDisabled(player);
            return;
        }

        World endWorld = plugin.getServer().getWorld("world_the_end");
        if (endWorld == null) {
            return;
        }

        Location safeLocation = manager.getRandomSafeLocation(endWorld);
        if (safeLocation == null) {
            plugin.getLogger().info("§e[End] Using vanilla spawn for " + player.getName());
            return;
        }

        event.setTo(safeLocation);
        manager.notifyEndTeleport(player);
        plugin.getLogger().info("§a[End] Custom teleport for " + player.getName() + " to " +
                String.format("(%.1f, %.1f, %.1f)",
                        safeLocation.getX(),
                        safeLocation.getY(),
                        safeLocation.getZ()));
    }
}