package com.meekdev.vachager.features.respawn;

import org.bukkit.Location;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LodestoneData {
    private final Location location;
    private final UUID ownerUUID;
    private final long expiryTime;
    private final TextDisplay textDisplay;
    private final BlockDisplay blockDisplay;
    private final Set<UUID> sharedPlayers;
    private BukkitTask breakTask;
    private int breakProgress;
    private boolean valid;

    public LodestoneData(Location location, UUID ownerUUID, long expiryTime, TextDisplay textDisplay, BlockDisplay blockDisplay) {
        this.location = location;
        this.ownerUUID = ownerUUID;
        this.expiryTime = expiryTime;
        this.textDisplay = textDisplay;
        this.blockDisplay = blockDisplay;
        this.sharedPlayers = ConcurrentHashMap.newKeySet();
        this.valid = true;
        this.breakProgress = 0;
    }

    public Location getLocation() {
        return location;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public long getExpiryTime() {
        return expiryTime;
    }

    public TextDisplay getTextDisplay() {
        return textDisplay;
    }

    public BlockDisplay getBlockDisplay() {
        return blockDisplay;
    }

    public Set<UUID> getSharedPlayers() {
        return sharedPlayers;
    }

    public boolean isValid() {
        return valid && textDisplay != null && textDisplay.isValid() &&
                blockDisplay != null && blockDisplay.isValid();
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= expiryTime;
    }

    public boolean isOwner(UUID uuid) {
        return ownerUUID.equals(uuid);
    }

    public void invalidate() {
        valid = false;
        if (textDisplay != null && textDisplay.isValid()) {
            textDisplay.remove();
        }
        if (blockDisplay != null && blockDisplay.isValid()) {
            blockDisplay.remove();
        }
        if (breakTask != null) {
            breakTask.cancel();
        }
    }

    public void setBreakTask(BukkitTask task) {
        if (this.breakTask != null) {
            this.breakTask.cancel();
        }
        this.breakTask = task;
    }

    public BukkitTask getBreakTask() {
        return breakTask;
    }

    public int getBreakProgress() {
        return breakProgress;
    }

    public void setBreakProgress(int progress) {
        this.breakProgress = progress;
    }

    public @NotNull String getOwner() {
        return ownerUUID.toString();
    }

    public boolean addSharedPlayer(@NotNull UUID uniqueId) {
        return sharedPlayers.add(uniqueId);
    }
}