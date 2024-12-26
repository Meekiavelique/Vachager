package com.meekdev.vachager.features.discord;

import com.meekdev.vachager.VachagerSMP;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class DragonEggListener implements Listener {
    private VachagerSMP plugin = new VachagerSMP();
    private final Random random = new Random();

    public DragonEggListener() {
        startLocationBroadcaster();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        if (event.getInventory().getType() == InventoryType.ENDER_CHEST) {
            ItemStack item = event.getCurrentItem();

            if (item != null && item.getType() == Material.DRAGON_EGG) {
                event.setCancelled(true);
                Player player = (Player) event.getWhoClicked();
                player.sendMessage(plugin.getMiniMessage().deserialize("<color:#ffce47>Vous ne pouvez pas stocker l'Œuf de Dragon dans votre Enderchest !</color>"));
            }
        }
    }

    private void startLocationBroadcaster() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Random chance (5% every hour)
                if (random.nextDouble() < 0.05) {
                    broadcastDragonEggLocation();
                }
            }
        }.runTaskTimer(plugin, 72000L, 72000L); // Run every hour (20 ticks * 60 seconds * 60 minutes)
    }

    private void broadcastDragonEggLocation() {
        // Find the dragon egg in the world
        for (World world : plugin.getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Item) {
                    Item item = (Item) entity;
                    if (item.getItemStack().getType() == Material.DRAGON_EGG) {
                        // Get the location
                        int x = item.getLocation().getBlockX();
                        int y = item.getLocation().getBlockY();
                        int z = item.getLocation().getBlockZ();
                        String worldName = world.getName();

                        // Create the message
                        String message = "**Dragon Egg Location**\n" +
                                "World: " + worldName + "\n" +
                                "X: " + x + "\n" +
                                "Y: " + y + "\n" +
                                "Z: " + z;

                        // Send to Discord
                        TextChannel channel = DiscordSRV.getPlugin().getMainTextChannel();
                        if (channel != null) {
                            channel.sendMessage(message).queue();
                        }

                        // Broadcast to all players
                        plugin.getServer().broadcastMessage(
                                plugin.getMiniMessage().deserialize("<color:#ffce47>La localisation de l'Œuf de Dragon a été révélée !</color>")
                                        .toString()
                        );
                        return;
                    }
                }
            }
        }
    }
}