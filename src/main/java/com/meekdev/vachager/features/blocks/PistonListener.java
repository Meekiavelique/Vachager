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


        if (block == null) return;
        if (block.getType() != Material.STICKY_PISTON) return;
        if (!isAxe(item.getType())) return;


        event.setCancelled(true);

        block.setType(Material.PISTON);
        block.getWorld().dropItemNaturally(
                block.getLocation().add(0.5, 0.5, 0.5),
                new ItemStack(Material.SLIME_BALL)
        );


        addEffects(block);
    }

    private boolean isAxe(Material material) {
        return material == Material.IRON_AXE ||
                material == Material.DIAMOND_AXE ||
                material == Material.NETHERITE_AXE;
    }

    private void addEffects(Block block) {
        block.getWorld().spawnParticle(
                Particle.ITEM_SLIME,
                block.getLocation().add(0.5, 0.5, 0.5),
                10, 0.3, 0.3, 0.3, 0
        );

        block.getWorld().playSound(
                block.getLocation(),
                Sound.BLOCK_SLIME_BLOCK_BREAK,
                1.0f, 1.0f
        );
    }
}