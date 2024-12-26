package com.meekdev.vachager.features.discord;

import com.meekdev.vachager.VachagerSMP;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Chunk;

import java.util.Random;

public class DragonEggListener implements Listener {
    private final VachagerSMP plugin;
    private final Random random = new Random();
    private Location lastKnownLocation;
    private String lastHolderName = null;
    private long lastAnnouncementTime = 0;
    private static final long ANNOUNCEMENT_COOLDOWN = 300000; // 5 minutes
    private boolean isEggInWorld = false;

    public DragonEggListener(VachagerSMP plugin) {
        this.plugin = plugin;
        startRandomAnnouncement();
        startPeriodicCheck();
        findInitialEggLocation();
    }

    private void findInitialEggLocation() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (org.bukkit.World world : plugin.getServer().getWorlds()) {
                    // Check loaded chunks for placed egg
                    for (Chunk chunk : world.getLoadedChunks()) {
                        for (int x = 0; x < 16; x++) {
                            for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                                for (int z = 0; z < 16; z++) {
                                    Block block = chunk.getBlock(x, y, z);
                                    if (block.getType() == Material.DRAGON_EGG) {
                                        lastKnownLocation = block.getLocation();
                                        isEggInWorld = true;
                                        return;
                                    }
                                }
                            }
                        }
                    }


                    for (Entity entity : world.getEntities()) {
                        if (entity instanceof Item item && item.getItemStack().getType() == Material.DRAGON_EGG) {
                            lastKnownLocation = item.getLocation();
                            isEggInWorld = true;
                            return;
                        }
                    }


                    for (Player player : world.getPlayers()) {
                        if (hasEggInInventory(player)) {
                            lastKnownLocation = player.getLocation();
                            lastHolderName = player.getName();
                            isEggInWorld = true;
                            return;
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 6000L); // Run every 5 minutes
    }

    private boolean hasEggInInventory(Player player) {

        if (player.getInventory().contains(Material.DRAGON_EGG)) return true;

        if (player.getItemOnCursor().getType() == Material.DRAGON_EGG) return true;

        return false;
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() == Material.DRAGON_EGG) {
            updateEggLocation(event.getBlock().getLocation(), event.getPlayer().getName(), "placed");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.DRAGON_EGG) {
            lastHolderName = event.getPlayer().getName();

        }
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        if (event.getEntity().getItemStack().getType() == Material.DRAGON_EGG) {
            updateEggLocation(event.getLocation(), lastHolderName, "spawned as item");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemDespawn(ItemDespawnEvent event) {
        if (event.getEntity().getItemStack().getType() == Material.DRAGON_EGG) {
            isEggInWorld = false;
            announceEggLocation(true, "despawned");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (event.getItem().getItemStack().getType() == Material.DRAGON_EGG) {
            if (event.getEntity() instanceof Player player) {
                updateEggLocation(player.getLocation(), player.getName(), "picked up");
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (event.getItemDrop().getItemStack().getType() == Material.DRAGON_EGG) {
            updateEggLocation(event.getItemDrop().getLocation(), event.getPlayer().getName(), "dropped");
        }
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;


        if (event.getInventory().getType() == InventoryType.ENDER_CHEST) {
            if (isEggInvolvedInClick(event)) {
                event.setCancelled(true);
                player.sendMessage(plugin.getMiniMessage().deserialize("<gradient:#FF0000:#FF6600>You cannot store the Dragon Egg in your enderchest!</gradient>"));
                return;
            }
        }


        if (isEggInvolvedInClick(event)) {
            updateEggLocation(player.getLocation(), player.getName(), "moved in inventory");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        if (event.getItem().getType() == Material.DRAGON_EGG) {
            updateEggLocation(event.getDestination().getLocation(), null, "moved between containers");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryPickupItem(InventoryPickupItemEvent event) {
        if (event.getItem().getItemStack().getType() == Material.DRAGON_EGG) {
            updateEggLocation(event.getInventory().getLocation(), null, "picked up by container");
        }
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        if (event.getBlock().getType() == Material.DRAGON_EGG) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    updateEggLocation(event.getBlock().getLocation(), lastHolderName, "physics update");
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        if (event.getBlock().getType() == Material.DRAGON_EGG) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    updateEggLocation(event.getToBlock().getLocation(), lastHolderName, "teleported");
                }
            }.runTaskLater(plugin, 1L);
        }
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (hasEggInInventory(event.getPlayer())) {
            lastHolderName = event.getPlayer().getName();

        }
    }


    private void startRandomAnnouncement() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (random.nextDouble() < 0.05) { // 5% chance
                    announceEggLocation(true, "random check");
                }
            }
        }.runTaskTimer(plugin, 72000L, 72000L); // Every hour
    }

    private void startPeriodicCheck() {
        new BukkitRunnable() {
            @Override
            public void run() {
                findInitialEggLocation();
            }
        }.runTaskTimer(plugin, 20L * 300, 20L * 300);
    }

    private boolean isEggInvolvedInClick(InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        return (clicked != null && clicked.getType() == Material.DRAGON_EGG) ||
                (cursor != null && cursor.getType() == Material.DRAGON_EGG);
    }

    private void updateEggLocation(Location newLocation, String holderName, String action) {
        lastKnownLocation = newLocation;
        lastHolderName = holderName;
        isEggInWorld = true;
        announceEggLocation(false, action);
    }

    private void announceEggLocation(boolean force, String action) {
        if (lastKnownLocation == null || (!force && !isEggInWorld)) return;

        long currentTime = System.currentTimeMillis();
        if (!force && currentTime - lastAnnouncementTime < ANNOUNCEMENT_COOLDOWN) {
            return;
        }
        lastAnnouncementTime = currentTime;


        StringBuilder message = new StringBuilder("**Dragon Egg Location Update**\n");

        if (isEggInWorld) {
            message.append("World: ").append(lastKnownLocation.getWorld().getName()).append("\n");
            message.append("X: ").append(lastKnownLocation.getBlockX()).append("\n");
            message.append("Y: ").append(lastKnownLocation.getBlockY()).append("\n");
            message.append("Z: ").append(lastKnownLocation.getBlockZ()).append("\n");

            if (lastHolderName != null) {
                message.append("Last interaction by: ").append(lastHolderName).append("\n");
            }

            message.append("Status: ").append(action);
        } else {
            message.append("Status: The Dragon Egg is currently missing!");
        }


        TextChannel channel = DiscordSRV.getPlugin().getMainTextChannel();
        if (channel != null) {
            channel.sendMessage(message.toString()).queue();
        }

        String inGameMessage = isEggInWorld ?
                "<gradient:#FFD700:#FFA500>The Dragon Egg's location has been revealed!</gradient>" :
                "<gradient:#FF0000:#FF6600>The Dragon Egg is missing!</gradient>";

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.sendMessage(plugin.getMiniMessage().deserialize(inGameMessage));
        }
    }
}