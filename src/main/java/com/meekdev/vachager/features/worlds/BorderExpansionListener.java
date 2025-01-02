package com.meekdev.vachager.features.worlds;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.Map;

public class BorderExpansionListener implements Listener {
    private final Map<Material, BorderExpansionData> expansionItems = new EnumMap<>(Material.class);
    private static final double EXPANSION_ANIMATION_TIME = 10.0;

    public BorderExpansionListener() {
        setupExpansionItems();
    }

    private void setupExpansionItems() {
        expansionItems.put(Material.DIAMOND, new BorderExpansionData(10.0, true));
        expansionItems.put(Material.NETHERITE_INGOT, new BorderExpansionData(45.0, true));
        expansionItems.put(Material.EMERALD, new BorderExpansionData(5.0, true));
        expansionItems.put(Material.DRAGON_EGG, new BorderExpansionData(5000.0, true));
        expansionItems.put(Material.NETHER_STAR, new BorderExpansionData(300.0, true));
        expansionItems.put(Material.BEACON, new BorderExpansionData(80.0, true));
    }

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {
        Item item = event.getEntity();
        ItemStack itemStack = item.getItemStack();
        World world = item.getWorld();

        WorldBorder border = world.getWorldBorder();
        BorderExpansionData expansionData = expansionItems.get(itemStack.getType());

        if (expansionData == null || !isNearBorder(item, border)) {
            return;
        }

        double expansionAmount = expansionData.blocks() * itemStack.getAmount();
        double newSize = border.getSize() + expansionAmount;

        border.setSize(newSize, (long) EXPANSION_ANIMATION_TIME);

        if (expansionData.consumeItem()) {
            event.setCancelled(true);
            world.spawnParticle(org.bukkit.Particle.PORTAL, item.getLocation(), 50, 0.5, 0.5, 0.5, 0.1);
            world.playSound(item.getLocation(), org.bukkit.Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.0f);
        }
    }

    private boolean isNearBorder(Item item, WorldBorder border) {
        double borderSize = border.getSize() / 2;
        double bufferZone = 5.0;

        double itemX = Math.abs(item.getLocation().getX() - border.getCenter().getX());
        double itemZ = Math.abs(item.getLocation().getZ() - border.getCenter().getZ());

        return (Math.abs(itemX - borderSize) <= bufferZone || Math.abs(itemZ - borderSize) <= bufferZone);
    }

    private record BorderExpansionData(double blocks, boolean consumeItem) {}
}