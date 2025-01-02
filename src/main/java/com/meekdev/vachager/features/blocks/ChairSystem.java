package com.meekdev.vachager.features.blocks;

import com.meekdev.vachager.VachagerSMP;
import com.meekdev.vachager.utils.MessageUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChairSystem implements Listener, CommandExecutor {
    private final VachagerSMP plugin;
    private final Map<UUID, ArmorStand> sittingPlayers = new ConcurrentHashMap<>();
    private final Map<UUID, Location> originalLocations = new ConcurrentHashMap<>();
    private volatile boolean enabled = true;
    private BukkitTask cleanupTask;

    private static final String COLOR_ERROR = "#ff4d2e";
    private static final String COLOR_WARN = "#ff9d52";
    private static final String COLOR_SUCCESS = "#a7ff78";

    public ChairSystem(@NotNull VachagerSMP plugin) {
        this.plugin = plugin;
        this.startCleanupTask();
        this.registerAll();
    }

    private void registerAll() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        try {
            if (plugin.getCommand("chairs") != null) {
                plugin.getCommand("chairs").setExecutor(this);
            } else {
                plugin.getLogger().warning("Failed to register /chairs command - command not found in plugin.yml");
            }
        } catch (IllegalStateException e) {
            plugin.getLogger().warning("Failed to register /chairs command - plugin not enabled");
        }
    }


    private void startCleanupTask() {
        cleanupTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            cleanupInvalidChairs();
        }, 20L, 20L);
    }


    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!enabled || event.getAction() != Action.RIGHT_CLICK_BLOCK ||
                event.getHand() != EquipmentSlot.HAND) return;

        Block block = event.getClickedBlock();
        Player player = event.getPlayer();

        if (block == null || !(block.getBlockData() instanceof Stairs stairs) ||
                player.isSneaking() || sittingPlayers.containsKey(player.getUniqueId())) return;

        if (isValidChair(block, stairs)) {
            event.setCancelled(true);
            sitPlayer(player, block);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        unsitPlayer(event.getPlayer(), false);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (sittingPlayers.containsKey(event.getPlayer().getUniqueId())) {
            unsitPlayer(event.getPlayer(), true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        unsitPlayer(event.getPlayer(), false);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(PlayerChangedWorldEvent event) {
        if (sittingPlayers.containsKey(event.getPlayer().getUniqueId())) {
            unsitPlayer(event.getPlayer(), true);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtils.sendMessage((Player) sender, "Cette commande ne peut être utilisée que par un joueur!", COLOR_ERROR);
            return true;
        }

        if (!sender.hasPermission("vachager.chairs.toggle")) {
            MessageUtils.sendMessage(player, "Vous n'avez pas la permission!", COLOR_ERROR);
            return true;
        }

        enabled = !enabled;
        if (enabled) {
            HandlerList.unregisterAll(this);
            registerAll();
            startCleanupTask();
            MessageUtils.sendMessage(player, "Système de chaises activé!", COLOR_SUCCESS);
        } else {
            disable();
            MessageUtils.sendMessage(player, "Système de chaises désactivé!", COLOR_WARN);
        }

        plugin.getLogger().info("Chair system " + (enabled ? "enabled" : "disabled") + " by " + player.getName());
        return true;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (event.isSneaking() && sittingPlayers.containsKey(player.getUniqueId())) {
            unsitPlayer(player, false);
        }
    }

    private void unsitPlayer(Player player, boolean teleportBack) {
        UUID playerId = player.getUniqueId();
        ArmorStand chair = sittingPlayers.remove(playerId);
        Location originalLoc = originalLocations.remove(playerId);

        try {
            if (chair != null && chair.isValid()) {

                chair.removePassenger(player);

                chair.remove();
            }


            if (teleportBack && originalLoc != null && player.isOnline()
                    && sittingPlayers.containsKey(playerId)) {
                player.teleport(originalLoc);
            }


            sittingPlayers.remove(playerId);
            originalLocations.remove(playerId);

        } catch (Exception e) {
            plugin.getLogger().warning("Error unsitting player: " + e.getMessage());

            if (chair != null && chair.isValid()) {
                chair.remove();
            }
            sittingPlayers.remove(playerId);
            originalLocations.remove(playerId);
        }
    }

    private boolean isValidChair(Block block, Stairs stairs) {
        return stairs.getHalf() == Bisected.Half.BOTTOM &&
                block.getRelative(BlockFace.UP).getType() == Material.AIR;
    }

    private void sitPlayer(Player player, Block block) {
        try {
            Location original = player.getLocation().clone();
            originalLocations.put(player.getUniqueId(), original);

            Location seatLocation = calculateSeatLocation(block, player);
            ArmorStand chair = spawnChair(seatLocation);

            if (!chair.addPassenger(player)) {
                chair.remove();
                originalLocations.remove(player.getUniqueId());
                return;
            }

            sittingPlayers.put(player.getUniqueId(), chair);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to sit player: " + e.getMessage());
            unsitPlayer(player, true);
        }
    }

    private Location calculateSeatLocation(Block block, Player player) {
        Location seatLocation = block.getLocation().clone().add(0.5, 0.3, 0.5);

        if (block.getBlockData() instanceof Stairs stairs) {
            seatLocation.setYaw(switch (stairs.getFacing()) {
                case NORTH -> 180;
                case SOUTH -> 0;
                case WEST -> 90;
                case EAST -> -90;
                default -> player.getLocation().getYaw();
            });
        }

        return seatLocation;
    }

    private ArmorStand spawnChair(Location location) {
        return location.getWorld().spawn(location, ArmorStand.class, stand -> {
            stand.setGravity(false);
            stand.setVisible(false);
            stand.setSmall(true);
            stand.setMarker(true);
            stand.setInvulnerable(true);
            stand.setCollidable(false);
            stand.setPersistent(false);


            stand.getPersistentDataContainer().set(
                    plugin.getKey("chair"),
                    PersistentDataType.BYTE,
                    (byte) 1
            );
        });
    }

    private void cleanupInvalidChairs() {

        sittingPlayers.entrySet().removeIf(entry -> {
            ArmorStand stand = entry.getValue();
            return stand == null || !stand.isValid() || stand.getPassengers().isEmpty();
        });


        originalLocations.keySet().removeIf(uuid -> !sittingPlayers.containsKey(uuid));
    }

    private void unsitAll() {
        new ConcurrentHashMap<>(sittingPlayers).keySet().forEach(uuid -> {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null) {
                unsitPlayer(player, true);
            }
        });
        sittingPlayers.clear();
        originalLocations.clear();
    }

    public void disable() {
        enabled = false;
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
        HandlerList.unregisterAll(this);

        if (plugin.getCommand("chairs") != null) {
            plugin.getCommand("chairs").setExecutor(null);
        }
        unsitAll();
    }

}