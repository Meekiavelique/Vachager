package com.meekdev.vachager.features.blocks;

import com.meekdev.vachager.VachagerSMP;
import com.meekdev.vachager.utils.MessageUtils;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class DragonEggListener implements Listener {
    private final VachagerSMP plugin;


    private final AtomicReference<Location> lastKnownLocation = new AtomicReference<>();
    public final AtomicReference<String> lastHolderName = new AtomicReference<>();
    private volatile long lastAnnouncementTime = 0;
    private static final long ANNOUNCEMENT_COOLDOWN = 300000;
    private volatile boolean isEggInWorld = false;
    private final Set<BukkitTask> activeTasks = new HashSet<>();

    public DragonEggListener(VachagerSMP plugin) {
        this.plugin = plugin;

        startMaintenanceTask();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clicked = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        boolean isDragonEggInvolved = (clicked != null && clicked.getType() == Material.DRAGON_EGG) ||
                (cursor != null && cursor.getType() == Material.DRAGON_EGG);

        if (isDragonEggInvolved) {
            if (event.getInventory().getType() == InventoryType.ENDER_CHEST) {
                event.setCancelled(true);
                MessageUtils.sendMessage(player, "Vous ne pouvez pas stocker l'oeuf du dragon dans votre enderchest!", "#FF4D2E");
                return;
            }

            updateEggLocation(player.getLocation(), player.getName(), "moved in inventory");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (event.getItem().getType() == Material.DRAGON_EGG) {
            if (event.getDestination().getType() == InventoryType.ENDER_CHEST) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryPickupItem(InventoryPickupItemEvent event) {
        if (event.getItem().getItemStack().getType() == Material.DRAGON_EGG) {
            if (event.getInventory().getType() == InventoryType.ENDER_CHEST) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack dragged = event.getOldCursor();
        if (dragged != null && dragged.getType() == Material.DRAGON_EGG) {
            if (event.getInventory().getType() == InventoryType.ENDER_CHEST) {
                event.setCancelled(true);
                MessageUtils.sendMessage(player, "Vous ne pouvez pas stocker l'oeuf du dragon dans votre enderchest!", "#FF4D2E");
            }
        }
    }

    private void startMaintenanceTask() {
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {

            if (!isEggInWorld) {
                plugin.getServer().getScheduler().runTask(plugin, this::scanForEgg);
            }


            if (Math.random() < 0.05) {
                announceEggLocation(true, "random check");
            }
        }, 20L * 60, 20L * 300);

        activeTasks.add(task);
    }

    private void scanForEgg() {
        for (World world : plugin.getServer().getWorlds()) {

            for (Entity entity : world.getEntities()) {
                if (entity instanceof Item item && item.getItemStack().getType() == Material.DRAGON_EGG) {
                    updateEggLocation(item.getLocation(), null, "found as item");
                    return;
                }
            }

            for (Player player : world.getPlayers()) {
                if (player.getInventory().contains(Material.DRAGON_EGG)) {
                    updateEggLocation(player.getLocation(), player.getName(), "found in inventory");
                    return;
                }
            }
        }
    }



    public void updateEggLocation(Location newLocation, String holderName, String action) {
        lastKnownLocation.set(newLocation);
        lastHolderName.set(holderName);
        isEggInWorld = true;
        announceEggLocation(false, action);
    }

    private void announceEggLocation(boolean force, String action) {
        Location location = lastKnownLocation.get();
        if (location == null || (!force && !isEggInWorld)) return;

        long currentTime = System.currentTimeMillis();
        if (!force && currentTime - lastAnnouncementTime < ANNOUNCEMENT_COOLDOWN) {
            return;
        }
        lastAnnouncementTime = currentTime;

        StringBuilder message = new StringBuilder(256)
                .append("**Dragon Egg Location Update**\n");

        if (isEggInWorld) {
            message.append("World: ").append(location.getWorld().getName())
                    .append("\nX: ").append(location.getBlockX())
                    .append("\nY: ").append(location.getBlockY())
                    .append("\nZ: ").append(location.getBlockZ());

            String holder = lastHolderName.get();
            if (holder != null) {
                message.append("\nLast interaction by: ").append(holder);
            }

            message.append("\nStatus: ").append(action);
        } else {
            message.append("Status: The Dragon Egg is currently missing!");
        }

        TextChannel channel = DiscordSRV.getPlugin().getMainTextChannel();
        if (channel != null) {
            if (plugin.getServer().getPluginManager().getPlugin("DiscordSRV") != null) {
                try {

                    if (channel != null) {
                        channel.sendMessage(message.toString()).queue();
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to send message to Discord: " + e.getMessage());
                }
            }
        }

        String inGameMessage = isEggInWorld ?
                "<gradient:#FFD700:#FFA500>The Dragon Egg's location has been revealed!</gradient>" :
                "<gradient:#FF0000:#FF6600>The Dragon Egg is missing!</gradient>";

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                player.sendMessage(plugin.getMiniMessage().deserialize(inGameMessage));
            }
        });
    }

    public void shutdown() {
        activeTasks.forEach(BukkitTask::cancel);
        activeTasks.clear();
    }
}