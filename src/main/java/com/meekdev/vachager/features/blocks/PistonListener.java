package com.meekdev.vachager.features.blocks;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class PistonListener implements Listener {

    @EventHandler
    public void onPistonInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (block == null || item.getType() != Material.IRON_AXE && item.getType() != Material.DIAMOND_AXE
                && item.getType() != Material.NETHERITE_AXE) return;

        if (block.getType() == Material.STICKY_PISTON) {
            block.setType(Material.PISTON);

            block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5),
                    new ItemStack(Material.SLIME_BALL));

            block.getWorld().spawnParticle(
                    Particle.ITEM_SLIME,
                    block.getLocation().add(0.5, 0.5, 0.5),
                    10,
                    0.3,
                    0.3,
                    0.3,
                    0
            );

            block.getWorld().playSound(
                    block.getLocation(),
                    Sound.BLOCK_SLIME_BLOCK_BREAK,
                    1.0f,
                    1.0f
            );

            event.setCancelled(true);
        }
    }
}