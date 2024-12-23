package com.meekdev.vachager.features.respawn;

import org.bukkit.Location;

public class RespawnPoint {
    private final Location location;
    private final long expiryTime;

    public RespawnPoint(Location location, long expiryTime) {
        this.location = location;
        this.expiryTime = expiryTime;
    }

    public Location getLocation() {
        return location;
    }

    public long getExpiryTime() {
        return expiryTime;
    }

    public boolean isValid() {
        return System.currentTimeMillis() < expiryTime;
    }
}