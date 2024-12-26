package com.meekdev.vachager.features.respawn;

import com.meekdev.vachager.VachagerSMP;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class LodestoneListener implements Listener {
    private final VachagerSMP plugin;
    private final Map<Location, Long> activeLodestones;
    private final Map<UUID, Long> playerCooldowns;
    private final NamespacedKey respawnKey;

    // Configuration settings
    private final int minutesPerDiamond;
    private final int maxMinutes;
    private final int cooldownSeconds;
    private final double minY;
    private final double maxY;
    private final boolean requireAirAbove;
    private final boolean playEffects;

    // Message constants
    private static final String MSG_COOLDOWN = "<gradient:#FF0000:#FF6600>Attendez encore %d secondes avant de réessayer!</gradient>";
    private static final String MSG_INVALID_LOCATION = "<gradient:#FF0000:#FF6600>Position invalide! Le point de réapparition doit être entre Y=%d et Y=%d avec de l'air au-dessus.</gradient>";
    private static final String MSG_HOLD_DIAMONDS = "<gradient:#FF0000:#FF6600>Tenez des diamants pour définir votre point de réapparition!</gradient>";
    private static final String MSG_MAX_TIME = "<gradient:#FF0000:#FF6600>Le temps maximum de réapparition est de %d minutes!</gradient>";
    private static final String MSG_SUCCESS = "<gradient:#00FF00:#00FFAA>Point de réapparition défini pour %d minutes avec %d diamants!</gradient>";
    private static final String MSG_EXPIRED = "<gradient:#FF6600:#FFAA00>Ce point de réapparition a expiré!</gradient>";
    private static final String MSG_ALREADY_ACTIVE = "<gradient:#FF6600:#FFAA00>Ce point de réapparition est déjà actif pour %d minutes!</gradient>";

    public LodestoneListener(VachagerSMP plugin) {
        this.plugin = plugin;
        this.activeLodestones = new ConcurrentHashMap<>();
        this.playerCooldowns = new ConcurrentHashMap<>();
        this.respawnKey = new NamespacedKey(plugin, "respawn_lodestone");

        // Load configuration
        FileConfiguration config = plugin.getConfig();
        this.minutesPerDiamond = config.getInt("respawn.minutes-per-diamond", 5);
        this.maxMinutes = config.getInt("respawn.max-minutes", 120);
        this.cooldownSeconds = config.getInt("respawn.cooldown-seconds", 30);
        this.minY = config.getDouble("respawn.min-y", -64);
        this.maxY = config.getDouble("respawn.max-y", 320);
        this.requireAirAbove = config.getBoolean("respawn.require-air-above", true);
        this.playEffects = config.getBoolean("respawn.play-effects", true);

        // Register events
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Start cleanup task
        startCleanupTask();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLodestoneInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK ||
                event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || clickedBlock.getType() != Material.LODESTONE) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();

        // Check cooldown
        if (isOnCooldown(player)) {
            long remainingSeconds = getCooldownRemaining(player);
            sendCooldownMessage(player, remainingSeconds);
            return;
        }

        // Validate location
        Location loc = clickedBlock.getLocation();
        if (!isValidLocation(loc)) {
            player.sendMessage(plugin.getMiniMessage().deserialize(
                    String.format(MSG_INVALID_LOCATION, (int)minY, (int)maxY)
            ));
            return;
        }

        // Check if lodestone is already active
        if (isLodestoneActive(loc)) {
            long remainingMinutes = TimeUnit.MILLISECONDS.toMinutes(
                    activeLodestones.get(loc) - System.currentTimeMillis()
            );
            player.sendMessage(plugin.getMiniMessage().deserialize(
                    String.format(MSG_ALREADY_ACTIVE, remainingMinutes)
            ));
            return;
        }

        // Check held item
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem.getType() != Material.DIAMOND) {
            player.sendMessage(plugin.getMiniMessage().deserialize(MSG_HOLD_DIAMONDS));
            return;
        }

        // Calculate time
        int diamonds = Math.min(heldItem.getAmount(), maxMinutes / minutesPerDiamond);
        int minutes = diamonds * minutesPerDiamond;

        if (minutes > maxMinutes) {
            player.sendMessage(plugin.getMiniMessage().deserialize(
                    String.format(MSG_MAX_TIME, maxMinutes)
            ));
            return;
        }


        setRespawnPoint(player, clickedBlock, diamonds, minutes);


        setPlayerCooldown(player);


        if (playEffects) {
            playRespawnEffects(loc, player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLodestoneBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.LODESTONE) {
            return;
        }

        Location loc = block.getLocation();
        if (isLodestoneActive(loc)) {
            activeLodestones.remove(loc);
            block.removeMetadata("respawn_owner", plugin);
        }
    }

    private boolean isValidLocation(Location loc) {
        if (loc.getY() < minY || loc.getY() > maxY) {
            return false;
        }

        if (requireAirAbove) {
            Block above = loc.getBlock().getRelative(BlockFace.UP);
            return above.getType() == Material.AIR || above.getType() == Material.CAVE_AIR;
        }

        return true;
    }

    private boolean isLodestoneActive(Location loc) {
        Long expiryTime = activeLodestones.get(loc);
        if (expiryTime == null) {
            return false;
        }

        if (expiryTime < System.currentTimeMillis()) {
            activeLodestones.remove(loc);
            return false;
        }

        return true;
    }

    private void setRespawnPoint(Player player, Block block, int diamonds, int minutes) {
        // Update inventory
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem.getAmount() == diamonds) {
            player.getInventory().setItemInMainHand(null);
        } else {
            heldItem.setAmount(heldItem.getAmount() - diamonds);
        }

        // Set expiry time
        long expiryTime = System.currentTimeMillis() + (minutes * 60L * 1000L);
        activeLodestones.put(block.getLocation(), expiryTime);

        // Store owner data
        block.setMetadata("respawn_owner", new FixedMetadataValue(plugin, player.getUniqueId().toString()));

        // Set respawn point in manager
        plugin.getRespawnManager().setRespawnPoint(player, block.getLocation());

        // Send success message
        player.sendMessage(plugin.getMiniMessage().deserialize(
                String.format(MSG_SUCCESS, minutes, diamonds)
        ));
    }

    private boolean isOnCooldown(Player player) {
        Long cooldownUntil = playerCooldowns.get(player.getUniqueId());
        return cooldownUntil != null && cooldownUntil > System.currentTimeMillis();
    }

    private long getCooldownRemaining(Player player) {
        Long cooldownUntil = playerCooldowns.get(player.getUniqueId());
        if (cooldownUntil == null) return 0;
        return TimeUnit.MILLISECONDS.toSeconds(cooldownUntil - System.currentTimeMillis());
    }

    private void setPlayerCooldown(Player player) {
        playerCooldowns.put(
                player.getUniqueId(),
                System.currentTimeMillis() + (cooldownSeconds * 1000L)
        );
    }

    private void sendCooldownMessage(Player player, long remainingSeconds) {
        player.sendMessage(plugin.getMiniMessage().deserialize(
                String.format(MSG_COOLDOWN, remainingSeconds)
        ));
    }

    private void playRespawnEffects(Location loc, Player player) {
        // Play sound effect
        player.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 1.0f, 1.0f);

        // Schedule particle effects
        new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 20) {
                    this.cancel();
                    return;
                }

                loc.getWorld().spawnParticle(
                        org.bukkit.Particle.PORTAL,
                        loc.clone().add(0.5, 1.0, 0.5),
                        10,
                        0.5,
                        0.5,
                        0.5,
                        0.1
                );

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void startCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();

                // Cleanup expired lodestones
                activeLodestones.entrySet().removeIf(entry -> entry.getValue() < now);

                // Cleanup expired cooldowns
                playerCooldowns.entrySet().removeIf(entry -> entry.getValue() < now);
            }
        }.runTaskTimer(plugin, 20L * 60L, 20L * 60L); // Run every minute
    }
}