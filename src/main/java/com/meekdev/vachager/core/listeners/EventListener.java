package com.meekdev.vachager.core.listeners;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.meekdev.vachager.VachagerSMP;
import com.meekdev.vachager.features.qol.XPBottleEvent;
import com.meekdev.vachager.utils.LegacyRandomManager;
import com.meekdev.vachager.utils.MessageUtils;
import org.bukkit.Material;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.PortalCreateEvent;

public class EventListener implements Listener {
    private final VachagerSMP plugin;

    public EventListener(VachagerSMP plugin) {
        this.plugin = plugin;
    }
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        plugin.getRespawnManager().handlePlayerRespawn(event);
    }

    @EventHandler
    public void onEntityAddToWorld(EntityAddToWorldEvent event) {
        if (event.getEntity().getType() == EntityType.SQUID) {
            LegacyRandomManager.updateEntityRandom(event.getEntity(), (long) event.getEntity().getEntityId());
        }
    }

    @EventHandler
    public void onEntityRemove(EntityRemoveFromWorldEvent event) {
        LegacyRandomManager.cleanup(event.getEntity());
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (event.getTo().getWorld().getEnvironment() == Environment.THE_END) {
            if (!plugin.getWorldManager().isEndEnabled()) {
                event.setCancelled(true);
                MessageUtils.sendMessage(event.getPlayer(), "L'End est actuellement désactivé !", "#ffce47");
            }
        }
    }

    @EventHandler
    public void onEnchantTableInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.ENCHANTING_TABLE) return;

        if (event.getItem() != null && event.getItem().getType() == Material.GLASS_BOTTLE) {
            plugin.getServer().getPluginManager().callEvent(new XPBottleEvent(event.getPlayer(), event.getItem()));
            event.setCancelled(true);
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