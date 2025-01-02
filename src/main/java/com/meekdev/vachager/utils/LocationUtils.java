package com.meekdev.vachager.utils;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class LocationUtils {

    public static boolean isValidLodestoneLocation(Location loc, double minY, double maxY, boolean requireAirAbove) {
        if (loc.getY() < minY || loc.getY() > maxY) {
            return false;
        }

        if (requireAirAbove) {
            Block above = loc.getBlock().getRelative(BlockFace.UP);
            return above.getType().isAir();
        }

        return true;
    }

    public static boolean isWithinDistance(Location loc1, Location loc2, double distance) {
        return loc1.getWorld().equals(loc2.getWorld()) && loc1.distance(loc2) < distance;
    }
}