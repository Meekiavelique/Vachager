package com.meekdev.vachager.features.respawn;

import com.meekdev.vachager.VachagerSMP;
import com.meekdev.vachager.utils.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import static org.apache.logging.log4j.LogManager.getLogger;

public class LodestoneListener implements Listener {
    private final VachagerSMP plugin;
    private final LodestoneManager manager;
    private final RespawnManager respawnManager;

    private static final String COLOR_ERROR = "#ff4d2e";
    private static final String COLOR_WARN = "#ff9d52";
    private static final String COLOR_SUCCESS = "#a7ff78";

    public LodestoneListener(VachagerSMP plugin, LodestoneManager manager, RespawnManager respawnManager) {
        this.plugin = plugin;
        this.manager = manager;
        this.respawnManager = respawnManager;
    }

    public static void playRespawnEffects(Location respawnLoc, Player player) {
        World world = respawnLoc.getWorld();
        Location particleLoc = respawnLoc.clone().add(0.5, 0.1, 0.5);

        for (double y = 0; y < 3; y += 0.2) {
            Location beamLoc = particleLoc.clone().add(0, y, 0);
            world.spawnParticle(Particle.END_ROD, beamLoc, 2, 0.1, 0, 0.1, 0.01);
        }

        for (int ring = 0; ring < 3; ring++) {
            double y = ring * 0.7;
            for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 16) {
                double x = Math.cos(angle) * 0.8;
                double z = Math.sin(angle) * 0.8;
                Location ringLoc = particleLoc.clone().add(x, y, z);
                world.spawnParticle(Particle.SOUL_FIRE_FLAME, ringLoc, 1, 0, 0, 0, 0.02);
            }
        }


        for (int i = 0; i < 50; i++) {
            double angle = Math.random() * Math.PI * 2;
            double radius = Math.random() * 1.5;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            Location ascendLoc = particleLoc.clone().add(x, 0, z);
            world.spawnParticle(Particle.DRAGON_BREATH, ascendLoc, 1, 0, 0.8, 0, 0.15);
        }


        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 32) {
            double x = Math.cos(angle) * 2;
            double z = Math.sin(angle) * 2;
            Location ringLoc = particleLoc.clone().add(x, 0.1, z);
            world.spawnParticle(Particle.REVERSE_PORTAL, ringLoc, 1, 0, 0, 0, 0.05);
        }

        world.spawnParticle(Particle.TOTEM_OF_UNDYING, particleLoc.clone().add(0, 1, 0), 100, 0.5, 0.5, 0.5, 0.15);

        player.playSound(respawnLoc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0f, 1.0f);
        player.playSound(respawnLoc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.2f);
        player.playSound(respawnLoc, Sound.BLOCK_END_PORTAL_SPAWN, 0.5f, 1.5f);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.LODESTONE) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        Location loc = block.getLocation();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (manager.isLodestoneActive(loc)) {
            if (item.getType() == Material.NETHERITE_INGOT) {
                handleNetheriteUpgrade(player, loc);
            }
            return;
        }

        if (item.getType() == Material.DIAMOND) {
            if (!manager.setLodestone(player, block, item.getAmount())) {
                MessageUtils.sendMessage(player, "Impossible de créer le point de réapparition!", "#ff4d2e");
            }
        }
    }


    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.LODESTONE) return;

        Location loc = block.getLocation();
        if (!manager.isLodestoneActive(loc)) return;

        Player player = event.getPlayer();
        if (!manager.isLodestoneOwner(loc, player.getUniqueId())) {
            event.setCancelled(true);
            MessageUtils.sendMessage(player, "Vous ne pouvez pas casser ce point!", COLOR_ERROR);
            return;
        }

        manager.removeLodestone(loc);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> {
            if (block.getType() == Material.LODESTONE && manager.isLodestoneActive(block.getLocation())) {
                return true;
            }
            return false;
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(block ->
                block.getType() == Material.LODESTONE && manager.isLodestoneActive(block.getLocation()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(block ->
                block.getType() == Material.LODESTONE && manager.isLodestoneActive(block.getLocation()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        for (BlockState state : event.getChunk().getTileEntities()) {
            if (state.getType() == Material.LODESTONE && manager.isLodestoneActive(state.getLocation())) {
                event.getChunk().addPluginChunkTicket(plugin);
                break;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Location respawnLoc = respawnManager.getRespawnLocation(player);

        if (respawnLoc != null && respawnLoc.getBlock().getType() == Material.LODESTONE) {
            if (manager.isLodestoneActive(respawnLoc)) {
                event.setRespawnLocation(respawnLoc.clone().add(0.5, 1, 0.5));
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    player.playSound(respawnLoc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0f, 1.0f);
                    respawnLoc.getWorld().spawnParticle(
                            Particle.REVERSE_PORTAL,
                            respawnLoc.clone().add(0.5, 1, 0.5),
                            50, 0.5, 0.5, 0.5, 0.1
                    );
                }, 1L);
            }
        }
    }

    private void handleNetheriteUpgrade(Player player, Location loc) {
        if (manager.isLodestoneOwner(loc, player.getUniqueId())) {
            MessageUtils.sendMessage(player, "Vous êtes déjà le propriétaire!", COLOR_ERROR);
            return;
        }

        if (manager.addSharedPlayer(loc, player)) {
            PlayerUtils.consumeItem(player, player.getInventory().getItemInMainHand(), 1);
        } else {
            MessageUtils.sendMessage(player, "Impossible d'ajouter plus de joueurs!", COLOR_ERROR);
        }
    }
    public void cleanup() {
        HandlerList.unregisterAll(this);
        manager.cleanup();
    }
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        manager.getPlayerLodestones(player.getUniqueId()).forEach(loc -> {
            if (manager.isLodestoneActive(loc)) {
                manager.removeGlowDisplay(loc, player.getUniqueId());
            }
        });
    }
}