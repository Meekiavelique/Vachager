package com.meekdev.vachager.features.mobs;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class BatDropListener implements Listener {
    private final Random random = new Random();
    private static final double DROP_CHANCE = 0.20;

    @EventHandler
    public void onBatDeath(EntityDeathEvent event) {
        if (event.getEntityType() != EntityType.BAT) return;

        if (random.nextDouble() <= DROP_CHANCE) {
            ItemStack membrane = new ItemStack(Material.PHANTOM_MEMBRANE);
            event.getEntity().getWorld().dropItemNaturally(
                    event.getEntity().getLocation(),
                    membrane
            );
        }
    }
}